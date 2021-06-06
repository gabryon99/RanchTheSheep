package it.gabriele.androidware.game.engine.states

import android.graphics.Canvas
import android.view.MotionEvent
import androidx.annotation.WorkerThread
import it.gabriele.androidware.game.engine.network.NetworkMessage

interface GameStateMachine {

    /***
     * Read incoming input events.
     */
    @WorkerThread fun readInputEvent(event: MotionEvent) = Unit

    /***
     * Update the game state.
     */
    @WorkerThread fun update(deltaTime: Float) = Unit

    /***
     * Render the game scene inside the canvas.
     */
    @WorkerThread fun render(canvas: Canvas) = Unit

    /***
     * Read incoming network message.
     */
    @WorkerThread fun readNetworkMessage(message: NetworkMessage) = Unit

    /***
     * Return a message to send through the network.
     */
    @WorkerThread fun sendNetworkMessage(): NetworkMessage? = null

}