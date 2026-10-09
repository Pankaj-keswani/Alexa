package com.example.phonebridge.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network availability in real-time to prevent battery drain
 * caused by looping socket connection attempts when offline.
 */
class NetworkConnectivityMonitor(
    context: Context,
    private val onNetworkRestored: (() -> Unit)? = null,
    private val onNetworkLostCallback: (() -> Unit)? = null
) {
    companion object {
        private const val TAG = "NetworkMonitor"
    }

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow(determineNetworkType())
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network became available")
            val wasOffline = !_isOnline.value
            _isOnline.value = true
            _networkType.value = determineNetworkType()

            if (wasOffline) {
                onNetworkRestored?.invoke()
            }
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network connection lost")
            val stillOnline = checkInitialConnectivity()
            _isOnline.value = stillOnline
            _networkType.value = if (stillOnline) determineNetworkType() else "Offline"

            if (!stillOnline) {
                onNetworkLostCallback?.invoke()
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            _networkType.value = if (hasInternet) determineNetworkType() else "Offline"
        }
    }

    private var isRegistered = false

    fun start() {
        if (isRegistered || connectivityManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
            isRegistered = true
            _isOnline.value = checkInitialConnectivity()
            _networkType.value = determineNetworkType()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun stop() {
        if (!isRegistered || connectivityManager == null) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }

    fun isNetworkAvailable(): Boolean {
        return checkInitialConnectivity()
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun determineNetworkType(): String {
        val cm = connectivityManager ?: return "Offline"
        val activeNetwork = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Offline"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }
}
