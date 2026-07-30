package com.vless.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.vless.vpn.vpn.VpnService as CustomVpnService

@RequiresApi(Build.VERSION_CODES.N)
class VpnTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_ACTIVE) {
            val intent = Intent(this, CustomVpnService::class.java).apply {
                action = CustomVpnService.ACTION_DISCONNECT
            }
            startService(intent)
            tile.state = Tile.STATE_INACTIVE
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent == null) {
                val intent = Intent(this, CustomVpnService::class.java).apply {
                    action = CustomVpnService.ACTION_CONNECT
                }
                startService(intent)
                tile.state = Tile.STATE_ACTIVE
            } else {
                vpnIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAndCollapse(vpnIntent)
            }
        }
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.label = "VLESS VPN"
        tile.updateTile()
    }
}
