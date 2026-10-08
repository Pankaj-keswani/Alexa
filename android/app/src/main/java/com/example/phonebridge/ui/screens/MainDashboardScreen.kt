package com.example.phonebridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.data.CallState
import com.example.phonebridge.ui.components.CallStatusCard
import com.example.phonebridge.ui.components.ConnectionStatusCard
import com.example.phonebridge.ui.components.PhoneRoleCard
import com.example.phonebridge.ui.theme.AccentGreen
import com.example.phonebridge.ui.theme.PrimaryBlue
import com.example.phonebridge.ui.theme.TextSecondary

@Composable
fun MainDashboardScreen(
    isBackendConnected: Boolean,
    isAlexaConfigured: Boolean,
    callState: CallState,
    callerName: String?,
    phoneNumber: String?,
    isDefaultDialer: Boolean,
    statusMessage: String?,
    onTestBackend: () -> Unit,
    onTestAlexa: () -> Unit,
    onTestCallState: () -> Unit,
    onRequestPhoneRole: () -> Unit,
    onQuickSimulateCall: () -> Unit,
    onNavigateToTest: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // App Title & Subtitle
        Text(
            text = "PhoneBridge",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Alexa phone control",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        if (!statusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Status Card
        ConnectionStatusCard(
            isBackendConnected = isBackendConnected,
            isAlexaConfigured = isAlexaConfigured
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Call Status Card
        CallStatusCard(
            callState = callState,
            callerName = callerName,
            phoneNumber = phoneNumber
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Role & Permission Card
        PhoneRoleCard(
            isDefaultDialer = isDefaultDialer,
            onRequestRole = onRequestPhoneRole
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Testing & Diagnostics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Buttons
        Button(
            onClick = onTestBackend,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Backend Connection")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onTestAlexa,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Alexa Integration")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onTestCallState,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Test Call State")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick simulation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2838))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Simulation (Rahul)",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Simulate an incoming call from Rahul to test the complete Alexa interaction flow right away.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Button(
                        onClick = onQuickSimulateCall,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate Call", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToTest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Full Test Suite")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
