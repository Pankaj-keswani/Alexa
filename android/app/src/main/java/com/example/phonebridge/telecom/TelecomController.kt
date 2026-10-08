package com.example.phonebridge.telecom

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.phonebridge.data.CommandResponse
import com.example.phonebridge.service.PhoneBridgeInCallService

class TelecomController(private val context: Context) {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    companion object {
        private const val TAG = "TelecomController"
    }

    /**
     * Attempts to answer the active incoming call.
     * Uses official Android InCallService when default dialer role is active,
     * or TelecomManager.acceptRingingCall() when ANSWER_PHONE_CALLS permission is granted.
     */
    fun answerCall(requestId: String?, isTestMode: Boolean): CommandResponse {
        Log.i(TAG, "Attempting to answer call. isTestMode=$isTestMode")

        if (isTestMode) {
            return CommandResponse(
                requestId = requestId,
                command = "ANSWER_CALL",
                success = true,
                reason = "Simulated call answered in Test Mode"
            )
        }

        // 1. Preferred Official Path: InCallService (App is Default Phone / Dialer)
        if (PhoneBridgeInCallService.currentCall != null) {
            val answered = PhoneBridgeInCallService.answerCurrentCall()
            return if (answered) {
                CommandResponse(
                    requestId = requestId,
                    command = "ANSWER_CALL",
                    success = true,
                    reason = "Answered via Android InCallService"
                )
            } else {
                CommandResponse(
                    requestId = requestId,
                    command = "ANSWER_CALL",
                    success = false,
                    reason = "Failed to answer via InCallService"
                )
            }
        }

        // 2. Alternative Path: TelecomManager.acceptRingingCall() (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasAnswerPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasAnswerPermission) {
                return CommandResponse(
                    requestId = requestId,
                    command = "ANSWER_CALL",
                    success = false,
                    reason = "Missing ANSWER_PHONE_CALLS permission"
                )
            }

            try {
                @Suppress("DEPRECATION")
                telecomManager?.acceptRingingCall()
                return CommandResponse(
                    requestId = requestId,
                    command = "ANSWER_CALL",
                    success = true,
                    reason = "Answered via TelecomManager.acceptRingingCall"
                )
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException answering call: ${se.message}")
                return CommandResponse(
                    requestId = requestId,
                    command = "ANSWER_CALL",
                    success = false,
                    reason = "Android restriction: Answering requires PhoneBridge to be Default Phone dialer or interactive foreground activity."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception answering call: ${e.message}")
                return CommandResponse(
                    requestId = requestId,
                    command = "ANSWER_CALL",
                    success = false,
                    reason = "TelecomManager error: ${e.message}"
                )
            }
        }

        return CommandResponse(
            requestId = requestId,
            command = "ANSWER_CALL",
            success = false,
            reason = "Unsupported Android SDK version for call answering"
        )
    }

    /**
     * Attempts to reject the active incoming call.
     * Uses official Android InCallService when default dialer role is active.
     */
    fun rejectCall(requestId: String?, isTestMode: Boolean): CommandResponse {
        Log.i(TAG, "Attempting to reject call. isTestMode=$isTestMode")

        if (isTestMode) {
            return CommandResponse(
                requestId = requestId,
                command = "REJECT_CALL",
                success = true,
                reason = "Simulated call rejected in Test Mode"
            )
        }

        // 1. Preferred Official Path: InCallService
        if (PhoneBridgeInCallService.currentCall != null) {
            val rejected = PhoneBridgeInCallService.rejectCurrentCall()
            return if (rejected) {
                CommandResponse(
                    requestId = requestId,
                    command = "REJECT_CALL",
                    success = true,
                    reason = "Rejected via Android InCallService"
                )
            } else {
                CommandResponse(
                    requestId = requestId,
                    command = "REJECT_CALL",
                    success = false,
                    reason = "Failed to reject via InCallService"
                )
            }
        }

        // 2. Alternative Path: TelecomManager.endCall() (Android 9.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                @Suppress("DEPRECATION")
                val ended = telecomManager?.endCall() ?: false
                if (ended) {
                    return CommandResponse(
                        requestId = requestId,
                        command = "REJECT_CALL",
                        success = true,
                        reason = "Ended via TelecomManager.endCall"
                    )
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "SecurityException on endCall: ${se.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Exception on endCall: ${e.message}")
            }
        }

        // Clarify official Android restriction to user/Alexa
        return CommandResponse(
            requestId = requestId,
            command = "REJECT_CALL",
            success = false,
            reason = "Android restriction: Rejecting calls requires PhoneBridge to be set as the Default Phone Dialer."
        )
    }

    fun setSpeakerphoneOn(enable: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            audioManager?.let { am ->
                am.mode = android.media.AudioManager.MODE_IN_CALL
                am.isSpeakerphoneOn = enable
                Log.i(TAG, "Speakerphone set to $enable")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set speakerphone: ${e.message}")
        }
    }
}
