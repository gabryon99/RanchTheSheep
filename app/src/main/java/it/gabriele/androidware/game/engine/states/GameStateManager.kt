package it.gabriele.androidware.game.engine.states

import android.os.Bundle
import android.util.Log
import java.lang.RuntimeException

class GameStateManager {

    companion object {
        private const val TAG = "GameStateManager"
        const val KEY_CURRENT_GAME_STATE = "__CURRENT_GAME_STATE"
    }

    var currentKeyState = 0
    var currentState: GameState? = null
        private set

    private var states: Map<Int, GameState>? = null

    /* Initialize the states map and start the 0th state. */
    fun init(s: Map<Int, GameState>, bundleState: Bundle?) {

        /* Did the states list have been initialized it? */
        if (states == null) {

            /* Save the states map */
            states = s

            var startState = 0
            if (bundleState != null) {
                startState = bundleState.getInt(KEY_CURRENT_GAME_STATE, startState)
            }

            /* Check if the starting state is valid. */
            if (s[startState] == null) {
                throw RuntimeException("State '$startState' not found.")
            }

            /* Prepare current state */
            currentKeyState = startState
            currentState = s[startState]

            /* Pass the state bundle in order to recover the previous game state. */
            currentState?.onStateStart(bundleState)

            Log.i(TAG, "init: GameStateManager initialized it!")
        }
        else {
            throw RuntimeException("The GameStateManager has been already initialized it.")

        }
    }

    fun saveGameState(bundle: Bundle) {
        currentState?.onStateSave(bundle)
    }

    /* Switch the game in a new state.*/
    fun switch(newState: Int) {

        if (states == null) {
            throw RuntimeException("The GameStateManager hasn't been initialized it.")
        }

        val nextState = states!![newState]

        if (nextState != null) {

            currentState?.onStateEnd()

            currentKeyState = newState
            currentState = nextState

            currentState?.onStateStart()
        }
        else {
            throw RuntimeException("State '$newState' not found.")
        }
    }

}