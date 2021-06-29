package it.gabriele.androidware.game.engine.network

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import it.gabriele.androidware.game.view.StatefulNetworkGameView
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

abstract class NetworkManager(
    private val gameView: StatefulNetworkGameView,
    private val networkInfo: WifiP2pInfo,
) {

    companion object {
        private const val TAG = "[Game]::NetworkManager"
    }

    var connected = false
        protected set

    val isGameMaster = networkInfo.isGroupOwner

    private val outgoingChannel = LinkedBlockingDeque<NetworkMessage>()
    private val incomingChannel = LinkedBlockingDeque<NetworkMessage>()

    private var incomingMessagesThread: Thread? = null
    private var outgoingMessagesThread: Thread? = null

    protected var outsideSocketChannel: SocketChannel? = null

    abstract fun connect()

    open fun disconnect() {

        Log.d(TAG, "disconnect: disconnecting...")

        // Terminate threads...
        outgoingChannel.offerFirst(NetworkMessage.endMessage())

    }

    fun beginCommunication() {
        incomingMessagesThread = thread(start = true, name = "[thread-outgoing]") {
            handleOutgoingMessages()
        }
        outgoingMessagesThread = thread(start = true, name = "[thread-incoming]") {
            handleIncomingMessages()
        }
    }

    fun receive(): NetworkMessage? {
        return try {
            incomingChannel.poll()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun receiveBlocking(milliseconds: Long): NetworkMessage? {
        return try {
            incomingChannel.poll(milliseconds, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun send(message: NetworkMessage) {
        try {
            outgoingChannel.offer(message)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleOutgoingMessages() {

        val socketChannel = outsideSocketChannel!!
        Log.d(TAG, "handleOutgoingMessages: preparing to handle outgoing messages")

        while (!Thread.interrupted()) {

            /* Get message to send from the channel */
            val messageToSend = outgoingChannel.take()
            if (messageToSend.isEndMessage) {
                Log.d(TAG, "handleOutgoingMessages: end message received, terminating thread...")
                break
            }

            val messageBuffer = messageToSend.buffer!!

            // Prepare the buffer in read mode
            messageBuffer.flip()

            val buffToWrite = ByteBuffer.allocate(Int.SIZE_BYTES + messageBuffer.limit())
            /* Put the message size inside the buffer */
            buffToWrite.putInt(messageBuffer.limit())
            /* Put the message inside the buffer */
            buffToWrite.put(messageToSend.buffer)
            /* Prepare the buffer to be sent */
            buffToWrite.flip()

            try {
                while (buffToWrite.hasRemaining()) {
                    // Log.d(TAG, "handleOutgoingMessages: sent $sent bytes to ${socketChannel.remoteAddress}")
                    socketChannel.write(buffToWrite)
                }
            }
            catch (e: IOException) {
                e.printStackTrace()
                break
            }
        }

        outgoingChannel.clear()
        Log.d(TAG, "handleOutgoingMessages: thread terminated...")
        
    }

    private fun handleIncomingMessages() {

        val socketChannel = outsideSocketChannel!!
        val readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)

        Log.d(TAG, "handleIncomingMessages: preparing to handle incoming messages")

        while (!Thread.interrupted()) {

            try {

                val bytesRead = socketChannel.read(readBuffer)
                if (bytesRead == -1) {
                    Log.e(TAG, "handleIncomingMessages: EOF of read")
                    gameView.signalConnectionLost()
                    break
                }

                readBuffer.flip()

                while (readBuffer.hasRemaining()) {
                    val messageSize = readBuffer.int
                    val incomingMsg = NetworkMessage.fromByteBuffer(readBuffer, messageSize)
                    incomingChannel.put(incomingMsg)
                }

                readBuffer.clear()
            }
            catch (e: Exception) {
                Log.e(TAG, "handleIncomingMessages: error $e", )
                break
            }
            
        }

        incomingChannel.clear()
        Log.d(TAG, "handleIncomingMessages: thread terminated...")
    }

}