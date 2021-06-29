package it.gabriele.androidware.game.engine.states

import android.content.Context
import android.os.Bundle
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.engine.network.NetworkManager

abstract class GameState(
    protected val networkManager: NetworkManager,
    protected val audioManager: AudioManager,
    protected val gameStateManager: GameStateManager,
    protected val context: Context
): GameStateMachine {

    /* When the new state is switched/entered this method will be invoked. */
    open fun onStateStart(oldState: Bundle? = null, args: Bundle? = null) = Unit

    open fun onStateSave(state: Bundle) = Unit

    /* When the state it's going to terminate this method will be invoked. */
    open fun onStateEnd() = Unit

}