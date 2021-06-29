package it.gabriele.androidware.game.ranchsheeps

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.os.bundleOf
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.ranchsheeps.states.client.ClientWaitingState
import it.gabriele.androidware.game.ranchsheeps.states.common.EndGameState
import it.gabriele.androidware.game.ranchsheeps.states.common.RanchTheSheepsState
import it.gabriele.androidware.game.ranchsheeps.states.server.ServerWaitingState
import it.gabriele.androidware.game.view.StatefulNetworkGameView
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.ranchsheeps.states.common.DisconnectedState

class RanchSheepsGame(
    context: Context,
    oldState: Bundle?,
): StatefulNetworkGameView(context, oldState) {

    companion object {

        private const val TAG = "[Game]::RanchSheepsGame"

        const val MAX_SHEEPS = 6
        const val MIN_SHEEP_SPEED = 300f
        const val MAX_SHEEP_SPEED = 450f

        /* Custom States */
        const val GAME_STATE = 1
        const val END_STATE = 2
    }

    override val gameStateManager: GameStateManager = GameStateManager()

    override fun initGameStateManager() {

        if (networkManager == null) {
            throw RuntimeException("The network manager must be registered to the game!")
        }

        val waitingState = getInitialWaitingState()
        gameStateManager.loadStates(mapOf(
            INITIAL_GAME_STATE to waitingState,
            DISCONNECTED_STATE to DisconnectedState(networkManager!!, audioManager, gameStateManager, context),
            GAME_STATE to RanchTheSheepsState(networkManager!!, audioManager, gameStateManager, context),
            END_STATE to EndGameState(networkManager!!, audioManager, gameStateManager, context)
        ))
    }

    @WorkerThread
    override fun handleConnectionLost() {
        super.handleConnectionLost()
        Log.d(TAG, "handleConnectionLost: changing to disconnected scene! I guess?")
        gameStateManager.switch(DISCONNECTED_STATE)
    }

    @WorkerThread
    override fun handleConnectionRecovery() {

        Log.d(TAG, "handleConnectionRecovery: $lostConnectionState - $oldState")

        var bundle: Bundle? = null

        if (lostConnectionState != null) {
            bundle = lostConnectionState
        }
        else if (oldState != null) {
            bundle = oldState?.getBundle(BUNDLE_GAME_STATE)
        }

        Log.d(TAG, "handleConnectionRecovery: $bundle")
        val previousState = bundle?.getInt(GameStateManager.KEY_CURRENT_GAME_STATE) ?: INITIAL_GAME_STATE
        gameStateManager.switch(previousState, oldState = bundle)
    }

    private fun getInitialWaitingState(): GameState {
        return if (networkManager!!.isGameMaster) {
            ServerWaitingState(networkManager!!, audioManager, gameStateManager, context)
        } else {
            ClientWaitingState(networkManager!!, audioManager, gameStateManager, context)
        }
    }

    @MainThread
    override fun restartGame() {
        super.restartGame()
        initGameStateManager()
    }

}