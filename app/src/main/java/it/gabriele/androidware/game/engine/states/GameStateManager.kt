package it.gabriele.androidware.game.engine.states

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import java.lang.RuntimeException

class GameStateManager {

    companion object {
        private const val TAG = "GameStateManager"
        const val KEY_CURRENT_GAME_STATE = "GameStateManager.CURRENT_GAME_STATE"
        const val KEY_RESTART = "GameStateManager.KEY_RESTART"
    }

    var currentKeyState = 0
    var currentState: GameState? = null
        private set

    private var states: Map<Int, GameState>? = null

    fun loadStates(states: Map<Int, GameState>) {
        this.states = states
    }

    fun start(bundleState: Bundle?) {

        if (states == null) {
            throw RuntimeException("The states haven't been initialized it!")
        }

        var startState = 0
        if (bundleState != null) {
            startState = bundleState.getInt(KEY_CURRENT_GAME_STATE, startState)
        }

        /* Check if the starting state is valid. */
        if (states!![startState] == null) {
            throw RuntimeException("State '$startState' not found.")
        }

        Log.i(TAG, "start: started!")

        /* Prepare current state */
        currentKeyState = startState
        currentState = states!![startState]

        /* Pass the state bundle in order to recover the previous game state. */
        currentState?.onStateStart(bundleState)
    }

    fun saveGameState(bundle: Bundle) {
        currentState?.onStateSave(bundle)
    }

    fun restart(initialState: Int) {

        if (states == null) {
            throw RuntimeException("The GameStateManager hasn't been initialized it.")
        }

        currentKeyState = initialState
        currentState = states!![initialState]

        currentState?.onStateStart(bundleOf(KEY_RESTART to true))
    }

    /* Switch the game in a new state.*/
    fun switch(newState: Int, args: Bundle? = null, oldState: Bundle? = null) {

        if (states == null) {
            throw RuntimeException("The GameStateManager hasn't been initialized it.")
        }

        val nextState = states!![newState]

        if (nextState != null) {

            Log.d(TAG, "switch: switching to ($newState, ${nextState.javaClass.name}) state... (${Thread.currentThread().name})")
            
            currentState?.onStateEnd()

            currentKeyState = newState
            currentState = nextState

            currentState?.onStateStart(oldState, args)
        }
        else {
            throw RuntimeException("State '$newState' not found.")
        }
    }

}