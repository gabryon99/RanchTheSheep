package it.gabriele.androidware.activities.receivers

fun interface OnWifiP2pStateChangeListener {
    fun onOnWifiP2pStateChange(status: Int)
}