package it.gabriele.androidware.game.engine.network

import android.util.Log
import java.nio.ByteBuffer

data class NetworkMessage(val buffer: ByteBuffer?) {

    companion object {

        const val TAG = "NetworkMessage"

        fun fromByteBuffer(srcBuffer: ByteBuffer, incomingBytes: Int): NetworkMessage {

            val copyBuff = ByteBuffer.allocate(incomingBytes)
            repeat(incomingBytes) {
                copyBuff.put(srcBuffer.get())
            }

            copyBuff.flip()

            return NetworkMessage(copyBuff)
        }

        fun endMessage(): NetworkMessage {
            return NetworkMessage(null)
        }

    }

    val isEndMessage = (buffer == null)


}