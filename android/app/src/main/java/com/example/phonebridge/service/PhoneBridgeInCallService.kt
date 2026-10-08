package com.example.phonebridge.service

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import com.example.phonebridge.data.CallState
import com.example.phonebridge.telecom.CallerResolver
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Official Android InCallService implementation.
 * When PhoneBridge is granted the Default Dialer / Phone role (RoleManager.ROLE_DIALER),
 * the Android Telecom framework binds this service for all incoming and outgoing calls,
 * allowing full official call control (answer, reject, disconnect).
 */
class PhoneBridgeInCallService : InCallService() {

    fun interface CallEventListener {
        fun onCallStateChanged(state: CallState, callerName: String?, phoneNumber: String?)
    }

    companion object {
        private const val TAG = "PhoneBridgeInCallService"
        var currentCall: Call? = null
            private set

        private val listeners = CopyOnWriteArrayList<CallEventListener>()

        fun addListener(listener: CallEventListener) {
            listeners.add(listener)
        }

        fun removeListener(listener: CallEventListener) {
            listeners.remove(listener)
        }

        fun answerCurrentCall(): Boolean {
            val call = currentCall ?: return false
            return try {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
                Log.i(TAG, "Successfully answered call via InCallService")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to answer call via InCallService: ${e.message}")
                false
            }
        }

        fun rejectCurrentCall(): Boolean {
            val call = currentCall ?: return false
            return try {
                if (call.state == Call.STATE_RINGING) {
                    call.reject(false, null)
                } else {
                    call.disconnect()
                }
                Log.i(TAG, "Successfully rejected/disconnected call via InCallService")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reject call via InCallService: ${e.message}")
                false
            }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)
            call?.let { updateState(it) }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.i(TAG, "onCallAdded: call state = ${call.state}")
        currentCall = call
        call.registerCallback(callCallback)
        updateState(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(TAG, "onCallRemoved")
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
        notifyListeners(CallState.IDLE, null, null)
    }

    private fun updateState(call: Call) {
        val rawNumber = call.details?.handle?.schemeSpecificPart ?: ""
        val callerName = CallerResolver.resolveCallerName(applicationContext, rawNumber)

        val appCallState = when (call.state) {
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.ACTIVE
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }

        notifyListeners(appCallState, callerName, rawNumber)
    }

    private fun notifyListeners(state: CallState, callerName: String?, phoneNumber: String?) {
        for (listener in listeners) {
            listener.onCallStateChanged(state, callerName, phoneNumber)
        }
    }
}
