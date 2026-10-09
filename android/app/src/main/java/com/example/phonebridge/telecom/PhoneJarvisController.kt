package com.example.phonebridge.telecom

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.phonebridge.data.CommandResponse
import java.net.URLEncoder
import java.util.Locale

class PhoneJarvisController(private val context: Context) {

    companion object {
        private const val TAG = "PhoneJarvisController"
        private const val ALARM_AUTO_STOP_TIMEOUT_MS = 90_000L // 90 seconds safety auto-shutoff
        private var mediaPlayer: MediaPlayer? = null
        private var isAlarmActive = false
        private var isFlashlightOn = false
        private var ttsInstance: TextToSpeech? = null
        private var isTtsReady = false
        private val mainHandler = Handler(Looper.getMainLooper())

        private val alarmTimeoutRunnable = Runnable {
            if (isAlarmActive) {
                Log.i(TAG, "Phone alarm reached 90s safety timeout. Auto-stopping to save battery.")
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (_: Exception) {}
                mediaPlayer = null
                isAlarmActive = false
            }
        }
    }

    init {
        initTts()
    }

    private fun initTts() {
        try {
            if (ttsInstance == null) {
                ttsInstance = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsInstance?.language = Locale.ENGLISH
                        isTtsReady = true
                        Log.i(TAG, "TextToSpeech engine initialized successfully")
                    } else {
                        Log.w(TAG, "TextToSpeech initialization returned status: $status")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS initialization error: ${e.message}")
        }
    }

    /**
     * 1. FIND MY PHONE: Maxes out alarm volume, vibrates, and loops alarm sound
     */
    fun findPhone(action: String?, requestId: String?): CommandResponse {
        val isStart = action.equals("START", ignoreCase = true) || action.isNullOrBlank()

        return try {
            if (isStart) {
                startPhoneAlarm()
                CommandResponse(
                    requestId = requestId,
                    command = "FIND_PHONE",
                    success = true,
                    reason = "Phone alarm ringing on maximum volume"
                )
            } else {
                stopPhoneAlarm()
                CommandResponse(
                    requestId = requestId,
                    command = "FIND_PHONE",
                    success = true,
                    reason = "Phone alarm stopped"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findPhone: ${e.message}", e)
            CommandResponse(
                requestId = requestId,
                command = "FIND_PHONE",
                success = false,
                reason = "Failed to toggle alarm: ${e.message}"
            )
        }
    }

    private fun startPhoneAlarm() {
        stopPhoneAlarm() // clean up any existing instance

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        }

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, alarmUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }

        // Start vibration
        triggerVibration(true)
        isAlarmActive = true

        // Schedule safety timeout to auto-stop after 90 seconds
        mainHandler.removeCallbacks(alarmTimeoutRunnable)
        mainHandler.postDelayed(alarmTimeoutRunnable, ALARM_AUTO_STOP_TIMEOUT_MS)
    }

    private fun stopPhoneAlarm() {
        mainHandler.removeCallbacks(alarmTimeoutRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        triggerVibration(false)
        isAlarmActive = false
    }

    private fun triggerVibration(start: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (start) {
                val timings = longArrayOf(0, 800, 300, 800, 300)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, 0)
                }
            } else {
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration control error: ${e.message}")
        }
    }

