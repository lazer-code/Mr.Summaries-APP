package com.example.mrsummaries_app.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {
    /**
     * Permissive online check using NetworkCapabilities / transports.
     */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false

        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return true
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true

        return false
    }

    /**
     * Active probe that attempts a TCP connection to a stable public endpoint.
     * Uses IP address (default 8.8.8.8:53) so it bypasses DNS resolution.
     * Call this from a background thread (suspend).
     *
     * Returns true if the TCP connect succeeded within timeoutMs.
     */
    suspend fun probeInternet(host: String = "8.8.8.8", port: Int = 53, timeoutMs: Int = 1500): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (t: Throwable) {
                false
            }
        }
    }

    fun openNetworkSettingsIntent(): Intent {
        return Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}