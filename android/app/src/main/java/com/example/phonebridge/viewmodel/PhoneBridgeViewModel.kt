package com.example.phonebridge.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
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

    // State flows
    val isBackendConnected: StateFlow<Boolean> = PhoneBridgeForegroundService.isBackendConnected
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

    // Editable settings
    val backendUrl = MutableStateFlow(prefs.backendUrl)
    val deviceToken = MutableStateFlow(prefs.deviceToken)
    val deviceId = MutableStateFlow(prefs.deviceId)
    val enableNotifications = MutableStateFlow(prefs.enableCallNotifications)
    val enableAnnouncement = MutableStateFlow(prefs.enableCallerAnnouncement)
    val debugLogging = MutableStateFlow(prefs.debugLogging)
    val isTestMode = MutableStateFlow(prefs.isTestMode)

    init {
        checkPhoneRole()
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
        testMode: Boolean
    ) {
        prefs.backendUrl = url
        prefs.deviceToken = token
        prefs.deviceId = devId
        prefs.enableCallNotifications = notifs
        prefs.enableCallerAnnouncement = announce
        prefs.debugLogging = logging
        prefs.isTestMode = testMode

        backendUrl.value = url
        deviceToken.value = token
        deviceId.value = devId
        enableNotifications.value = notifs
        enableAnnouncement.value = announce
        debugLogging.value = logging
        isTestMode.value = testMode

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
