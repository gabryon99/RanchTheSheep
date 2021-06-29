package it.gabriele.androidware.activities.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

class WifiP2StatusReceiver(
    private val wifiP2pManager: WifiP2pManager,
    private val wifiDirectChannel: WifiP2pManager.Channel,
    private val connectionStateChanged: OnConnectionStateChangeListener? = null,
    private val statusStateChanged: OnWifiP2pStateChangeListener? = null
): BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiP2StatusReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent) {

        Log.d(TAG, "onReceive: ${intent.action}")
        
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // We are connected to a peer! Let's request the connection info to invoke the callback.
                wifiP2pManager.requestConnectionInfo(wifiDirectChannel) { info ->
                    connectionStateChanged?.onConnectionStateChanged(info)
                }
            }
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0)
                statusStateChanged?.onOnWifiP2pStateChange(wifiState)
            }
        }

    }
}