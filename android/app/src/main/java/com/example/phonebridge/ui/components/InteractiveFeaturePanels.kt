package com.example.phonebridge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.phonebridge.data.JarvisFeature
import com.example.phonebridge.data.JarvisFeatureRegistry
import com.example.phonebridge.data.PreferencesManager
import com.example.phonebridge.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveFeaturePanels(
    featureStates: Map<String, Boolean>,
    onToggleFeature: (String, Boolean) -> Unit,
    onTestFlashlight: (() -> Unit)? = null,
    onTestAlarm: (() -> Unit)? = null,
    onTestSimulateCall: (() -> Unit)? = null,
    onTestSoundMode: ((String) -> Unit)? = null,
    onTestTts: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedFeatureId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "JARVIS FEATURE CONTROL PANELS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap any panel to view all voice commands & toggle Alexa control",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        JarvisFeatureRegistry.features.forEach { feature ->
            val isEnabled = featureStates[feature.id] ?: true
            val isExpanded = expandedFeatureId == feature.id

            FeaturePanelCard(
                feature = feature,
                isEnabled = isEnabled,
                isExpanded = isExpanded,
                onCardClick = {
                    expandedFeatureId = if (isExpanded) null else feature.id
                },
                onToggle = { enabled ->
                    onToggleFeature(feature.id, enabled)
                },
                onTestFlashlight = onTestFlashlight,
                onTestAlarm = onTestAlarm,
                onTestSimulateCall = onTestSimulateCall,
                onTestSoundMode = onTestSoundMode,
                onTestTts = onTestTts
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun FeaturePanelCard(
    feature: JarvisFeature,
    isEnabled: Boolean,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTestFlashlight: (() -> Unit)?,
    onTestAlarm: (() -> Unit)?,
    onTestSimulateCall: (() -> Unit)?,
    onTestSoundMode: ((String) -> Unit)?,
    onTestTts: ((String) -> Unit)?
) {
    val icon = getFeatureIcon(feature.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFF161F2E) else CyberCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isExpanded) {
                    listOf(CyanPrimary.copy(alpha = 0.6f), NeonGreen.copy(alpha = 0.4f))
                } else if (isEnabled) {
                    listOf(CyberCardBorder, CyberCardBorder)
                } else {
                    listOf(ErrorRed.copy(alpha = 0.3f), CyberCardBorder)
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row (Always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (isEnabled) CyanPrimary else TextMuted).copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isEnabled) CyanPrimary else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = feature.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = feature.summary,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = if (isExpanded) 3 else 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Alexa Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEnabled) NeonGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isEnabled) "ALEXA ON" else "ALEXA OFF",
                            color = if (isEnabled) NeonGreen else ErrorRed,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = if (isExpanded) CyanPrimary else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expandable Content Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = CyberCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Alexa Control Master Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Alexa to Control This Feature",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isEnabled) {
                                    "Alexa Echo can trigger and execute this action on your phone."
                                } else {
                                    "Disabled: Alexa will decline commands and state it's turned off in app."
                                },
                                color = if (isEnabled) NeonGreen else ErrorRed,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { onToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = CyberSurface
                            )
                        )
                    }

                    // 2. Direct In-App Action / Testing Shortcut (if applicable)
                    FeatureActionButtons(
                        featureId = feature.id,
                        onTestFlashlight = onTestFlashlight,
                        onTestAlarm = onTestAlarm,
                        onTestSimulateCall = onTestSimulateCall,
                        onTestSoundMode = onTestSoundMode,
                        onTestTts = onTestTts
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Super-Short English Voice Commands
                    Text(
                        text = "⚡ ULTRA-SHORT ENGLISH COMMANDS",
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    CommandChipsRow(commands = feature.shortCommands, accentColor = CyanPrimary)

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Natural Hinglish / Hindi Voice Commands
                    Text(
                        text = "🇮🇳 NATURAL HINGLISH / HINDI COMMANDS",
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    CommandChipsRow(commands = feature.hinglishCommands, accentColor = NeonGreen)

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Spoken Format Example Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberSurface,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Speak to Alexa:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${feature.spokenFormat}\"",
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (feature.tip.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 Tip: ${feature.tip}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommandChipsRow(commands: List<String>, accentColor: Color) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        commands.forEach { cmd ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyberSurface,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.1f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"$cmd\"",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureActionButtons(
    featureId: String,
    onTestFlashlight: (() -> Unit)?,
    onTestAlarm: (() -> Unit)?,
    onTestSimulateCall: (() -> Unit)?,
    onTestSoundMode: ((String) -> Unit)?,
    onTestTts: ((String) -> Unit)?
) {
    when (featureId) {
        PreferencesManager.FEATURE_FLASHLIGHT -> {
            if (onTestFlashlight != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onTestFlashlight,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Toggle Flashlight Test", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        PreferencesManager.FEATURE_FIND_PHONE -> {
            if (onTestAlarm != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onTestAlarm,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger / Stop Alarm Siren", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        PreferencesManager.FEATURE_CALL -> {
            if (onTestSimulateCall != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onTestSimulateCall,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate Test Call (Rahul)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        PreferencesManager.FEATURE_VOLUME -> {
            if (onTestSoundMode != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onTestSoundMode("SILENT") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Silent", fontSize = 11.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onTestSoundMode("VIBRATE") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Vibrate", fontSize = 11.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onTestSoundMode("MAX") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Max", fontSize = 11.sp, color = CyanPrimary)
                    }
                }
            }
        }
        PreferencesManager.FEATURE_INTERCOM -> {
            if (onTestTts != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onTestTts("Dinner is ready! PhoneBridge voice test.") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Spoken Announcement", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun getFeatureIcon(featureId: String): ImageVector {
    return when (featureId) {
        PreferencesManager.FEATURE_FIND_PHONE -> Icons.Default.NotificationsActive
        PreferencesManager.FEATURE_FLASHLIGHT -> Icons.Default.FlashlightOn
        PreferencesManager.FEATURE_CALL -> Icons.Default.PhoneInTalk
        PreferencesManager.FEATURE_MAKE_CALL -> Icons.Default.Phone
        PreferencesManager.FEATURE_VOLUME -> Icons.Default.VolumeUp
        PreferencesManager.FEATURE_BATTERY -> Icons.Default.BatteryChargingFull
        PreferencesManager.FEATURE_DEVICE_STATS -> Icons.Default.Storage
        PreferencesManager.FEATURE_INTERCOM -> Icons.Default.RecordVoiceOver
        PreferencesManager.FEATURE_WHATSAPP -> Icons.Default.Chat
        PreferencesManager.FEATURE_SMS -> Icons.Default.Sms
        PreferencesManager.FEATURE_OPEN_APP -> Icons.Default.Apps
        else -> Icons.Default.SmartToy
    }
}
