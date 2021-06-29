package it.gabriele.androidware.game.engine.network

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.view.StatefulNetworkGameView
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.RuntimeException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class ClientNetworkManager(
    gameView: StatefulNetworkGameView,
    private val networkInfo: WifiP2pInfo,
): NetworkManager(gameView, networkInfo) {

    companion object {
        private const val TAG = "[Game]::ClientNetworkManager"
    }

    override fun connect() {

        try {

            val serverAddress = networkInfo.groupOwnerAddress

            Log.d(TAG, "connect: trying to connect to $serverAddress:${GameContext.NET_PORT}")
            
            val socketChannel = SocketChannel.open()
            socketChannel.connect(InetSocketAddress(serverAddress, GameContext.NET_PORT))

            outsideSocketChannel = socketChannel
            connected = true

        }
        catch (e: IOException) {
            Log.e(TAG, "connect: error while connecting to the other peer ${e.message}", )
        }
        finally {
            if (connected) {
                Log.d(TAG, "connect: connection success")
            }
        }

    }

    override fun disconnect() {

        Log.d(TAG, "disconnect: disconnecting...")

        if (connected) {
            try {
                outsideSocketChannel?.close()
            }
            catch (e: IOException) {
                Log.e(TAG, "disconnect: error while closing client channel: ${e.message}", )
            }
            connected = false
        }

        super.disconnect()
    }

}