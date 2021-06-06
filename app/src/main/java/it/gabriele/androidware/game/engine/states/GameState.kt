package it.gabriele.androidware.game.engine.states

import android.content.Context
import android.os.Bundle

abstract class GameState(
    protected val mGameStateManager: GameStateManager,
    protected val context: Context
): GameStateMachine {

    /* When the new state is switched/entered this method will be invoked. */
    open fun onStateStart(oldState: Bundle? = null) = Unit

    open fun onStateSave(state: Bundle) = Unit

    /* When the state it's going to terminate this method will be invoked. */
    open fun onStateEnd() = Unit

}