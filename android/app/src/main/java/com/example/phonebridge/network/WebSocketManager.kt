package com.example.phonebridge.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.phonebridge.data.CommandMessage
import com.example.phonebridge.data.CommandResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketManager(
    private val baseUrlProvider: () -> String,
    private val tokenProvider: () -> String,
    private val deviceIdProvider: () -> String,
    private val isNetworkAvailable: () -> Boolean = { true },
    private val pingIntervalSecProvider: () -> Long = { 60L },
    private val isEcoModeProvider: () -> Boolean = { false },
    private val onCommandReceived: (CommandMessage) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit,
    private val onLog: (tag: String, message: String, isError: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "WebSocketManager"
    }

    private val gson = Gson()
    private var client: OkHttpClient? = null

    private var webSocket: WebSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0

    private val reconnectRunnable = Runnable {
        if (isRunning.get() && !isConnected.get()) {
            connectInternal()
        }
    }

    private fun getClient(): OkHttpClient {
        val pingSec = pingIntervalSecProvider().coerceIn(30L, 300L)
        val currentClient = client
        if (currentClient != null) {
            return currentClient
        }

        val newClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(pingSec, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        client = newClient
        return newClient
    }

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            reconnectAttempts = 0
            connectInternal()
        }
    }

    fun stop() {
        isRunning.set(false)
        mainHandler.removeCallbacks(reconnectRunnable)
        closeSocket()
    }

    fun reconnect() {
        reconnectAttempts = 0
        mainHandler.removeCallbacks(reconnectRunnable)
        closeSocket()
        connectInternal()
    }

    fun onNetworkRestored() {
        if (isRunning.get() && !isConnected.get()) {
            onLog("BACKEND", "Network restored: attempting immediate reconnection", false)
            reconnectAttempts = 0
            mainHandler.removeCallbacks(reconnectRunnable)
            connectInternal()
        }
    }

    fun onNetworkLost() {
        mainHandler.removeCallbacks(reconnectRunnable)
        closeSocket()
        onLog("BACKEND", "Network offline: pausing socket retries to preserve battery", false)
    }

    fun sendCommandResult(result: CommandResponse) {
        val json = gson.toJson(result)
        onLog("BACKEND", "Sending response: success=${result.success}, command=${result.command}", false)
        webSocket?.send(json)
    }

    private fun closeSocket() {
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
        notifyConnection(false)
    }

    private fun getNextReconnectDelayMs(): Long {
        val isEco = isEcoModeProvider()
        val base = if (isEco) 8000L else 5000L
        val maxDelay = if (isEco) 120000L else 60000L
        val factor = (1 shl reconnectAttempts.coerceAtMost(5)).toLong()
        val delay = (base * factor).coerceAtMost(maxDelay)
        val jitter = (Math.random() * 2000).toLong()
        return delay + jitter
    }

    private fun connectInternal() {
        if (!isNetworkAvailable()) {
            onLog("BACKEND", "No active internet connection. Waiting for network...", false)
            return
        }

        val rawBase = baseUrlProvider().trim().removeSuffix("/")
        if (rawBase.isBlank()) {
            onLog("BACKEND", "WebSocket URL is empty, skipping connection", true)
            return
        }

        val wsUrl = when {
            rawBase.startsWith("https://", ignoreCase = true) -> rawBase.replaceFirst("https://", "wss://", true)
            rawBase.startsWith("http://", ignoreCase = true) -> rawBase.replaceFirst("http://", "ws://", true)
            rawBase.startsWith("ws://", ignoreCase = true) || rawBase.startsWith("wss://", ignoreCase = true) -> rawBase
            else -> "ws://$rawBase"
        }

        val deviceId = deviceIdProvider()
        val token = tokenProvider()
        val fullUrl = "$wsUrl/ws?deviceId=$deviceId&token=$token"

        onLog("BACKEND", "Connecting to WebSocket at $wsUrl/ws ...", false)

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("x-device-token", token)
            .addHeader("x-device-id", deviceId)
            .build()

        webSocket = getClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully")
                isConnected.set(true)
                reconnectAttempts = 0
                notifyConnection(true)
                onLog("BACKEND", "WebSocket connected to backend", false)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket onMessage: $text")
                try {
                    val command = gson.fromJson(text, CommandMessage::class.java)
                    if (command != null && !command.command.isNullOrBlank()) {
                        onLog("ALEXA", "Command received via backend: ${command.command}", false)
                        mainHandler.post { onCommandReceived(command) }
                    }
                } catch (e: Exception) {
                    onLog("BACKEND", "Error parsing WebSocket message: ${e.message}", true)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code / $reason")
                isConnected.set(false)
                notifyConnection(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closed: $code / $reason")
                isConnected.set(false)
                notifyConnection(false)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                isConnected.set(false)
                notifyConnection(false)
                onLog("BACKEND", "WebSocket connection failed: ${t.message ?: "Connection error"}", true)
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!isRunning.get()) return
        mainHandler.removeCallbacks(reconnectRunnable)

        if (!isNetworkAvailable()) {
            onLog("BACKEND", "Offline: waiting for network to reconnect", false)
            return
        }

        reconnectAttempts++
        val delay = getNextReconnectDelayMs()
        onLog("BACKEND", "Next retry in ${delay / 1000}s (attempt #$reconnectAttempts)", false)
        mainHandler.postDelayed(reconnectRunnable, delay)
    }

    private fun notifyConnection(connected: Boolean) {
        mainHandler.post { onConnectionChanged(connected) }
    }
}
