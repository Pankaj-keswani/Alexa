package com.example.phonebridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.data.CallState
import com.example.phonebridge.ui.theme.AccentGreen
import com.example.phonebridge.ui.theme.CardBackground
import com.example.phonebridge.ui.theme.ErrorRed
import com.example.phonebridge.ui.theme.PrimaryBlue
import com.example.phonebridge.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSimulationScreen(
    currentCallState: CallState,
    currentCaller: String?,
    onSimulateIncoming: (caller: String, number: String) -> Unit,
    onSimulateAnswer: () -> Unit,
    onSimulateReject: () -> Unit,
    onSimulateEndCall: () -> Unit
) {
    var callerName by remember { mutableStateOf("Rahul") }
    var phoneNumber by remember { mutableStateOf("+919876543210") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Test Mode Simulation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Test the complete Alexa pipeline without real phone calls",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // State indicator
        Surface(
            color = when (currentCallState) {
                CallState.RINGING -> Color(0xFF1B382B)
                CallState.ACTIVE -> Color(0xFF1A334B)
                else -> CardBackground
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current State: $currentCallState",
                    fontWeight = FontWeight.Bold,
                    color = when (currentCallState) {
                        CallState.RINGING -> AccentGreen
                        CallState.ACTIVE -> PrimaryBlue
                        else -> Color.White
                    },
                    fontSize = 16.sp
                )
                if (currentCallState != CallState.IDLE) {
                    Text(
                        text = "Caller: ${currentCaller ?: "Unknown"}",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simulation parameters
        Text(
            text = "1. Configure Simulated Caller",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = callerName,
            onValueChange = { callerName = it },
            label = { Text("Caller Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Text(
            text = "2. Trigger Events",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onSimulateIncoming(callerName, phoneNumber) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Incoming Call", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSimulateAnswer,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = currentCallState == CallState.RINGING
            ) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Answer")
            }

            Button(
                onClick = onSimulateReject,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                enabled = currentCallState == CallState.RINGING
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reject")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onSimulateEndCall,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = currentCallState != CallState.IDLE
        ) {
            Text("End / Hang Up Call")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step by step walkthrough card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Alexa Voice Verification Flow",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Click 'Simulate Incoming Call' above.\n" +
                            "2. Say to your Echo or Alexa test console:\n" +
                            "   \"Alexa, ask Phone Bridge who is calling\"\n" +
                            "   -> Alexa answers: \"You have an incoming call from $callerName.\"\n" +
                            "3. Then say:\n" +
                            "   \"Alexa, ask Phone Bridge to answer\"\n" +
                            "   -> Alexa answers: \"Okay, I answered the call.\"\n" +
                            "4. Or say:\n" +
                            "   \"Alexa, ask Phone Bridge to reject\"\n" +
                            "   -> Alexa answers: \"Okay, I rejected the call.\"",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
