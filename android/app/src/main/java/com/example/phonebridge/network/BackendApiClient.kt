package com.example.phonebridge.network

import android.util.Log
import com.example.phonebridge.data.ApiResponse
import com.example.phonebridge.data.CallEventPayload
import com.example.phonebridge.data.CallStatusResponse
import com.example.phonebridge.data.DeviceRegisterRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class BackendApiClient(
    private val baseUrlProvider: () -> String,
    private val tokenProvider: () -> String,
    private val deviceIdProvider: () -> String
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "BackendApiClient"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private fun buildRequest(endpoint: String, method: String = "GET", body: String? = null): Request {
        val cleanBase = baseUrlProvider().removeSuffix("/")
        val url = "$cleanBase$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("x-device-token", tokenProvider())
            .addHeader("x-device-id", deviceIdProvider())
            .addHeader("Content-Type", "application/json")

        if (method == "POST") {
            val reqBody = (body ?: "{}").toRequestBody(JSON)
            builder.post(reqBody)
        } else {
            builder.get()
        }
        return builder.build()
    }

    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/api/health")
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkHealth failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun registerDevice(deviceName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = DeviceRegisterRequest(
                deviceId = deviceIdProvider(),
                deviceName = deviceName
            )
            val json = gson.toJson(payload)
            val request = buildRequest("/api/device/register", method = "POST", body = json)
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerDevice failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendCallEvent(payload: CallEventPayload): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(payload)
            val request = buildRequest("/api/events/call", method = "POST", body = json)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendCallEvent failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCallStatus(): Result<CallStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/api/call/status")
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val status = gson.fromJson(body, CallStatusResponse::class.java)
                    Result.success(status)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCallStatus failed: ${e.message}")
            Result.failure(e)
        }
    }
}
