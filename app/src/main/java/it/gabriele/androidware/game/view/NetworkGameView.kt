package it.gabriele.androidware.game.view

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.annotation.MainThread
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.states.GameStateMachine
import it.gabriele.androidware.game.engine.network.NetworkMessage
import it.gabriele.androidware.game.engine.network.NetworkThread
import java.lang.Float.min
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

abstract class NetworkGameView(
    context: Context,
    attributeSet: AttributeSet? = null
): SurfaceView(context, attributeSet), Runnable, GameStateMachine {

    companion object {

        private const val TAG = "NetworkGameView"

        /* Upper bound for the Delta Time */
        private const val DT = (1f / GameContext.MAX_FPS)
    }

    @Transient private var mGameRunning = false

    private var mRenderThread: Thread? = null
    private var mNetworkThread: NetworkThread? = null

    private val _copyInputEvents: Queue<MotionEvent> = LinkedList()
    private val _copyNetworkEvents: Queue<NetworkMessage> = LinkedList()

    private val mInputEvents: LinkedBlockingQueue<MotionEvent> = LinkedBlockingQueue()
    private val mIncomingNetworkMessages: LinkedBlockingQueue<NetworkMessage> = LinkedBlockingQueue()
    private val mOutgoingNetworkMessages: LinkedBlockingQueue<NetworkMessage> = LinkedBlockingQueue()

    @MainThread fun resume() {

        mGameRunning = true

        mRenderThread = Thread(this).apply {
            name = "[render-thread]"
            start()
        }

        /*
        mNetworkThread = NetworkThread(mIncomingNetworkMessages, mOutgoingNetworkMessages).apply {
            name = "[network-thread]"
            start()
        }
        */
    }

    @MainThread fun pause() {

        Log.d(TAG, "pause: pausing the game view...")
        
        mGameRunning = false
        mNetworkThread?.stopCommunication()

        mRenderThread?.join()
        mNetworkThread?.join()
    }

    /***
     * Defer touch events to the Input Event Queue.
     */
    @MainThread final override fun onTouchEvent(event: MotionEvent): Boolean {
        mInputEvents.add(event)
        return true
    }

    final override fun run() {

        Log.d(TAG, "Render Thread: running...")
        
        var currentTime = SystemClock.elapsedRealtime()

        /* Game Loop: https://gameprogrammingpatterns.com/game-loop.html#play-catch-up */
        while (mGameRunning) {

            if (!holder.surface.isValid) continue

            val newTime = SystemClock.elapsedRealtime()
            var frameTime = (newTime.toFloat() - currentTime).div(1000)

            currentTime = newTime

            /* Handle received input events */
            processInputEventQueue()
            /* Handle incoming network messages */
            processIncomingNetworkMessagesQueue()

            while (frameTime > 0) {

                val deltaTime = min(DT, frameTime)

                /* Update the game state */
                update(deltaTime)

                /* Check if there are message to send */
                sendNetworkMessage()?.let {
                    /* Enqueue the message to send to the Network Thread */
                    mOutgoingNetworkMessages.add(it)
                }

                frameTime -= deltaTime
            }

            /* Acquire canvas to draw */
            val canvas = holder.lockHardwareCanvas()

            render(canvas)

            /* Release canvas and post edits */
            holder.unlockCanvasAndPost(canvas)

        }
    }

    /***
     * Extract input events into a queue and dispatch the events.
     */
    private fun processInputEventQueue() {

        mInputEvents.drainTo(_copyInputEvents)

        while (_copyInputEvents.isNotEmpty()) {
            readInputEvent(_copyInputEvents.remove())
        }

    }

    /***
     * Extract network events into a queue and dispatch the events.
     */
    private fun processIncomingNetworkMessagesQueue() {

        mIncomingNetworkMessages.drainTo(_copyNetworkEvents)

        while (_copyNetworkEvents.isNotEmpty()) {
            readNetworkMessage(_copyNetworkEvents.remove())
        }

    }

}