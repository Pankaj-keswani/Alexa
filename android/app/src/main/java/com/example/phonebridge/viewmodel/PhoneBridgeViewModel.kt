package com.example.phonebridge.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonebridge.data.CallEventPayload
import com.example.phonebridge.data.CallState
import com.example.phonebridge.data.LogEntry
import com.example.phonebridge.data.LogRepository
import com.example.phonebridge.data.PreferencesManager
import com.example.phonebridge.network.BackendApiClient
import com.example.phonebridge.service.PhoneBridgeForegroundService
import com.example.phonebridge.telecom.PhoneJarvisController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhoneBridgeViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PreferencesManager(application)
    private val backendClient = BackendApiClient(
        baseUrlProvider = { prefs.backendUrl },
        tokenProvider = { prefs.deviceToken },
        deviceIdProvider = { prefs.deviceId }
    )

    private val localJarvisController = PhoneJarvisController(application)

    // State flows
    val isBackendConnected: StateFlow<Boolean> = PhoneBridgeForegroundService.isBackendConnected
    val isBridgeActive: StateFlow<Boolean> = PhoneBridgeForegroundService.isBridgeActiveFlow
    val networkType: StateFlow<String> = PhoneBridgeForegroundService.networkTypeFlow
    val callState: StateFlow<CallState> = PhoneBridgeForegroundService.callStateFlow
    val callerName: StateFlow<String?> = PhoneBridgeForegroundService.callerNameFlow
    val phoneNumber: StateFlow<String?> = PhoneBridgeForegroundService.phoneNumberFlow
    val logs: StateFlow<List<LogEntry>> = LogRepository.logsFlow

    private val _isAlexaConfigured = MutableStateFlow(true)
    val isAlexaConfigured: StateFlow<Boolean> = _isAlexaConfigured.asStateFlow()

    private val _isDefaultDialer = MutableStateFlow(false)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Interactive Device Controls State
    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _isAlarmPlaying = MutableStateFlow(false)
    val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isBatteryCharging = MutableStateFlow(false)
    val isBatteryCharging: StateFlow<Boolean> = _isBatteryCharging.asStateFlow()

    private val _freeStorageGb = MutableStateFlow(0L)
    val freeStorageGb: StateFlow<Long> = _freeStorageGb.asStateFlow()

    private val _totalStorageGb = MutableStateFlow(0L)
    val totalStorageGb: StateFlow<Long> = _totalStorageGb.asStateFlow()

    private val _freeRamGb = MutableStateFlow("0.0")
    val freeRamGb: StateFlow<String> = _freeRamGb.asStateFlow()

    private val _totalRamGb = MutableStateFlow("0.0")
    val totalRamGb: StateFlow<String> = _totalRamGb.asStateFlow()

    private val _soundMode = MutableStateFlow("NORMAL")
    val soundMode: StateFlow<String> = _soundMode.asStateFlow()

    private val _volumeLevel = MutableStateFlow(50)
    val volumeLevel: StateFlow<Int> = _volumeLevel.asStateFlow()

    // Editable settings
    val backendUrl = MutableStateFlow(prefs.backendUrl)
    val deviceToken = MutableStateFlow(prefs.deviceToken)
    val deviceId = MutableStateFlow(prefs.deviceId)
    val enableNotifications = MutableStateFlow(prefs.enableCallNotifications)
    val enableAnnouncement = MutableStateFlow(prefs.enableCallerAnnouncement)
    val debugLogging = MutableStateFlow(prefs.debugLogging)
    val isTestMode = MutableStateFlow(prefs.isTestMode)
    val isEcoMode = MutableStateFlow(prefs.isEcoMode)
    val pingIntervalSeconds = MutableStateFlow(prefs.pingIntervalSeconds)

    init {
        checkPhoneRole()
        refreshDeviceStats()
    }

    private fun getJarvisController(): PhoneJarvisController {
        return PhoneBridgeForegroundService.instance?.getJarvisController() ?: localJarvisController
    }

    fun checkPhoneRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getApplication<Application>().getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    _isDefaultDialer.value = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
                } else {
                    _isDefaultDialer.value = false
                }
            } else {
                _isDefaultDialer.value = false
            }
        } catch (e: Exception) {
            _isDefaultDialer.value = false
        }
    }

    fun toggleBridge(enabled: Boolean) {
        prefs.isBridgeEnabled = enabled
        PhoneBridgeForegroundService.instance?.setBridgeEnabled(enabled)
        _statusMessage.value = if (enabled) "PhoneBridge resumed (online)" else "PhoneBridge paused (battery saving)"
    }

    fun toggleEcoMode(enabled: Boolean) {
        prefs.isEcoMode = enabled
        isEcoMode.value = enabled
        _statusMessage.value = if (enabled) "Eco Battery Saver Mode Enabled" else "Normal Performance Mode Enabled"
        PhoneBridgeForegroundService.instance?.reconnectWebSocket()
    }

    fun refreshDeviceStats() {
        try {
            val controller = getJarvisController()
            val batRes = controller.getBatteryStatus(null)
            val batMap = batRes.data as? Map<*, *>
            if (batRes.success && batMap != null) {
                _batteryLevel.value = (batMap["batteryLevel"] as? Number)?.toInt() ?: 100
                _isBatteryCharging.value = (batMap["isCharging"] as? Boolean) ?: false
            }

            val statRes = controller.getDeviceStats(null)
            val statMap = statRes.data as? Map<*, *>
            if (statRes.success && statMap != null) {
                _freeStorageGb.value = (statMap["freeStorageGb"] as? Number)?.toLong() ?: 0L
                _totalStorageGb.value = (statMap["totalStorageGb"] as? Number)?.toLong() ?: 0L
                _freeRamGb.value = statMap["freeRamGb"]?.toString() ?: "0.0"
                _totalRamGb.value = statMap["totalRamGb"]?.toString() ?: "0.0"
            }

            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                _soundMode.value = when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> "SILENT"
                    AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                    else -> "NORMAL"
                }
                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
                _volumeLevel.value = ((curVol.toFloat() / maxVol) * 100).toInt()
            }
        } catch (_: Exception) {}
    }

    fun toggleFlashlight() {
        val controller = getJarvisController()
        val newState = controller.toggleFlashlight()
        _isFlashlightOn.value = newState
        _statusMessage.value = if (newState) "Flashlight turned ON" else "Flashlight turned OFF"
    }

    fun toggleAlarm() {
        val controller = getJarvisController()
        val newState = controller.toggleAlarm()
        _isAlarmPlaying.value = newState
        _statusMessage.value = if (newState) "Phone Alarm sounding!" else "Phone Alarm stopped."
    }

    fun setSoundProfile(mode: String) {
        val controller = getJarvisController()
        controller.setVolumeOrMode(mode, null, null)
        _soundMode.value = mode.uppercase()
        _statusMessage.value = "Sound mode set to $mode"
        refreshDeviceStats()
    }

    fun setVolumePercent(percent: Int) {
        val controller = getJarvisController()
        controller.setVolumeOrMode(null, percent, null)
        _volumeLevel.value = percent
        _statusMessage.value = "Volume set to $percent%"
    }

    fun speakTts(message: String) {
        if (message.isBlank()) return
        val controller = getJarvisController()
        controller.speakMessage(message, null)
        _statusMessage.value = "Speaking: \"$message\""
    }

    fun openAppDirect(appName: String) {
        val controller = getJarvisController()
        val res = controller.openApp(appName, null)
        _statusMessage.value = res.reason ?: "App action triggered"
    }

    fun testBackendConnection() {
        viewModelScope.launch {
            LogRepository.addLog("ANDROID", "Testing Backend connection to ${prefs.backendUrl} ...")
            _statusMessage.value = "Testing backend connection..."
            val result = backendClient.checkHealth()
            if (result.isSuccess) {
                LogRepository.addLog("BACKEND", "Backend connection check succeeded! Health: OK")
                _statusMessage.value = "Backend is reachable & healthy!"
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                LogRepository.addLog("BACKEND", "Backend connection failed: $err", isError = true)
                _statusMessage.value = "Backend connection failed: $err"
            }
        }
    }

    fun testAlexaIntegration() {
        viewModelScope.launch {
            LogRepository.addLog("ANDROID", "Testing Alexa integration via backend /api/call/status ...")
            _statusMessage.value = "Testing Alexa integration..."
            val result = backendClient.getCallStatus()
            if (result.isSuccess) {
                val status = result.getOrNull()
                _isAlexaConfigured.value = true
                LogRepository.addLog("ALEXA", "Alexa status queried: state=${status?.state}, caller=${status?.callerName ?: "None"}")
                _statusMessage.value = "Alexa integration test successful!"
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                LogRepository.addLog("ALEXA", "Alexa integration check failed: $err", isError = true)
                _statusMessage.value = "Alexa check failed: $err"
            }
        }
    }

    fun testCallState() {
        viewModelScope.launch {
            LogRepository.addLog("ANDROID", "Checking active Call State...")
            val result = backendClient.getCallStatus()
            if (result.isSuccess) {
                val data = result.getOrNull()
                val msg = "Call State: ${data?.state ?: "IDLE"} | Active: ${data?.hasActiveCall} | Caller: ${data?.callerName ?: "None"}"
                LogRepository.addLog("TELECOM", msg)
                _statusMessage.value = msg
            } else {
                _statusMessage.value = "Failed to query call state"
            }
        }
    }

    fun simulateIncomingCall(caller: String = "Rahul", number: String = "+919876543210") {
        LogRepository.addLog("ANDROID", "TEST MODE: Simulating incoming call from $caller ($number)")
        PhoneBridgeForegroundService.instance?.handleCallRinging(caller, number)
        _statusMessage.value = "Simulated incoming call from $caller"
    }

    fun simulateAnswerCall() {
        LogRepository.addLog("ALEXA", "TEST MODE: Simulating Alexa 'Answer' command")
        PhoneBridgeForegroundService.instance?.executeAnswerCommand("test-answer-${System.currentTimeMillis()}")
        _statusMessage.value = "Simulated answer command processed"
    }

    fun simulateRejectCall() {
        LogRepository.addLog("ALEXA", "TEST MODE: Simulating Alexa 'Reject' command")
        PhoneBridgeForegroundService.instance?.executeRejectCommand("test-reject-${System.currentTimeMillis()}")
        _statusMessage.value = "Simulated reject command processed"
    }

    fun simulateEndCall() {
        LogRepository.addLog("ANDROID", "TEST MODE: Simulating call ended / hangup")
        PhoneBridgeForegroundService.instance?.handleCallEnded()
        _statusMessage.value = "Simulated call ended"
    }

    fun saveSettings(
        url: String,
        token: String,
        devId: String,
        notifs: Boolean,
        announce: Boolean,
        logging: Boolean,
        testMode: Boolean,
        ecoMode: Boolean,
        pingSec: Int
    ) {
        prefs.backendUrl = url
        prefs.deviceToken = token
        prefs.deviceId = devId
        prefs.enableCallNotifications = notifs
        prefs.enableCallerAnnouncement = announce
        prefs.debugLogging = logging
        prefs.isTestMode = testMode
        prefs.isEcoMode = ecoMode
        prefs.pingIntervalSeconds = pingSec

        backendUrl.value = url
        deviceToken.value = token
        deviceId.value = devId
        enableNotifications.value = notifs
        enableAnnouncement.value = announce
        debugLogging.value = logging
        isTestMode.value = testMode
        isEcoMode.value = ecoMode
        pingIntervalSeconds.value = pingSec

        LogRepository.addLog("ANDROID", "Settings saved. Reconnecting WebSocket...")
        PhoneBridgeForegroundService.instance?.reconnectWebSocket()
        _statusMessage.value = "Settings saved & reconnected!"
    }

    fun clearLogs() {
        LogRepository.clear()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
