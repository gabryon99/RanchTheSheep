package it.gabriele.androidware.game.engine.network

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.view.StatefulNetworkGameView
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.net.SocketOption
import java.net.SocketTimeoutException
import java.net.StandardSocketOptions
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class ServerNetworkManager(
    gameView: StatefulNetworkGameView,
    networkInfo: WifiP2pInfo,
): NetworkManager(gameView, networkInfo) {

    companion object {
        private const val TAG = "[Game]::ServerNetworkManager"
    }

    private var serverSocketChannel: ServerSocketChannel? = null

    override fun connect() {

        try {

            Log.d(TAG, "connect: *:${GameContext.NET_PORT}")

            val srvSocketChannel = ServerSocketChannel.open()
            srvSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
            srvSocketChannel.socket().bind(InetSocketAddress(GameContext.NET_PORT))
            srvSocketChannel.socket().soTimeout = 5000

            serverSocketChannel = srvSocketChannel

            val clientSocketChannel = srvSocketChannel.socket().accept().channel
            outsideSocketChannel = clientSocketChannel
            connected = true
        }
        catch (e: SocketTimeoutException) {
            connected = false
            serverSocketChannel?.close()
            Log.e(TAG, "connect: timeout exception", )
        }
        catch (e: IOException) {
            connected = false
            Log.e(TAG, "connect: error while connecting to the other peer (${e} - $connected)", )
        }
        finally {
            if (connected) {
                Log.d(TAG, "connect: connection success!")
            }
        }
        
    }

    override fun disconnect() {

        Log.d(TAG, "disconnect: disconnecting...")

        try {
            serverSocketChannel?.close()
        }
        catch (e: IOException) {
            Log.e(TAG, "disconnect: error while closing server channel: ${e.message}", )
        }

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