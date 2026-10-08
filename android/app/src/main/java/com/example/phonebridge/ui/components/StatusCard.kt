package com.example.phonebridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun ConnectionStatusCard(
    isBackendConnected: Boolean,
    isAlexaConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Backend Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isBackendConnected) AccentGreen else ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Backend", color = TextSecondary, fontSize = 14.sp)
                }
                Text(
                    text = if (isBackendConnected) "Connected" else "Disconnected",
                    color = if (isBackendConnected) AccentGreen else ErrorRed,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alexa Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAlexaConfigured) AccentGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Alexa Integration", color = TextSecondary, fontSize = 14.sp)
                }
                Text(
                    text = if (isAlexaConfigured) "Connected" else "Not configured",
                    color = if (isAlexaConfigured) AccentGreen else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CallStatusCard(
    callState: CallState,
    callerName: String?,
    phoneNumber: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Call Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (callState) {
                CallState.IDLE, CallState.DISCONNECTED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No active call",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
                CallState.RINGING -> {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Incoming call ringing...",
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "From: ${callerName ?: "Unknown caller"}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        if (!phoneNumber.isNullOrBlank()) {
                            Text(
                                text = phoneNumber,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                CallState.ACTIVE -> {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call active",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "With: ${callerName ?: phoneNumber ?: "Unknown"}",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneRoleCard(
    isDefaultDialer: Boolean,
    onRequestRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefaultDialer) Color(0xFF1B382B) else Color(0xFF382E1B)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isDefaultDialer) AccentGreen else Color(0xFFFFB74D),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDefaultDialer) "Default Phone Role Active" else "Phone Role Required for Answering",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isDefaultDialer) {
                    "PhoneBridge has the official InCallService dialer role. Alexa can answer and reject active calls."
                } else {
                    "Android restricts background apps from answering/rejecting calls. Granting PhoneBridge the Default Phone role enables full Telecom control, or you can use Test Mode."
                },
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (!isDefaultDialer) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestRole,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Set as Default Phone App", fontSize = 13.sp)
                }
            }
        }
    }
}
