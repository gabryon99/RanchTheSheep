package it.gabriele.androidware.activities.receivers

import android.net.wifi.p2p.WifiP2pInfo

fun interface OnConnectionStateChangeListener {
    fun onConnectionStateChanged(info: WifiP2pInfo)
}