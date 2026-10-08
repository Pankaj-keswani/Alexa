package com.example.phonebridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.phonebridge.MainActivity
import com.example.phonebridge.R
import com.example.phonebridge.data.CallEventPayload
import com.example.phonebridge.data.CallState
import com.example.phonebridge.data.CommandMessage
import com.example.phonebridge.data.PreferencesManager
import com.example.phonebridge.network.BackendApiClient
import com.example.phonebridge.network.WebSocketManager
import com.example.phonebridge.telecom.PhoneJarvisController
import com.example.phonebridge.telecom.TelecomController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhoneBridgeForegroundService : Service() {

    companion object {
        private const val TAG = "ForegroundService"
        const val NOTIFICATION_ID = 1001
        const val CALL_NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "phonebridge_service_channel"
        const val CALL_CHANNEL_ID = "phonebridge_call_alerts"

        const val ACTION_START = "com.example.phonebridge.START"
        const val ACTION_STOP = "com.example.phonebridge.STOP"
        const val ACTION_INCOMING_CALL = "com.example.phonebridge.INCOMING_CALL"
        const val ACTION_CALL_ANSWERED = "com.example.phonebridge.CALL_ANSWERED"
        const val ACTION_CALL_ENDED = "com.example.phonebridge.CALL_ENDED"
        const val ACTION_EXECUTE_ANSWER = "com.example.phonebridge.EXECUTE_ANSWER"
        const val ACTION_EXECUTE_REJECT = "com.example.phonebridge.EXECUTE_REJECT"

        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"

        private val _callStateFlow = MutableStateFlow(CallState.IDLE)
        val callStateFlow = _callStateFlow.asStateFlow()

        private val _callerNameFlow = MutableStateFlow<String?>("")
        val callerNameFlow = _callerNameFlow.asStateFlow()

        private val _phoneNumberFlow = MutableStateFlow<String?>("")
        val phoneNumberFlow = _phoneNumberFlow.asStateFlow()

        private val _isBackendConnected = MutableStateFlow(false)
        val isBackendConnected = _isBackendConnected.asStateFlow()

        var instance: PhoneBridgeForegroundService? = null
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: PreferencesManager
    private lateinit var backendClient: BackendApiClient
    private lateinit var telecomController: TelecomController
    private lateinit var phoneJarvisController: PhoneJarvisController
    private var webSocketManager: WebSocketManager? = null
    private var telephonyCallback: Any? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesManager(applicationContext)
        telecomController = TelecomController(applicationContext)
        phoneJarvisController = PhoneJarvisController(applicationContext)

        backendClient = BackendApiClient(
            baseUrlProvider = { prefs.backendUrl },
            tokenProvider = { prefs.deviceToken },
            deviceIdProvider = { prefs.deviceId }
        )

        createNotificationChannels()
        try {
            val notification = buildForegroundNotification("Connected & Monitoring Calls")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                androidx.core.app.ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}")
        }

        setupWebSocket()
        setupInCallServiceListener()
        setupTelephonyListener()
    }

    private fun setupWebSocket() {
        webSocketManager = WebSocketManager(
            baseUrlProvider = { prefs.backendUrl },
            tokenProvider = { prefs.deviceToken },
            deviceIdProvider = { prefs.deviceId },
            onCommandReceived = { command -> handleIncomingCommand(command) },
            onConnectionChanged = { connected ->
                _isBackendConnected.value = connected
                updateForegroundNotification(if (connected) "Connected to Alexa backend" else "Reconnecting to backend...")
            },
            onLog = { tag, msg, isError ->
                logEvent(tag, msg, isError)
            }
        )
        webSocketManager?.start()
    }

    private fun setupInCallServiceListener() {
        PhoneBridgeInCallService.addListener { state, callerName, phoneNumber ->
            when (state) {
                CallState.RINGING -> handleCallRinging(callerName ?: "Unknown", phoneNumber ?: "")
                CallState.ACTIVE -> handleCallAnswered()
                CallState.DISCONNECTED, CallState.IDLE -> handleCallEnded()
            }
        }
    }

    private fun setupTelephonyListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING -> {
                            // Handled via CallReceiver or InCallService
                        }
                        TelephonyManager.CALL_STATE_OFFHOOK -> handleCallAnswered()
                        TelephonyManager.CALL_STATE_IDLE -> handleCallEnded()
                    }
                }
            }
            telephonyCallback = callback
            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            } catch (e: Exception) {
                Log.w(TAG, "Could not register TelephonyCallback: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_INCOMING_CALL -> {
                val caller = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown caller"
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
                handleCallRinging(caller, number)
            }
            ACTION_CALL_ANSWERED -> {
                handleCallAnswered()
            }
            ACTION_CALL_ENDED -> {
                handleCallEnded()
            }
            ACTION_EXECUTE_ANSWER -> {
                executeAnswerCommand(null)
            }
            ACTION_EXECUTE_REJECT -> {
                executeRejectCommand(null)
            }
        }
        return START_STICKY
    }

    fun handleCallRinging(callerName: String, phoneNumber: String) {
        _callStateFlow.value = CallState.RINGING
        _callerNameFlow.value = callerName
        _phoneNumberFlow.value = phoneNumber

        logEvent("ANDROID", "Incoming call detected: $callerName ($phoneNumber)", false)

        // Show Heads-Up Notification if notifications enabled
        if (prefs.enableCallNotifications) {
            showIncomingCallNotification(callerName, phoneNumber)
        }

        // Send INCOMING_CALL event to backend
        serviceScope.launch {
            val payload = CallEventPayload(
                deviceId = prefs.deviceId,
                event = "INCOMING_CALL",
                callerName = callerName,
                phoneNumber = phoneNumber
            )
            val result = backendClient.sendCallEvent(payload)
            if (result.isSuccess) {
                logEvent("BACKEND", "Incoming call event sent successfully to backend", false)
            } else {
                logEvent("BACKEND", "Failed to send event to backend: ${result.exceptionOrNull()?.message}", true)
            }
        }
    }

    fun handleCallAnswered() {
        if (_callStateFlow.value == CallState.RINGING) {
            _callStateFlow.value = CallState.ACTIVE
            cancelIncomingCallNotification()
            logEvent("ANDROID", "Call answered / active", false)

            serviceScope.launch {
                val payload = CallEventPayload(
                    deviceId = prefs.deviceId,
                    event = "CALL_ANSWERED",
                    callerName = _callerNameFlow.value,
                    phoneNumber = _phoneNumberFlow.value
                )
                backendClient.sendCallEvent(payload)
            }
        }
    }

    fun handleCallEnded() {
        if (_callStateFlow.value != CallState.IDLE) {
            _callStateFlow.value = CallState.IDLE
            cancelIncomingCallNotification()
            logEvent("ANDROID", "Call ended", false)

            serviceScope.launch {
                val payload = CallEventPayload(
                    deviceId = prefs.deviceId,
                    event = "CALL_ENDED"
                )
                backendClient.sendCallEvent(payload)
            }
            _callerNameFlow.value = null
            _phoneNumberFlow.value = null
        }
    }

    private fun handleIncomingCommand(command: CommandMessage) {
        logEvent("ALEXA", "Processing Alexa command: ${command.command}", false)
        when (command.command) {
            "ANSWER_CALL" -> executeAnswerCommand(command.requestId)
            "REJECT_CALL" -> executeRejectCommand(command.requestId)
            "FIND_PHONE" -> {
                val result = phoneJarvisController.findPhone(command.action, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Find My Phone: ${result.reason}", !result.success)
            }
            "GET_BATTERY" -> {
                val result = phoneJarvisController.getBatteryStatus(command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Battery check sent to backend", !result.success)
            }
            "SET_FLASHLIGHT" -> {
                val result = phoneJarvisController.setFlashlight(command.state ?: true, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Flashlight: ${result.reason}", !result.success)
            }
            "MAKE_CALL" -> {
                val result = phoneJarvisController.makeCall(command.target, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Outgoing call: ${result.reason}", !result.success)
            }
            "SEND_WHATSAPP" -> {
                val result = phoneJarvisController.sendWhatsApp(command.target, command.message, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "WhatsApp: ${result.reason}", !result.success)
            }
            "OPEN_APP" -> {
                val result = phoneJarvisController.openApp(command.target, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Open App: ${result.reason}", !result.success)
            }
            "SET_VOLUME" -> {
                val result = phoneJarvisController.setVolumeOrMode(command.mode, command.level, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Volume: ${result.reason}", !result.success)
            }
            "SPEAK_MESSAGE" -> {
                val result = phoneJarvisController.speakMessage(command.message, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "TTS Broadcast: ${result.reason}", !result.success)
            }
            "GET_DEVICE_STATS" -> {
                val result = phoneJarvisController.getDeviceStats(command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "Device stats check", !result.success)
            }
            "SEND_SMS" -> {
                val result = phoneJarvisController.sendSms(command.target, command.message, command.requestId)
                webSocketManager?.sendCommandResult(result)
                logEvent("JARVIS", "SMS: ${result.reason}", !result.success)
            }
            "TEST_CALL" -> {
                val caller = command.callerName ?: "Rahul"
                val number = command.phoneNumber ?: "+919876543210"
                handleCallRinging(caller, number)
            }
            "PING" -> {
                logEvent("BACKEND", "Ping received from backend", false)
            }
        }
    }

    fun executeAnswerCommand(requestId: String?) {
        logEvent("TELECOM", "Executing Answer attempt via TelecomController", false)
        val result = telecomController.answerCall(requestId, prefs.isTestMode)
        webSocketManager?.sendCommandResult(result)

        if (result.success) {
            logEvent("TELECOM", "Call answered successfully (enabling speakerphone)", false)
            telecomController.setSpeakerphoneOn(true)
            handleCallAnswered()
        } else {
            logEvent("TELECOM", "Answer failed: ${result.reason}", true)
        }
    }

    fun executeRejectCommand(requestId: String?) {
        logEvent("TELECOM", "Executing Reject attempt via TelecomController", false)
        val result = telecomController.rejectCall(requestId, prefs.isTestMode)
        webSocketManager?.sendCommandResult(result)

        if (result.success) {
            logEvent("TELECOM", "Call rejected successfully", false)
            handleCallEnded()
        } else {
            logEvent("TELECOM", "Reject failed: ${result.reason}", true)
        }
    }

    fun reconnectWebSocket() {
        webSocketManager?.reconnect()
    }

    private fun showIncomingCallNotification(callerName: String, phoneNumber: String) {
        val answerIntent = Intent(this, PhoneBridgeForegroundService::class.java).apply {
            action = ACTION_EXECUTE_ANSWER
        }
        val pendingAnswer = PendingIntent.getService(
            this, 101, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, PhoneBridgeForegroundService::class.java).apply {
            action = ACTION_EXECUTE_REJECT
        }
        val pendingReject = PendingIntent.getService(
            this, 102, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 103, openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Incoming Call: $callerName")
            .setContentText(if (phoneNumber.isNotBlank()) phoneNumber else "Alexa PhoneBridge")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher_foreground, "Answer (Alexa/Phone)", pendingAnswer)
            .addAction(R.drawable.ic_launcher_foreground, "Reject", pendingReject)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun cancelIncomingCallNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(CALL_NOTIFICATION_ID)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }

            val callAlertChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                getString(R.string.call_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.call_channel_description)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(callAlertChannel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(contentText))
    }

    private fun logEvent(tag: String, message: String, isError: Boolean) {
        if (prefs.debugLogging) {
            Log.d("PhoneBridge_$tag", message)
            com.example.phonebridge.data.LogRepository.addLog(tag, message, isError)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        webSocketManager?.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            (telephonyCallback as? TelephonyCallback)?.let { telephonyManager?.unregisterTelephonyCallback(it) }
        }
    }
}
