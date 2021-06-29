package it.gabriele.androidware.fragments

import android.net.wifi.p2p.WifiP2pDevice

fun interface OnGameClickListener {
    fun onGameClick(device: WifiP2pDevice)
}