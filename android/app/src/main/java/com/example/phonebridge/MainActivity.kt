package com.example.phonebridge

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Tune
import com.example.phonebridge.service.PhoneBridgeForegroundService
import com.example.phonebridge.ui.screens.ControlCenterScreen
import com.example.phonebridge.ui.screens.LogsScreen
import com.example.phonebridge.ui.screens.MainDashboardScreen
import com.example.phonebridge.ui.screens.SettingsScreen
import com.example.phonebridge.ui.screens.TestSimulationScreen
import androidx.compose.ui.graphics.Color
import com.example.phonebridge.ui.theme.*
import com.example.phonebridge.viewmodel.PhoneBridgeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PhoneBridgeViewModel by viewModels()

    // Runtime permissions launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Restart foreground service to apply newly granted permissions
        startForegroundService()
    }

    // RoleManager launcher for Default Phone Dialer
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.checkPhoneRole()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhoneBridgeTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                val isBackendConnected by viewModel.isBackendConnected.collectAsState()
                val isBridgeActive by viewModel.isBridgeActive.collectAsState()
                val isEcoMode by viewModel.isEcoMode.collectAsState()
                val pingIntervalSeconds by viewModel.pingIntervalSeconds.collectAsState()
                val networkType by viewModel.networkType.collectAsState()
                val isAlexaConfigured by viewModel.isAlexaConfigured.collectAsState()
                val callState by viewModel.callState.collectAsState()
                val callerName by viewModel.callerName.collectAsState()
                val phoneNumber by viewModel.phoneNumber.collectAsState()
                val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()
                val logs by viewModel.logs.collectAsState()

                // Interactive control states
                val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
                val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsState()
                val batteryLevel by viewModel.batteryLevel.collectAsState()
                val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
                val freeStorageGb by viewModel.freeStorageGb.collectAsState()
                val totalStorageGb by viewModel.totalStorageGb.collectAsState()
                val freeRamGb by viewModel.freeRamGb.collectAsState()
                val totalRamGb by viewModel.totalRamGb.collectAsState()
                val soundMode by viewModel.soundMode.collectAsState()
                val volumeLevel by viewModel.volumeLevel.collectAsState()

                val backendUrl by viewModel.backendUrl.collectAsState()
                val deviceToken by viewModel.deviceToken.collectAsState()
                val deviceId by viewModel.deviceId.collectAsState()
                val enableNotifications by viewModel.enableNotifications.collectAsState()
                val enableAnnouncement by viewModel.enableAnnouncement.collectAsState()
                val debugLogging by viewModel.debugLogging.collectAsState()
                val isTestMode by viewModel.isTestMode.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = CyberSurface,
                            contentColor = TextPrimary
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Tune, contentDescription = "Controls") },
                                label = { Text("Controls") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Build, contentDescription = "Simulator") },
                                label = { Text("Simulator") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                                label = { Text("Logs") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> MainDashboardScreen(
                                isBackendConnected = isBackendConnected,
                                isAlexaConfigured = isAlexaConfigured,
                                callState = callState,
                                callerName = callerName,
                                phoneNumber = phoneNumber,
                                isDefaultDialer = isDefaultDialer,
                                statusMessage = statusMessage,
                                onTestBackend = { viewModel.testBackendConnection() },
                                onTestAlexa = { viewModel.testAlexaIntegration() },
                                onTestCallState = { viewModel.testCallState() },
                                onRequestPhoneRole = { requestPhoneDialerRole() },
                                onQuickSimulateCall = { viewModel.simulateIncomingCall("Rahul", "+919876543210") },
                                onNavigateToControls = { selectedTab = 1 },
                                onNavigateToTest = { selectedTab = 2 }
                            )
                            1 -> ControlCenterScreen(
                                isBridgeActive = isBridgeActive,
                                isEcoMode = isEcoMode,
                                networkType = networkType,
                                batteryLevel = batteryLevel,
                                isCharging = isBatteryCharging,
                                freeStorageGb = freeStorageGb,
                                totalStorageGb = totalStorageGb,
                                freeRamGb = freeRamGb,
                                totalRamGb = totalRamGb,
                                isFlashlightOn = isFlashlightOn,
                                isAlarmPlaying = isAlarmPlaying,
                                soundMode = soundMode,
                                volumeLevel = volumeLevel,
                                onToggleBridge = { viewModel.toggleBridge(it) },
                                onToggleEcoMode = { viewModel.toggleEcoMode(it) },
                                onToggleFlashlight = { viewModel.toggleFlashlight() },
                                onToggleAlarm = { viewModel.toggleAlarm() },
                                onSetSoundProfile = { viewModel.setSoundProfile(it) },
                                onSetVolume = { viewModel.setVolumePercent(it) },
                                onSpeakTts = { viewModel.speakTts(it) },
                                onOpenApp = { viewModel.openAppDirect(it) },
                                onRefreshStats = { viewModel.refreshDeviceStats() }
                            )
                            2 -> TestSimulationScreen(
                                currentCallState = callState,
                                currentCaller = callerName,
                                onSimulateIncoming = { name, num -> viewModel.simulateIncomingCall(name, num) },
                                onSimulateAnswer = { viewModel.simulateAnswerCall() },
                                onSimulateReject = { viewModel.simulateRejectCall() },
                                onSimulateEndCall = { viewModel.simulateEndCall() }
                            )
                            3 -> LogsScreen(
                                logs = logs,
                                onClearLogs = { viewModel.clearLogs() }
                            )
                            4 -> SettingsScreen(
                                currentUrl = backendUrl,
                                currentToken = deviceToken,
                                currentDeviceId = deviceId,
                                currentNotifs = enableNotifications,
                                currentAnnounce = enableAnnouncement,
                                currentDebug = debugLogging,
                                currentTestMode = isTestMode,
                                currentEcoMode = isEcoMode,
                                currentPingSec = pingIntervalSeconds,
                                onSaveSettings = { url, token, devId, notifs, announce, debug, testMode, eco, pingSec ->
                                    viewModel.saveSettings(url, token, devId, notifs, announce, debug, testMode, eco, pingSec)
                                }
                            )
                        }
                    }
                }
            }
        }

        checkAndRequestPermissions()
        startForegroundService()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPhoneRole()
    }

    private fun checkAndRequestPermissions() {
        try {
            val permissions = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (needed.isNotEmpty()) {
                requestPermissionsLauncher.launch(needed.toTypedArray())
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Permission request error: ${e.message}")
        }
    }

    private fun requestPhoneDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        roleRequestLauncher.launch(intent)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Role request error: ${e.message}")
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, PhoneBridgeForegroundService::class.java).apply {
            action = PhoneBridgeForegroundService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start foreground service: ${e.message}")
        }
    }
}
