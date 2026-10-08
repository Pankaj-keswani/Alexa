package com.example.phonebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUrl: String,
    currentToken: String,
    currentDeviceId: String,
    currentNotifs: Boolean,
    currentAnnounce: Boolean,
    currentDebug: Boolean,
    currentTestMode: Boolean,
    onSaveSettings: (
        url: String,
        token: String,
        deviceId: String,
        notifs: Boolean,
        announce: Boolean,
        debug: Boolean,
        testMode: Boolean
    ) -> Unit
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    var token by remember(currentToken) { mutableStateOf(currentToken) }
    var deviceId by remember(currentDeviceId) { mutableStateOf(currentDeviceId) }
    var notifs by remember(currentNotifs) { mutableStateOf(currentNotifs) }
    var announce by remember(currentAnnounce) { mutableStateOf(currentAnnounce) }
    var debug by remember(currentDebug) { mutableStateOf(currentDebug) }
    var testMode by remember(currentTestMode) { mutableStateOf(currentTestMode) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Cloud & App Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Connected to Render 24/7 Cloud Server",
            style = MaterialTheme.typography.bodyMedium,
            color = CyanPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Backend URL Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backend Server URL",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://alexa-phonebridge.onrender.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Default: https://alexa-phonebridge.onrender.com (Live Render 24/7 cloud)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Auth & Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Authentication & Device Identity",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Device Secret Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Notifications toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Call Notifications", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show ongoing heads-up notification with action buttons", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = notifs,
                        onCheckedChange = { notifs = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyanPrimary)
                    )
                }

                Divider(color = CyberCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Caller announcement toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Caller Announcement", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Forward caller name to Alexa Echo for voice announcement", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = announce,
                        onCheckedChange = { announce = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyanPrimary)
                    )
                }

                Divider(color = CyberCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Debug logging toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Developer Activity Logging", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Record real-time Alexa voice logs on device", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = debug,
                        onCheckedChange = { debug = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyanPrimary)
                    )
                }

                Divider(color = CyberCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Test Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Test Mode (Simulated Calls)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Simulate answering & rejecting calls safely", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = testMode,
                        onCheckedChange = { testMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSaveSettings(url, token, deviceId, notifs, announce, debug, testMode)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
        ) {
            Text("Save & Reconnect", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
