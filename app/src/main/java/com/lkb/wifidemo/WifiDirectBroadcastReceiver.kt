package com.lkb.wifidemo

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

class WifiDirectBroadcastReceiver(
    private val mManager: WifiP2pManager?,
    private val mChannel: WifiP2pManager.Channel?,
    private val mActivity: MainActivity
) :
    BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wifi is ON", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Wifi is OFF", Toast.LENGTH_SHORT).show()
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            if (ActivityCompat.checkSelfPermission(
                    mActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mManager?.requestPeers(mChannel, mActivity.peerListListener)
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            //do something
            if (mManager == null) {
                return
            }
            val networkInfo =
                intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
            if (networkInfo!!.isConnected) {
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener)
            } else {
                mActivity.connectionStatus!!.text = "Device Disconnected"
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
        }
    }
}

