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
}
