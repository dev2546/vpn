package com.vless.vpn.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vless.vpn.VpnApp
import com.vless.vpn.ui.MainActivity

/**
 * Android VpnService implementation creating TUN interface and routing device traffic.
 */
class VpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayManager: XrayManager? = null
    private var isRunning = false

    companion object {
        const val ACTION_CONNECT = "com.vless.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.vless.vpn.ACTION_DISCONNECT"
        const val ACTION_STATUS_CHANGE = "com.vless.vpn.ACTION_STATUS_CHANGE"
        const val EXTRA_STATUS = "extra_status"
        
        const val STATUS_DISCONNECTED = "Disconnected"
        const val STATUS_CONNECTING = "Connecting"
        const val STATUS_CONNECTED = "Connected"
        
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "VpnService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_CONNECT -> startVpnTunnel()
            ACTION_DISCONNECT -> stopVpnTunnel()
        }
        return START_STICKY
    }

    private fun startVpnTunnel() {
        if (isRunning) return
        
        broadcastStatus(STATUS_CONNECTING)
        Log.i(TAG, "Initializing VPN Service & Xray Core...")

        try {
            // Start Foreground Notification for Android 8.0+
            startForeground(NOTIFICATION_ID, buildNotification("Connecting to VLESS Server..."))

            // 1. Start Xray-Core
            xrayManager = XrayManager(applicationContext)
            val xrayStarted = xrayManager?.startXray() ?: false
            
            if (!xrayStarted) {
                Log.e(TAG, "Xray-Core failed to start")
                broadcastStatus(STATUS_DISCONNECTED)
                stopForegroundNotification()
                return
            }

            // 2. Establish TUN Interface
            val builder = Builder()
                .setSession("VLESS VPN")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            // Bypass VLESS app from VPN routing loop
            builder.addDisallowedApplication(packageName)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish TUN interface")
                xrayManager?.stopXray()
                broadcastStatus(STATUS_DISCONNECTED)
                stopForegroundNotification()
                return
            }

            isRunning = true
            broadcastStatus(STATUS_CONNECTED)
            startForeground(NOTIFICATION_ID, buildNotification("Connected to sakura.proxy.rlwy.net:10322"))
            Log.i(TAG, "VPN Tunnel successfully established!")

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error establishing VPN service", e)
            stopVpnTunnel()
        }
    }

    private fun stopVpnTunnel() {
        try {
            xrayManager?.stopXray()
            vpnInterface?.close()
            vpnInterface = null
            isRunning = false
            broadcastStatus(STATUS_DISCONNECTED)
            stopForegroundNotification()
            Log.i(TAG, "VPN Service stopped")
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN service", e)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, pendingIntentFlags)

        val disconnectIntent = Intent(this, VpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(this, 1, disconnectIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, VpnApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("VLESS VPN")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS_CHANGE).apply {
            putExtra(EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }
}
