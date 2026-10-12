package com.example.phonebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ControlCenterScreen(
    isBridgeActive: Boolean,
    isEcoMode: Boolean,
    isBedtimeShield: Boolean,
    networkType: String,
    batteryLevel: Int,
    isCharging: Boolean,
    batteryTemp: Double,
    batteryVoltage: Int,
    batteryHealth: String,
    freeStorageGb: Long,
    totalStorageGb: Long,
    freeRamGb: String,
    totalRamGb: String,
    isFlashlightOn: Boolean,
    isAlarmPlaying: Boolean,
    soundMode: String,
    volumeLevel: Int,
    customPhrases: List<String>,
    onToggleBridge: (Boolean) -> Unit,
    onToggleEcoMode: (Boolean) -> Unit,
    onToggleBedtimeShield: (Boolean) -> Unit,
    onToggleFlashlight: () -> Unit,
    onStrobeFlashlight: () -> Unit,
    onToggleAlarm: () -> Unit,
    onSetSoundProfile: (String) -> Unit,
    onSetVolume: (Int) -> Unit,
    onSpeakTts: (String) -> Unit,
    onAddCustomPhrase: (String) -> Unit,
    onRemoveCustomPhrase: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onRefreshStats: () -> Unit
) {
    val scrollState = rememberScrollState()
    var ttsText by remember { mutableStateOf("") }
    var sliderValue by remember(volumeLevel) { mutableFloatStateOf(volumeLevel.toFloat()) }
    var isAddingPhrase by remember { mutableStateOf(false) }
    var newPhraseText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DEVICE CONTROL CENTER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Hardware, Sound, Power & Intercom Remote",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary
                )
            }

            IconButton(
                onClick = onRefreshStats,
                modifier = Modifier
                    .size(38.dp)
                    .background(CyberCard, CircleShape)
                    .border(1.dp, CyberCardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Diagnostics",
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Device Diagnostics Quick Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DiagnosticPill(
                icon = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                label = "$batteryLevel%",
                subLabel = if (isCharging) "Charging" else "Battery",
                accentColor = if (batteryLevel > 20) NeonGreen else ErrorRed,
                modifier = Modifier.weight(1f)
            )

            DiagnosticPill(
                icon = Icons.Default.DeviceThermostat,
                label = "${batteryTemp.toInt()}°C",
                subLabel = batteryHealth,
                accentColor = if (batteryTemp > 40) ErrorRed else CyanPrimary,
                modifier = Modifier.weight(1f)
            )

            DiagnosticPill(
                icon = Icons.Default.Storage,
                label = "${freeStorageGb}G",
                subLabel = "Storage",
                accentColor = CyanPrimary,
                modifier = Modifier.weight(1f)
            )

            DiagnosticPill(
                icon = Icons.Default.Memory,
                label = "${freeRamGb}G",
                subLabel = "RAM Free",
                accentColor = CyanPrimary,
                modifier = Modifier.weight(1f)
            )

            DiagnosticPill(
                icon = Icons.Default.Wifi,
                label = networkType,
                subLabel = "Network",
                accentColor = if (networkType == "Offline") ErrorRed else NeonGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Power & Eco Optimization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isBridgeActive) NeonGreen else TextMuted).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isBridgeActive) NeonGreen else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bridge Background Sync",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBridgeActive) "Sync Active (Listening for Alexa)" else "Paused (0% background battery)",
                                color = if (isBridgeActive) NeonGreen else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isBridgeActive,
                        onCheckedChange = onToggleBridge,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurface
                        )
                    )
                }

                HorizontalDivider(color = CyberCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isEcoMode) CyanPrimary else TextMuted).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EnergySavingsLeaf,
                                    contentDescription = null,
                                    tint = if (isEcoMode) CyanPrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Eco Battery Saver Mode",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isEcoMode) "90s Pings • Dynamic backoff active" else "Standard real-time response",
                                color = if (isEcoMode) CyanPrimary else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isEcoMode,
                        onCheckedChange = onToggleEcoMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurface
                        )
                    )
                }

                HorizontalDivider(color = CyberCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isBedtimeShield) Color(0xFFFFD700) else TextMuted).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (isBedtimeShield) Color(0xFFFFD700) else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bedtime Silent Shield",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBedtimeShield) "Shield Active • Alarms & noise muted" else "Normal • Alarms & speech allowed",
                                color = if (isBedtimeShield) Color(0xFFFFD700) else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isBedtimeShield,
                        onCheckedChange = onToggleBedtimeShield,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFD700),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hardware Controls: Torch, Strobe & Siren
        Text(
            text = "HARDWARE QUICK TOGGLES",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Flashlight Toggle Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleFlashlight() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlashlightOn) Color(0xFF2A2800) else CyberCard
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (isFlashlightOn) Color(0xFFFFD700) else CyberCardBorder,
                            if (isFlashlightOn) Color(0xFFFFD700) else CyberCardBorder
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (isFlashlightOn) Color(0xFFFFD700) else TextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TORCH",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFlashlightOn) "ON" else "OFF",
                        color = if (isFlashlightOn) Color(0xFFFFD700) else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // SOS Strobe Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStrobeFlashlight() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD700).copy(alpha = 0.5f), CyberCardBorder)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Strobe",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SOS STROBE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "8x BLINK",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Siren / Find My Phone Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleAlarm() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAlarmPlaying) Color(0xFF3B1010) else CyberCard
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (isAlarmPlaying) ErrorRed else CyberCardBorder,
                            if (isAlarmPlaying) ErrorRed else CyberCardBorder
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isAlarmPlaying) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Alarm",
                        tint = if (isAlarmPlaying) ErrorRed else TextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SIREN",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isAlarmPlaying) "RINGING" else "IDLE",
                        color = if (isAlarmPlaying) ErrorRed else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sound Profiles & Volume Card
        Text(
            text = "AUDIO & SOUND PROFILE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                    text = "Ringer Profile",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileButton(
                        label = "Silent",
                        icon = Icons.Default.VolumeOff,
                        isSelected = soundMode == "SILENT",
                        onClick = { onSetSoundProfile("SILENT") },
                        modifier = Modifier.weight(1f)
                    )
                    ProfileButton(
                        label = "Vibrate",
                        icon = Icons.Default.Vibration,
                        isSelected = soundMode == "VIBRATE",
                        onClick = { onSetSoundProfile("VIBRATE") },
                        modifier = Modifier.weight(1f)
                    )
                    ProfileButton(
                        label = "Normal",
                        icon = Icons.Default.VolumeDown,
                        isSelected = soundMode == "NORMAL",
                        onClick = { onSetSoundProfile("NORMAL") },
                        modifier = Modifier.weight(1f)
                    )
                    ProfileButton(
                        label = "Max Vol",
                        icon = Icons.Default.VolumeUp,
                        isSelected = soundMode == "MAX",
                        onClick = { onSetSoundProfile("MAX") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Volume Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ring Volume Level",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${sliderValue.toInt()}%",
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onSetVolume(sliderValue.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = CyberSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Intercom & Voice Broadcast Box
        Text(
            text = "VOICE INTERCOM / TTS BROADCAST",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                    text = "Speak Message on Phone Loudspeaker",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ttsText,
                    onValueChange = { ttsText = it },
                    placeholder = { Text("E.g., Dinner is ready, please come downstairs!") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Presets (Tap to use)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { isAddingPhrase = !isAddingPhrase },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (isAddingPhrase) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAddingPhrase) "Cancel" else "+ Add Preset",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isAddingPhrase) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPhraseText,
                            onValueChange = { newPhraseText = it },
                            placeholder = { Text("Enter phrase to speak...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (newPhraseText.isNotBlank()) {
                                    onAddCustomPhrase(newPhraseText.trim())
                                    newPhraseText = ""
                                    isAddingPhrase = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val phrasesToShow = if (customPhrases.isNotEmpty()) customPhrases else listOf("Dinner is ready!", "Time to wake up!", "Where are you?")
                    phrasesToShow.forEach { phrase ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurface,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { ttsText = phrase }
                                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phrase,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onRemoveCustomPhrase(phrase) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (ttsText.isNotBlank()) {
                            onSpeakTts(ttsText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Broadcast Message", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Direct App Shortcuts
        Text(
            text = "ONE-TAP APP LAUNCHER",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppShortcutTile("Camera", Icons.Default.CameraAlt) { onOpenApp("camera") }
            AppShortcutTile("Settings", Icons.Default.Settings) { onOpenApp("settings") }
            AppShortcutTile("YouTube", Icons.Default.PlayCircle) { onOpenApp("youtube") }
            AppShortcutTile("WhatsApp", Icons.Default.Chat) { onOpenApp("whatsapp") }
            AppShortcutTile("Maps", Icons.Default.Map) { onOpenApp("maps") }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun DiagnosticPill(
    icon: ImageVector,
    label: String,
    subLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = CyberCard,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subLabel,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ProfileButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else CyberSurface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isSelected) CyanPrimary else CyberCardBorder,
                    if (isSelected) CyanPrimary else CyberCardBorder
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyanPrimary else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun QuickPhraseChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = CyberSurface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
        )
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RowScope.AppShortcutTile(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = CyberCard,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = CyanPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
