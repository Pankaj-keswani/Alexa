package com.example.phonebridge.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("phonebridge_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ENABLE_NOTIFICATIONS = "enable_call_notifications"
        private const val KEY_ENABLE_ANNOUNCEMENT = "enable_caller_announcement"
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_IS_TEST_MODE = "is_test_mode"
        private const val KEY_IS_BRIDGE_ENABLED = "is_bridge_enabled"
        private const val KEY_IS_ECO_MODE = "is_eco_mode"
        private const val KEY_PING_INTERVAL = "ping_interval_seconds"
        private const val DEFAULT_BACKEND_URL = "https://alexa-phonebridge.onrender.com"

        // Feature keys for Alexa voice control permissions
        const val FEATURE_FIND_PHONE = "find_phone"
        const val FEATURE_FLASHLIGHT = "flashlight"
        const val FEATURE_CALL = "call"
        const val FEATURE_MAKE_CALL = "make_call"
        const val FEATURE_VOLUME = "volume"
        const val FEATURE_BATTERY = "battery"
        const val FEATURE_DEVICE_STATS = "device_stats"
        const val FEATURE_INTERCOM = "intercom"
        const val FEATURE_WHATSAPP = "whatsapp"
        const val FEATURE_SMS = "sms"
        const val FEATURE_OPEN_APP = "open_app"

        val ALL_FEATURES = listOf(
            FEATURE_FIND_PHONE,
            FEATURE_FLASHLIGHT,
            FEATURE_CALL,
            FEATURE_MAKE_CALL,
            FEATURE_VOLUME,
            FEATURE_BATTERY,
            FEATURE_DEVICE_STATS,
            FEATURE_INTERCOM,
            FEATURE_WHATSAPP,
            FEATURE_SMS,
            FEATURE_OPEN_APP
        )
    }

    var backendUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value.trim()).apply()

    var deviceToken: String
        get() = prefs.getString(KEY_DEVICE_TOKEN, "dev_token_secret_123") ?: "dev_token_secret_123"
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value.trim()).apply()

    var deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = "phone-" + UUID.randomUUID().toString().substring(0, 8)
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value.trim()).apply()

    var enableCallNotifications: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_NOTIFICATIONS, value).apply()

    var enableCallerAnnouncement: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_ANNOUNCEMENT, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_ANNOUNCEMENT, value).apply()

    var debugLogging: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_LOGGING, true)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_LOGGING, value).apply()

    var isTestMode: Boolean
        get() = prefs.getBoolean(KEY_IS_TEST_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_TEST_MODE, value).apply()

    var isBridgeEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_BRIDGE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_BRIDGE_ENABLED, value).apply()

    var isEcoMode: Boolean
        get() = prefs.getBoolean(KEY_IS_ECO_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ECO_MODE, value).apply()

    var pingIntervalSeconds: Int
        get() = prefs.getInt(KEY_PING_INTERVAL, 60)
        set(value) = prefs.edit().putInt(KEY_PING_INTERVAL, value.coerceIn(30, 300)).apply()

    fun isFeatureEnabled(featureKey: String): Boolean {
        return prefs.getBoolean("feature_${featureKey}_alexa_enabled", true)
    }

    fun setFeatureEnabled(featureKey: String, enabled: Boolean) {
        prefs.edit().putBoolean("feature_${featureKey}_alexa_enabled", enabled).apply()
    }

    fun getAllFeatureStates(): Map<String, Boolean> {
        return ALL_FEATURES.associateWith { isFeatureEnabled(it) }
    }

    var isBedtimeShieldEnabled: Boolean
        get() = prefs.getBoolean("bedtime_shield_enabled", false)
        set(value) = prefs.edit().putBoolean("bedtime_shield_enabled", value).apply()

    fun getCustomPhrases(): List<String> {
        val raw = prefs.getString("custom_tts_phrases", null)
        return if (raw.isNullOrBlank()) {
            listOf("Dinner is ready!", "Time to wake up!", "Where are you?", "Chai peelo!", "Come downstairs!")
        } else {
            raw.split("|||").filter { it.isNotBlank() }
        }
    }

    fun saveCustomPhrases(phrases: List<String>) {
        prefs.edit().putString("custom_tts_phrases", phrases.joinToString("|||")).apply()
    }
}
