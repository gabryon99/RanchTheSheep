package it.gabriele.androidware.game.view

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.WorkerThread
import it.gabriele.androidware.game.engine.states.GameStateManager
import kotlin.math.log

abstract class StatefulNetworkGameView(
    context: Context,
    oldState: Bundle?,
): NetworkGameView(context, oldState) {

    companion object {
        private const val TAG = "[Game]::StatefulNetworkGameView"

        const val BUNDLE_GAME_STATE = "StatefulNetworkGameView.BUNDLE_GAME_STATE"
        const val STATE_CONNECTION_LOST = "StatefulNetworkGameView.CONNECTION_LOST"

        const val STATE_OLD_WIDTH = "StatefulNetworkGameView.STATE_OLD_WIDTH"
        const val STATE_OLD_HEIGHT = "StatefulNetworkGameView.STATE_OLD_HEIGHT"

        /* Default States */
        const val INITIAL_GAME_STATE = 0
        const val DISCONNECTED_STATE = 9999
    }

    protected var lostConnectionState: Bundle? = null
    private var restartGameRequest = false

    abstract val gameStateManager: GameStateManager

    abstract fun initGameStateManager()

    /***
     * Save the current game state inside a bundle.
     */
    fun saveCurrentState(): Bundle {

        /* Build a new empty bundle that already contains the current key state. */
        val stateBundle = Bundle().apply {
            putInt(GameStateManager.KEY_CURRENT_GAME_STATE, gameStateManager.currentKeyState)
            putInt(STATE_OLD_WIDTH, width)
            putInt(STATE_OLD_HEIGHT, height)
            putBoolean(STATE_CONNECTION_LOST, true)
        }

        /* Ask to the current game state to save its internal state. */
        gameStateManager.saveGameState(stateBundle)
        Log.d(TAG, "saveCurrentState: saving bundle $stateBundle")
        
        return stateBundle
    }

    override fun pause() {
        super.pause()
        oldState = saveCurrentState()
    }

    @WorkerThread
    override fun handleConnectionLost() {
        lostConnectionState = saveCurrentState()
        handler.post {
            connectionLostListener?.onConnectionLost(lostConnectionState)
        }
    }

    override fun restartGame() {
        restartGameRequest = true
    }

    @WorkerThread
    final override fun startGameLoop() {
        if (!restartGameRequest) {

            Log.d(TAG, "startGameLoop: $oldState")

            var previousState = INITIAL_GAME_STATE
            val bundleGameState = oldState?.getBundle(BUNDLE_GAME_STATE)
            if (bundleGameState != null) {
                previousState = bundleGameState.getInt(GameStateManager.KEY_CURRENT_GAME_STATE)
                Log.d(TAG, "startGameLoop: beggining state $previousState")
            }

            if (previousState == INITIAL_GAME_STATE) {
                gameStateManager.start(oldState)
            }
            else {
                gameStateManager.switch(previousState, oldState = bundleGameState)
            }
        }
        else {
            gameStateManager.restart(INITIAL_GAME_STATE)
        }
    }

    final override fun readInputEvent(event: MotionEvent) {
        gameStateManager.currentState?.readInputEvent(event)
    }

    final override fun render(canvas: Canvas) {
        gameStateManager.currentState?.render(canvas)
    }

    final override fun update(deltaTime: Float) {
        gameStateManager.currentState?.update(deltaTime)
    }

}