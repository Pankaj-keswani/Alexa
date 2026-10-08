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
import com.example.phonebridge.service.PhoneBridgeForegroundService
import com.example.phonebridge.ui.screens.LogsScreen
import com.example.phonebridge.ui.screens.MainDashboardScreen
import com.example.phonebridge.ui.screens.SettingsScreen
import com.example.phonebridge.ui.screens.TestSimulationScreen
import com.example.phonebridge.ui.theme.PhoneBridgeTheme
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
                val isAlexaConfigured by viewModel.isAlexaConfigured.collectAsState()
                val callState by viewModel.callState.collectAsState()
                val callerName by viewModel.callerName.collectAsState()
                val phoneNumber by viewModel.phoneNumber.collectAsState()
                val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()
                val logs by viewModel.logs.collectAsState()

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
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Build, contentDescription = "Test Mode") },
                                label = { Text("Test Mode") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                                label = { Text("Logs") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
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
                                onNavigateToTest = { selectedTab = 1 }
                            )
                            1 -> TestSimulationScreen(
                                currentCallState = callState,
                                currentCaller = callerName,
                                onSimulateIncoming = { name, num -> viewModel.simulateIncomingCall(name, num) },
                                onSimulateAnswer = { viewModel.simulateAnswerCall() },
                                onSimulateReject = { viewModel.simulateRejectCall() },
                                onSimulateEndCall = { viewModel.simulateEndCall() }
                            )
                            2 -> LogsScreen(
                                logs = logs,
                                onClearLogs = { viewModel.clearLogs() }
                            )
                            3 -> SettingsScreen(
                                currentUrl = backendUrl,
                                currentToken = deviceToken,
                                currentDeviceId = deviceId,
                                currentNotifs = enableNotifications,
                                currentAnnounce = enableAnnouncement,
                                currentDebug = debugLogging,
                                currentTestMode = isTestMode,
                                onSaveSettings = { url, token, devId, notifs, announce, debug, testMode ->
                                    viewModel.saveSettings(url, token, devId, notifs, announce, debug, testMode)
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
                Manifest.permission.ANSWER_PHONE_CALLS
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
