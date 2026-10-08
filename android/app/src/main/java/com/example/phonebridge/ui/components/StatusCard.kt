package com.example.phonebridge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonebridge.data.CallState
import com.example.phonebridge.ui.theme.*

@Composable
fun ConnectionStatusCard(
    isBackendConnected: Boolean,
    isAlexaConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isBackendConnected) CyanPrimary.copy(alpha = 0.4f) else CyberCardBorder,
                    if (isBackendConnected) NeonGreen.copy(alpha = 0.2f) else CyberCardBorder
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBackendConnected) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(NeonGreen.copy(alpha = 0.25f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isBackendConnected) NeonGreen else NeonRed)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isBackendConnected) "JARVIS Cloud Active" else "Offline / Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBackendConnected) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isBackendConnected) "24/7 ONLINE" else "OFFLINE",
                        color = if (isBackendConnected) NeonGreen else NeonRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = CyberCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Cloud Server row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Cloud Backend", color = TextSecondary, fontSize = 13.sp)
                Text(
                    text = "Render.com (WSS)",
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alexa Skill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Alexa Voice Skill", color = TextSecondary, fontSize = 13.sp)
                Text(
                    text = "Mobile Buddy (Live)",
                    color = if (isAlexaConfigured) NeonGreen else TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (callState == CallState.RINGING) NeonGreen.copy(alpha = 0.6f) else CyberCardBorder,
                    CyberCardBorder
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Telephony State",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (callState) {
                CallState.IDLE, CallState.DISCONNECTED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyberSurface,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Standby • Ready", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(text = "Waiting for incoming or outgoing calls", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                CallState.RINGING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Incoming Call Ringing",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = callerName ?: "Unknown caller",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (!phoneNumber.isNullOrBlank()) {
                                Text(text = phoneNumber, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                CallState.ACTIVE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyanPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Active Call (Speakerphone)",
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = callerName ?: phoneNumber ?: "Active Connection",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        }
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefaultDialer) Color(0xFF0D251A) else Color(0xFF231B10)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isDefaultDialer) NeonGreen.copy(alpha = 0.4f) else NeonAmber.copy(alpha = 0.4f),
                    Color.Transparent
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isDefaultDialer) NeonGreen else NeonAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isDefaultDialer) "Default Telecom Dialer Active" else "Dialer Role Recommended",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isDefaultDialer) {
                    "Full call management enabled. Alexa can answer and reject calls on your device seamlessly."
                } else {
                    "Set PhoneBridge as Default Phone App for hands-free answering & auto-speakerphone when commanded by Alexa."
                },
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (!isDefaultDialer) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestRole,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Grant Phone Role", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun JarvisCapabilitiesShowcase() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "JARVIS Voice Capabilities",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Ready to control hands-free via physical Alexa Echo",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val skills = listOf(
            Triple(Icons.Default.NotificationsActive, "Find My Phone", "Siren, vibration & strobe on voice"),
            Triple(Icons.Default.FlashlightOn, "Flashlight", "Hands-free camera torch toggle"),
            Triple(Icons.Default.BatteryChargingFull, "Battery & Power", "Real-time percentage & charging state"),
            Triple(Icons.Default.VolumeUp, "Sound Profile", "Silent, Vibrate, Normal & Max volume"),
            Triple(Icons.Default.RecordVoiceOver, "Voice Intercom", "Broadcast TTS speech on phone"),
            Triple(Icons.Default.Storage, "Storage & RAM", "Query remaining GB & free memory"),
            Triple(Icons.Default.Apps, "App Launcher", "Launch YouTube, Spotify, Camera, etc."),
            Triple(Icons.Default.Phone, "Hands-Free Call", "Dial contact & auto-speakerphone"),
            Triple(Icons.Default.Chat, "Voice WhatsApp", "Compose & dispatch messages hands-free"),
            Triple(Icons.Default.Sms, "Voice SMS", "Direct carrier text messaging")
        )

        skills.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (icon, title, desc) ->
                    SkillBadge(
                        icon = icon,
                        title = title,
                        desc = desc,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SkillBadge(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = CyberCard,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder)))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = CyanPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(text = desc, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
