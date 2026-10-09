package com.example.phonebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.data.CallState
import com.example.phonebridge.ui.components.CallStatusCard
import com.example.phonebridge.ui.components.ConnectionStatusCard
import com.example.phonebridge.ui.components.JarvisCapabilitiesShowcase
import com.example.phonebridge.ui.components.PhoneRoleCard
import com.example.phonebridge.ui.theme.*

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
    onNavigateToControls: () -> Unit,
    onNavigateToTest: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Futuristic Top Header with Core Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = CyanPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "PHONEBRIDGE JARVIS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Alexa Echo Physical Bridge • 24/7 Cloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary
                )
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyanPrimary.copy(alpha = 0.12f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyanPrimary.copy(alpha = 0.4f), Color.Transparent))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Status Card
        ConnectionStatusCard(
            isBackendConnected = isBackendConnected,
            isAlexaConfigured = isAlexaConfigured
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Call Status Card
        CallStatusCard(
            callState = callState,
            callerName = callerName,
            phoneNumber = phoneNumber
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Role & Permission Card
        PhoneRoleCard(
            isDefaultDialer = isDefaultDialer,
            onRequestRole = onRequestPhoneRole
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 10 JARVIS capabilities showcase grid
        JarvisCapabilitiesShowcase()

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onNavigateToControls,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Remote Controls & Diagnostics", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Quick Simulation & Diagnostics Section
        Text(
            text = "Testing & Simulator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onTestBackend,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ping Cloud", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onTestAlexa,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
                )
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Alexa", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick simulation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Quick Simulation (Rahul)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simulate an incoming phone call to test hands-free caller inquiry and answering on your Echo Dot.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onQuickSimulateCall,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate Call", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToTest,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("All Test Cases", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
