package com.example.phonebridge.data

import com.google.gson.annotations.SerializedName

enum class CallState {
    IDLE,
    RINGING,
    ACTIVE,
    DISCONNECTED
}

data class CallEventPayload(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("event") val event: String, // INCOMING_CALL, CALL_ANSWERED, CALL_ENDED
    @SerializedName("callerName") val callerName: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("timestamp") val timestamp: String = System.currentTimeMillis().toString()
)

data class CommandMessage(
    @SerializedName("command") val command: String, // ANSWER_CALL, REJECT_CALL, FIND_PHONE, GET_BATTERY, SET_FLASHLIGHT, MAKE_CALL, SEND_WHATSAPP, PING, TEST_CALL
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("callerName") val callerName: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("action") val action: String? = null, // START, STOP
    @SerializedName("state") val state: Boolean? = null, // for flashlight
    @SerializedName("target") val target: String? = null, // contact, phone number, or app name
    @SerializedName("message") val message: String? = null, // for whatsapp / sms / tts
    @SerializedName("mode") val mode: String? = null, // SILENT, VIBRATE, NORMAL, MAX
    @SerializedName("level") val level: Int? = null // for volume level
)

data class CommandResponse(
    @SerializedName("requestId") val requestId: String?,
    @SerializedName("command") val command: String,
    @SerializedName("success") val success: Boolean,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("data") val data: Any? = null,
    @SerializedName("type") val type: String = "COMMAND_RESULT",
    @SerializedName("timestamp") val timestamp: String = System.currentTimeMillis().toString()
)

data class DeviceRegisterRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("clientVersion") val clientVersion: String = "1.0.0"
)

data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Any? = null
)

data class CallStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("hasActiveCall") val hasActiveCall: Boolean,
    @SerializedName("state") val state: String,
    @SerializedName("callerName") val callerName: String?,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    @SerializedName("timestamp") val timestamp: String?
)

data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val tag: String, // ANDROID, BACKEND, ALEXA, TELECOM
    val message: String,
    val isError: Boolean = false
)
