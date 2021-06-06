package it.gabriele.androidware.game.view

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.MainThread
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.engine.network.NetworkMessage

abstract class StatefulNetworkGameView(
    context: Context,
): NetworkGameView(context) {

    companion object {
        private const val TAG = "StatefulNetworkGameView"
    }
    
    protected abstract val mGameStateManager: GameStateManager

    /***
     * Save the current game state inside a bundle.
     */
    @MainThread fun saveCurrentState(): Bundle {

        /* Build a new empty bundle that already contains the current key state. */
        val stateBundle = Bundle().apply {
            putInt(GameStateManager.KEY_CURRENT_GAME_STATE, mGameStateManager.currentKeyState)
        }

        /* Ask to the current game state to save its internal state. */
        mGameStateManager.saveGameState(stateBundle)

        Log.d(TAG, "saveCurrentState: saving current game state machine...")
        
        return stateBundle
    }

    final override fun readInputEvent(event: MotionEvent) {
        mGameStateManager.currentState?.readInputEvent(event)
    }

    final override fun readNetworkMessage(message: NetworkMessage) {
        mGameStateManager.currentState?.readNetworkMessage(message)
    }

    final override fun sendNetworkMessage(): NetworkMessage? {
        return mGameStateManager.currentState?.sendNetworkMessage()
    }

    final override fun render(canvas: Canvas) {
        mGameStateManager.currentState?.render(canvas)
    }

    final override fun update(deltaTime: Float) {
        mGameStateManager.currentState?.update(deltaTime)
    }

}