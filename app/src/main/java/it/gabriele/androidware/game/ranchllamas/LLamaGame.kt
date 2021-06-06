package it.gabriele.androidware.game.ranchllamas

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.ranchllamas.states.SecondTestState
import it.gabriele.androidware.game.ranchllamas.states.FirstTestState
import it.gabriele.androidware.game.view.StatefulNetworkGameView

class LLamaGame(
    context: Context,
    savedBundle: Bundle?,
): StatefulNetworkGameView(context) {

    companion object {

        private const val TAG = "LLama Game"

        /* States */
        const val WAITING_FOR_OPPONENT = 0
        const val TEST = 1
    }

    override val mGameStateManager: GameStateManager = GameStateManager()

    init {
        mGameStateManager.init(mapOf(
            WAITING_FOR_OPPONENT to FirstTestState(mGameStateManager, context),
            TEST to SecondTestState(mGameStateManager, context)
        ), savedBundle)
    }

}