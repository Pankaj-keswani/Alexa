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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterScreen(
    isBridgeActive: Boolean,
    isEcoMode: Boolean,
    networkType: String,
    batteryLevel: Int,
    isCharging: Boolean,
    freeStorageGb: Long,
    totalStorageGb: Long,
    freeRamGb: String,
    totalRamGb: String,
    isFlashlightOn: Boolean,
    isAlarmPlaying: Boolean,
    soundMode: String,
    volumeLevel: Int,
    onToggleBridge: (Boolean) -> Unit,
    onToggleEcoMode: (Boolean) -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleAlarm: () -> Unit,
    onSetSoundProfile: (String) -> Unit,
    onSetVolume: (Int) -> Unit,
    onSpeakTts: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onRefreshStats: () -> Unit
) {
    val scrollState = rememberScrollState()
    var ttsText by remember { mutableStateOf("") }
    var sliderValue by remember(volumeLevel) { mutableFloatStateOf(volumeLevel.toFloat()) }

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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiagnosticPill(
                icon = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                label = "$batteryLevel%",
                subLabel = if (isCharging) "Charging" else "Battery",
                accentColor = if (batteryLevel > 20) NeonGreen else ErrorRed,
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

                Divider(color = CyberCardBorder, modifier = Modifier.padding(vertical = 12.dp))

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
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hardware Controls: Torch & Siren
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (isFlashlightOn) Color(0xFFFFD700) else TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "FLASHLIGHT",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isFlashlightOn) "STATUS: ON" else "STATUS: OFF",
                        color = if (isFlashlightOn) Color(0xFFFFD700) else TextMuted,
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
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isAlarmPlaying) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Alarm",
                        tint = if (isAlarmPlaying) ErrorRed else TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "FIND PHONE SIREN",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isAlarmPlaying) "RINGING (90s max)" else "IDLE",
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPhraseChip("Dinner is ready!") { ttsText = "Dinner is ready!" }
                    QuickPhraseChip("Time to wake up!") { ttsText = "Time to wake up!" }
                    QuickPhraseChip("Where are you?") { ttsText = "Where are you?" }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
