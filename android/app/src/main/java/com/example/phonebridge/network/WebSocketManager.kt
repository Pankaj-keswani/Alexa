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
    private val onCommandReceived: (CommandMessage) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit,
    private val onLog: (tag: String, message: String, isError: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val reconnectRunnable = Runnable {
        if (isRunning.get() && !isConnected.get()) {
            connectInternal()
        }
    }

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            connectInternal()
        }
    }

    fun stop() {
        isRunning.set(false)
        mainHandler.removeCallbacks(reconnectRunnable)
        closeSocket()
    }

    fun reconnect() {
        closeSocket()
        connectInternal()
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

    private fun connectInternal() {
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

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully")
                isConnected.set(true)
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
        if (isRunning.get()) {
            mainHandler.removeCallbacks(reconnectRunnable)
            mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS)
        }
    }

    private fun notifyConnection(connected: Boolean) {
        mainHandler.post { onConnectionChanged(connected) }
    }
}
