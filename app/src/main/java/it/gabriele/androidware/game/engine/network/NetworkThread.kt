package it.gabriele.androidware.game.engine.network

import it.gabriele.androidware.game.engine.GameContext
import java.nio.ByteBuffer
import java.util.*

class NetworkThread(
    incomingMessages: Queue<NetworkMessage>,
    outgoingMessages: Queue<NetworkMessage>
): Thread() {

    companion object {
        private const val BUFFER_SIZE = 4096
    }

    @Transient private var mRunning = false

    private val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

    fun stopCommunication() {
        mRunning = false
    }

    override fun run() {

        mRunning = true

        if (GameContext.IS_GAME_MASTER) {
            serverLoop()
        }
        else {
            clientLoop()
        }
    }

    private fun clientLoop() {



    }

    private fun serverLoop() {

    }


}