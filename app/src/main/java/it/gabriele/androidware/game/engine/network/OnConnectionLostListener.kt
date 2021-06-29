package it.gabriele.androidware.game.engine.network

import android.os.Bundle

fun interface OnConnectionLostListener {
    fun onConnectionLost(stateBeforeConnectionLost: Bundle?)
}