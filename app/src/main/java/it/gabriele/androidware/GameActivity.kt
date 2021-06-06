package it.gabriele.androidware

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.gabriele.androidware.game.ranchllamas.LLamaGame
import it.gabriele.androidware.game.view.StatefulNetworkGameView

class GameActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GameActivity"
        private const val KEY_BUNDLE_GAME_STATE = "__GAME_STATE_PARCEL"
    }

    private lateinit var game: StatefulNetworkGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        game = LLamaGame(this, savedInstanceState?.getBundle(KEY_BUNDLE_GAME_STATE))

        setContentView(game)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the current game state inside the outState bundle
        // in order to recover the old game state. There are no worries
        // about race condition problem because the game will be paused
        // when the saving will occur.
        game.saveCurrentState().let { state ->
            outState.putBundle(KEY_BUNDLE_GAME_STATE, state)
        }

        Log.d(TAG, "onSaveInstanceState: saving bundle...")
    }

    override fun onResume() {
        super.onResume()
        game.resume()
    }

    override fun onPause() {
        super.onPause()
        game.pause()
    }

}