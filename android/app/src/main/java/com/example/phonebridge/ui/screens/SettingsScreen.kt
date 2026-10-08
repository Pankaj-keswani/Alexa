package com.example.phonebridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.ui.theme.CardBackground
import com.example.phonebridge.ui.theme.PrimaryBlue
import com.example.phonebridge.ui.theme.TextSecondary

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
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Configure server connection & preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Backend URL
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Backend URL") },
            placeholder = { Text("http://10.0.2.2:3000 or https://...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.DarkGray
            )
        )
        Text(
            text = "Tip: For Android emulator use http://10.0.2.2:3000. For physical devices use local IP or HTTPS tunnel (ngrok).",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Device Auth Token
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Device Authentication Token") },
            placeholder = { Text("Matches DEVICE_TOKEN in backend .env") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.DarkGray
            )
        )
        Text(
            text = "Shared secret protecting backend REST & WebSocket endpoints.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Device ID
        OutlinedTextField(
            value = deviceId,
            onValueChange = { deviceId = it },
            label = { Text("Device ID") },
            placeholder = { Text("e.g. phone-pixel7") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Notifications toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable call notifications", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Show ongoing heads-up notification with action buttons", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = notifs, onCheckedChange = { notifs = it })
                }

                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                // Caller announcement toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable caller announcement", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Send caller contact name to backend for Alexa", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = announce, onCheckedChange = { announce = it })
                }

                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                // Debug logging toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Debug logging", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Record tagged events on the Developer Log screen", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = debug, onCheckedChange = { debug = it })
                }

                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                // Test Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Test Mode (Simulated Telecom)", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Simulate answering & rejecting calls safely without cellular access", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = testMode, onCheckedChange = { testMode = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSaveSettings(url, token, deviceId, notifs, announce, debug, testMode)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Save & Reconnect", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
