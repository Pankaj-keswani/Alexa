package com.example.phonebridge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.phonebridge.data.CallState
import com.example.phonebridge.telecom.CallerResolver

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (stateStr == lastState) return
        lastState = stateStr

        val rawNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val callerName = CallerResolver.resolveCallerName(context, rawNumber)

        Log.i(TAG, "Telephony state changed: $stateStr, number: $rawNumber, caller: $callerName")

        val serviceIntent = Intent(context, PhoneBridgeForegroundService::class.java).apply {
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    action = PhoneBridgeForegroundService.ACTION_INCOMING_CALL
                    putExtra(PhoneBridgeForegroundService.EXTRA_CALLER_NAME, callerName)
                    putExtra(PhoneBridgeForegroundService.EXTRA_PHONE_NUMBER, rawNumber ?: "Unknown")
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    action = PhoneBridgeForegroundService.ACTION_CALL_ANSWERED
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    action = PhoneBridgeForegroundService.ACTION_CALL_ENDED
                }
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver telephony event to ForegroundService: ${e.message}")
        }
    }
}
