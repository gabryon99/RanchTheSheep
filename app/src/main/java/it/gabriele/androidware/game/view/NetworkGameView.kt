package it.gabriele.androidware.game.view

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.engine.network.NetworkManager
import it.gabriele.androidware.game.engine.network.OnConnectionLostListener
import it.gabriele.androidware.game.engine.states.GameStateMachine
import java.lang.Float.min
import java.lang.Runnable
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.floor

abstract class NetworkGameView(
    context: Context,
    protected var oldState: Bundle? = null,
    val audioManager: AudioManager = AudioManager(context),
): SurfaceView(context, null), Runnable, GameStateMachine {

    companion object {

        private const val TAG = "[Game]::NetworkGameView"
        private const val MS_TO_SECS = 1000

        /* Upper bound for the Delta Time */
        private const val DT = (1f / GameContext.MAX_FPS)
    }

    protected var connectionLostListener: OnConnectionLostListener? = null

    /* Boolean flag to indicate when the connection fails */
    @Volatile private var connectionLost = false

    @Volatile private var connectionRecovery = false

    /* Boolean flag used to indicate if the game view is running or not */
    @Volatile private var gameRunning = false

    var networkManager: NetworkManager? = null

    /* Thread used to render the main game loop. */
    private var renderThread: Thread? = null

    /* Queue used to handle input events coming from the Main Thread */
    private val inputEvents: LinkedBlockingQueue<MotionEvent> = LinkedBlockingQueue()

    /***
     * Restart the game.
     */
    abstract fun restartGame()

    /***
     * Resume the game.
     */
    @MainThread open fun resume() {

        gameRunning = true

        renderThread = thread(start = true, name = "[render-thread]") {
            run()
        }

        /* Resume all paused music players */
        audioManager.resumeMusicPlayers()
    }

    /***
     * Pause the game.
     */
    @MainThread open fun pause() {

        Log.d(TAG, "pause: pausing the game view...")

        gameRunning = false

        renderThread?.join()
        renderThread = null

        Log.d(TAG, "pause: render thread joined...")
        
        /* Pause all music players used for the game */
        audioManager.pauseMusicPlayers()

        Log.d(TAG, "pause: pause process completed!")
    }

    /***
     * Defer touch events to the Input Event Queue.
     */
    @MainThread final override fun onTouchEvent(event: MotionEvent): Boolean {
        inputEvents.put(event)
        return true
    }

    @MainThread
    final override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Let's update the Scaling Factor
        GameContext.SCALE_X = floor(w.toFloat() / GameContext.LOGICAL_PORTRAIT_WIDTH).toInt()
        GameContext.SCALE_Y = floor(h.toFloat() / GameContext.LOGICAL_PORTRAIT_HEIGHT).toInt()

        Log.d(TAG, "onSizeChanged: updated scaling factor (${GameContext.SCALE_X}, ${GameContext.SCALE_Y})")
    }

    @WorkerThread final override fun run() {

        Log.d(TAG, "running (${Thread.currentThread().name})...")

        startGameLoop()

        var currentTime = SystemClock.elapsedRealtime()

        /* Game Loop: https://gameprogrammingpatterns.com/game-loop.html#play-catch-up */
        while (gameRunning) {

            val newTime = SystemClock.elapsedRealtime()
            var frameTime = (newTime.toFloat() - currentTime).div(MS_TO_SECS)

            currentTime = newTime

            /* Handle received input events */
            processInputEventQueue()

            while (frameTime > 0) {

                /* Did we lose the connection with the other peer? */
                if (connectionLost) {
                    handleConnectionLost()
                    connectionLost = false
                }

                if (connectionRecovery) {
                    handleConnectionRecovery()
                    connectionRecovery = false
                }

                val deltaTime = min(DT, frameTime)

                /* Update the game state */
                update(deltaTime)

                frameTime -= deltaTime
            }

            /* Acquire canvas to draw */
            val canvas = holder.lockHardwareCanvas()
            if (canvas != null) {
                /* Render the canvas */
                render(canvas)
                /* Release canvas and post edits */
                holder.unlockCanvasAndPost(canvas)
            }
        }

        endGameLoop()
        Log.d(TAG, "terminating (${Thread.currentThread().name})...")

    }

    /***
     * Signal that the connection has been interrupted.
     */
    fun signalConnectionLost() {
        connectionLost = true
    }

    fun signalConnectionRecovery() {
        connectionRecovery = true
    }

    /***
     * Called when the render thread begins its task. The function
     * is called by the render thread!
     */
    @WorkerThread protected open fun startGameLoop() = Unit

    /***
     * Called when the render thread ends its task. The function
     * is called by the render thread!
     */
    @WorkerThread protected open fun endGameLoop() = Unit

    @WorkerThread protected open fun handleConnectionLost() = Unit

    @WorkerThread protected open fun handleConnectionRecovery() = Unit

    /***
     * Extract input events into a queue and dispatch the events.
     */
    @WorkerThread private fun processInputEventQueue() {
        while (inputEvents.isNotEmpty()) {
            readInputEvent(inputEvents.remove())
        }
    }

    /***
     * Register the network manager to be used by the game.
     */
    fun registerNetworkManager(networkManager: NetworkManager, listener: OnConnectionLostListener) {
        this.networkManager = networkManager
        this.connectionLostListener = listener
    }

}