    /**
     * 2. GET BATTERY STATUS: Returns real-time battery capacity & charging state
     */
    fun getBatteryStatus(requestId: String?): CommandResponse {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 0

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val data = mapOf(
                "batteryLevel" to batteryPct,
                "isCharging" to isCharging
            )

            CommandResponse(
                requestId = requestId,
                command = "GET_BATTERY",
                success = true,
                data = data
            )
        } catch (e: Exception) {
            CommandResponse(
                requestId = requestId,
                command = "GET_BATTERY",
                success = false,
                reason = "Error reading battery: ${e.message}"
            )
        }
    }

    /**
     * 3. SET FLASHLIGHT: Toggles LED torch
     */
    fun setFlashlight(enable: Boolean, requestId: String?): CommandResponse {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                isFlashlightOn = enable
                CommandResponse(
                    requestId = requestId,
                    command = "SET_FLASHLIGHT",
                    success = true,
                    reason = "Flashlight set to $enable"
                )
            } else {
                CommandResponse(
                    requestId = requestId,
                    command = "SET_FLASHLIGHT",
                    success = false,
                    reason = "No camera with flash available"
                )
            }
        } catch (e: Exception) {
            CommandResponse(
                requestId = requestId,
                command = "SET_FLASHLIGHT",
                success = false,
                reason = "Flashlight error: ${e.message}"
            )
        }
    }

    fun isFlashlightActive(): Boolean = isFlashlightOn
    fun isAlarmActive(): Boolean = isAlarmActive

    fun toggleFlashlight(): Boolean {
        val target = !isFlashlightOn
        setFlashlight(target, null)
        return isFlashlightOn
    }

    fun toggleAlarm(): Boolean {
        if (isAlarmActive) {
            stopPhoneAlarm()
        } else {
            startPhoneAlarm()
        }
        return isAlarmActive
    }

    /**
     * 4. MAKE OUTGOING CALL: Resolves contact name or cleans number and places call
     */
    fun makeCall(target: String?, requestId: String?): CommandResponse {
        if (target.isNullOrBlank()) {
            return CommandResponse(requestId, "MAKE_CALL", false, "Target phone number or contact name is required.")
        }

        return try {
            val isNumeric = target.replace(Regex("[^0-9+]"), "").length >= 3
            val numberToDial = if (isNumeric && target.any { it.isDigit() }) {
                target.trim()
            } else {
                CallerResolver.resolvePhoneNumberByName(context, target)
                    ?: return CommandResponse(requestId, "MAKE_CALL", false, "Could not find contact '$target' in phone contacts.")
            }

            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:${Uri.encode(numberToDial)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            CommandResponse(
                requestId = requestId,
                command = "MAKE_CALL",
                success = true,
                reason = "Calling $numberToDial"
            )
        } catch (e: Exception) {
            CommandResponse(
                requestId = requestId,
                command = "MAKE_CALL",
                success = false,
                reason = "Failed to place call: ${e.message}"
            )
        }
    }

    /**
     * 5. SEND WHATSAPP: Opens WhatsApp with pre-filled text or dispatches
     */
    fun sendWhatsApp(target: String?, message: String?, requestId: String?): CommandResponse {
        if (target.isNullOrBlank() || message.isNullOrBlank()) {
            return CommandResponse(requestId, "SEND_WHATSAPP", false, "Target and message are required.")
        }

        return try {
            val rawNumber = if (target.any { it.isDigit() } && target.replace(Regex("[^0-9]"), "").length >= 7) {
                target
            } else {
                CallerResolver.resolvePhoneNumberByName(context, target)
                    ?: return CommandResponse(requestId, "SEND_WHATSAPP", false, "Could not find contact '$target' for WhatsApp.")
            }

            var cleanNumber = rawNumber.replace(Regex("[^0-9]"), "")
            if (cleanNumber.length == 10) {
                cleanNumber = "91$cleanNumber"
            }

            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMessage"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.whatsapp")
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
            } else {
                intent.setPackage(null)
                context.startActivity(intent)
            }

            CommandResponse(
                requestId = requestId,
                command = "SEND_WHATSAPP",
                success = true,
                reason = "WhatsApp message prepared for $target: $message"
            )
        } catch (e: Exception) {
            CommandResponse(
                requestId = requestId,
                command = "SEND_WHATSAPP",
                success = false,
                reason = "Failed to send WhatsApp: ${e.message}"
            )
        }
    }

    /**
     * 6. OPEN APP: Launches any requested application or system tool
     */
    fun openApp(appName: String?, requestId: String?): CommandResponse {
        if (appName.isNullOrBlank()) {
            return CommandResponse(requestId, "OPEN_APP", false, "App name is required.")
        }

        val query = appName.trim().lowercase(Locale.ROOT)
        val packageManager = context.packageManager

        // Special system handlers
        if (query == "camera") {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                return CommandResponse(requestId, "OPEN_APP", true, "Opened Camera")
            }
        } else if (query == "settings") {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return CommandResponse(requestId, "OPEN_APP", true, "Opened Settings")
        }

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "spotify" to "com.spotify.music",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "calculator" to "com.google.android.calculator",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos",
            "gmail" to "com.google.android.gm",
            "play store" to "com.android.vending",
            "telegram" to "org.telegram.messenger",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android"
        )

        var targetPackage = knownPackages[query]

        // If not found in known map, search installed applications dynamically
        if (targetPackage == null || packageManager.getLaunchIntentForPackage(targetPackage) == null) {
            try {
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in installedApps) {
                    val label = packageManager.getApplicationLabel(app).toString().lowercase(Locale.ROOT)
                    if (label == query || label.contains(query) || query.contains(label)) {
                        if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                            targetPackage = app.packageName
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search installed apps error: ${e.message}")
            }
        }

        if (targetPackage != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return CommandResponse(requestId, "OPEN_APP", true, "Opened $appName")
            }
        }

        return CommandResponse(requestId, "OPEN_APP", false, "Could not find app '$appName' installed on your phone.")
    }

    /**
     * 7. SET VOLUME / RINGER MODE: Silent, Vibrate, Normal, Max volume
     */
    fun setVolumeOrMode(mode: String?, level: Int?, requestId: String?): CommandResponse {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return CommandResponse(requestId, "SET_VOLUME", false, "Audio service unavailable")

        val upperMode = mode?.uppercase(Locale.ROOT) ?: ""

        return try {
            when {
                upperMode.contains("SILENT") || upperMode.contains("MUTE") -> {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager?.isNotificationPolicyAccessGranted == true) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } else {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    }
                    CommandResponse(requestId, "SET_VOLUME", true, "Phone set to silent mode")
                }
                upperMode.contains("VIBRATE") -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    CommandResponse(requestId, "SET_VOLUME", true, "Phone set to vibrate mode")
                }
                upperMode.contains("NORMAL") || upperMode.contains("RING") -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val halfVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, halfVol, 0)
                    CommandResponse(requestId, "SET_VOLUME", true, "Phone set to normal ring mode")
                }
                upperMode.contains("MAX") || upperMode.contains("LOUD") -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val maxNotif = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotif, 0)
                    CommandResponse(requestId, "SET_VOLUME", true, "Phone volume set to maximum")
                }
                level != null -> {
                    val clamped = level.coerceIn(0, 100)
                    val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    val targetVol = ((clamped / 100.0) * maxRing).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0)
                    CommandResponse(requestId, "SET_VOLUME", true, "Phone volume set to $clamped percent")
                }
                else -> {
                    CommandResponse(requestId, "SET_VOLUME", false, "Unrecognized mode or volume level")
                }
            }
        } catch (e: Exception) {
            CommandResponse(requestId, "SET_VOLUME", false, "Volume change error: ${e.message}")
        }
    }

    /**
     * 8. VOICE BROADCAST / TTS INTERCOM: Speaks custom message through phone loudspeaker
     */
    fun speakMessage(message: String?, requestId: String?): CommandResponse {
        if (message.isNullOrBlank()) {
            return CommandResponse(requestId, "SPEAK_MESSAGE", false, "Message cannot be empty.")
        }

        return try {
            // Maximize media audio stream
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            }

            if (ttsInstance == null || !isTtsReady) {
                ttsInstance = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsInstance?.language = Locale.ENGLISH
                        isTtsReady = true
                        ttsInstance?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts")
                    }
                }
            } else {
                ttsInstance?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts")
            }

            CommandResponse(
                requestId = requestId,
                command = "SPEAK_MESSAGE",
                success = true,
                reason = "Broadcast message announced on phone: $message"
            )
        } catch (e: Exception) {
            CommandResponse(requestId, "SPEAK_MESSAGE", false, "Failed to speak message: ${e.message}")
        }
    }

    /**
     * 9. DEVICE STATS: Storage & Available RAM
     */
    fun getDeviceStats(requestId: String?): CommandResponse {
        return try {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalStorageGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
            val freeStorageGb = (availableBlocks * blockSize) / (1024 * 1024 * 1024)

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            val totalRamGb = String.format(Locale.ROOT, "%.1f", memoryInfo.totalMem / (1024.0 * 1024 * 1024))
            val freeRamGb = String.format(Locale.ROOT, "%.1f", memoryInfo.availMem / (1024.0 * 1024 * 1024))

            val data = mapOf(
                "totalStorageGb" to totalStorageGb,
                "freeStorageGb" to freeStorageGb,
                "totalRamGb" to totalRamGb,
                "freeRamGb" to freeRamGb
            )

            CommandResponse(
                requestId = requestId,
                command = "GET_DEVICE_STATS",
                success = true,
                data = data,
                reason = "Free storage: ${freeStorageGb}GB / ${totalStorageGb}GB, Available RAM: ${freeRamGb}GB"
            )
        } catch (e: Exception) {
            CommandResponse(requestId, "GET_DEVICE_STATS", false, "Failed reading device stats: ${e.message}")
        }
    }

    /**
     * 10. DIRECT SMS: Sends SMS text hands-free
     */
    fun sendSms(target: String?, message: String?, requestId: String?): CommandResponse {
        if (target.isNullOrBlank() || message.isNullOrBlank()) {
            return CommandResponse(requestId, "SEND_SMS", false, "Recipient and message are required.")
        }

        return try {
            val number = if (target.any { it.isDigit() } && target.replace(Regex("[^0-9]"), "").length >= 3) {
                target.trim()
            } else {
                CallerResolver.resolvePhoneNumberByName(context, target)
                    ?: return CommandResponse(requestId, "SEND_SMS", false, "Could not find contact '$target'")
            }

            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSmsPermission) {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(number, null, message, null, null)
                CommandResponse(requestId, "SEND_SMS", true, "SMS sent to $target: $message")
            } else {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
                CommandResponse(requestId, "SEND_SMS", true, "SMS prepared for $target")
            }
        } catch (e: Exception) {
            CommandResponse(requestId, "SEND_SMS", false, "Failed to send SMS: ${e.message}")
        }
    }
}
