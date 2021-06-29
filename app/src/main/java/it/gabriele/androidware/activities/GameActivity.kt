package it.gabriele.androidware.activities

import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import it.gabriele.androidware.R
import it.gabriele.androidware.activities.receivers.WifiP2StatusReceiver
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.network.ClientNetworkManager
import it.gabriele.androidware.game.engine.network.NetworkMessage
import it.gabriele.androidware.game.engine.network.ServerNetworkManager
import it.gabriele.androidware.game.ranchsheeps.RanchSheepsGame
import it.gabriele.androidware.game.view.StatefulNetworkGameView
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class GameActivity : AppCompatActivity(), View.OnClickListener {

    companion object {

        const val TAG = "[Game]::GameActivity"

        const val INTENT_NETWORK_INFO_EXTRA = "GameActivity.NETWORK_INFO_EXTRA"

        const val KEY_GAME_ENDED = "GameActivity.KEY_GAME_ENDED"

        private const val SHOW_END_GAME_DIALOG_DELAY = 3000L
        private const val RECONNECTION_NEXT_ROUND_DELAY = 3000L
        private const val MAX_ATTEMPTS_RECONNECTION = 5

        private const val NET_END_GAME =    (0x00000000).toByte()
        private const val NET_PLAY_AGAIN =  (0x00000001).toByte()
    }

    //region Game Fields
    private var game: RanchSheepsGame? = null

    private var gameEnded = false
    private var wasConnected = false
    //endregion

    //region Other fields
    private lateinit var sharedPrefs: SharedPreferences
    //endregion

    //region Activity's Widgets
    private lateinit var gameContainer: FrameLayout
    private lateinit var pbWaiting: ProgressBar
    private lateinit var btnMuteSounds: Button
    private lateinit var btnMuteMusics: Button
    private lateinit var txtConnecting: TextView
    //endregion

    //region Wi-Fi P2p Fields
    private val statusIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var wifiP2pStatusReceiver: WifiP2StatusReceiver? = null

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var wifiDirectChannel: WifiP2pManager.Channel
    //endregion
    
    //region Activity's Methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        sharedPrefs = getSharedPreferences(GameContext.GAME_PREFS, Context.MODE_PRIVATE)
        gameEnded = savedInstanceState?.getBoolean(KEY_GAME_ENDED) ?: false

        initializeWidgets()
        initWifiDirect()

        /* Get the network info and prepare the game start */
        val networkInfo = intent.getParcelableExtra<WifiP2pInfo>(INTENT_NETWORK_INFO_EXTRA) ?: return

        initializeGame(savedInstanceState, networkInfo)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged: $newConfig")
    }

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        // Save the current game state inside the outState bundle
        // in order to recover the old game state.
        game?.saveCurrentState().let { state ->
            Log.d(TAG, "onSaveInstanceState: saving state bundle...")

            if (wasConnected) {
                state?.putBoolean(StatefulNetworkGameView.STATE_CONNECTION_LOST, true)
            }

            outState.putBundle(StatefulNetworkGameView.BUNDLE_GAME_STATE, state)
        }

        outState.putBoolean(KEY_GAME_ENDED, gameEnded)

        Log.d(TAG, "onSaveInstanceState: $outState")
    }

    override fun onResume() {
        super.onResume()

        wifiP2pStatusReceiver = WifiP2StatusReceiver(wifiP2pManager, wifiDirectChannel, statusStateChanged = { status ->

                /* If the user disables the Wifi then we get back to the Lobby Activity */
                if (status == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {

                    val dialog = AlertDialog.Builder(this).apply {
                        setCancelable(false)
                        setFinishOnTouchOutside(false)
                        setTitle(R.string.wifi_p2p_disabled)
                        setMessage(R.string.wifi_p2p_required)
                    }.create()

                    dialog.show()

                    /* Hide the dialog and return to the Lobby Activity after 2 seconds */
                    lifecycleScope.launch {
                        delay(2000L)
                        dialog.dismiss()
                        finish()
                    }
                }
            })
        registerReceiver(wifiP2pStatusReceiver, statusIntentFilter)

        /* Play/Resume the game view... */
        if (game != null) {
            
            if (wasConnected) {
                Log.d(TAG, "onResume: lost connection, let's try to reestablish it")
                game?.resume()
                tryToReconnect()
            }
            
            toggleButtons(true)
        }
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(wifiP2pStatusReceiver)
        wifiP2pStatusReceiver = null

        /* Pause the game view... */
        if (game != null) {

            game?.networkManager?.disconnect()

            game?.pause()
            toggleButtons(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (gameEnded) {
            /* Free the audio resources */
            game?.audioManager?.releaseAllMediaPlayers()
        }
    }

    override fun onClick(v: View) {
        when (v) {
            btnMuteMusics -> {

                game?.audioManager?.toggleMusics()

                /* Save preference inside the shared preferences */
                lifecycleScope.launch(Dispatchers.IO) {
                    sharedPrefs.edit(commit = true) {
                        putBoolean(
                            GameContext.GAME_PREFS_MUSIC_ENABLED,
                            game?.audioManager?.musicEnabled ?: true
                        )
                    }
                }

                if (game?.audioManager?.musicEnabled == true) {
                    btnMuteMusics.text = getText(R.string.music_enabled)
                } else {
                    btnMuteMusics.text = getText(R.string.music_disabled)
                }
            }
            btnMuteSounds -> {

                game?.audioManager?.toggleSounds()

                /* Save preference inside the shared preferences */
                lifecycleScope.launch(Dispatchers.IO) {
                    sharedPrefs.edit(commit = true) {
                        putBoolean(
                            GameContext.GAME_PREFS_SOUNDS_ENABLED,
                            game?.audioManager?.soundsEnabled ?: true
                        )
                    }
                }

                if (game?.audioManager?.soundsEnabled == true) {
                    btnMuteSounds.text = getText(R.string.sounds_enabled)
                } else {
                    btnMuteSounds.text = getText(R.string.sounds_disabled)
                }
            }
        }
    }

    //endregion

    //region Initialize Methods
    private fun initializeGame(savedInstanceState: Bundle?, networkInfo: WifiP2pInfo) {

        val soundsEnabled = sharedPrefs.getBoolean(GameContext.GAME_PREFS_SOUNDS_ENABLED, true)
        val musicEnabled = sharedPrefs.getBoolean(GameContext.GAME_PREFS_MUSIC_ENABLED, true)

        game = RanchSheepsGame(this@GameActivity, savedInstanceState)

        /* If we are the group owner then use a ServerNetworkManager */
        val networkManager = if (networkInfo.isGroupOwner) {
            ServerNetworkManager(game!!, networkInfo)
        } else {
            ClientNetworkManager(game!!, networkInfo)
        }

        if (!soundsEnabled) {
            game?.audioManager?.toggleSounds()
            btnMuteSounds.text = getText(R.string.sounds_disabled)
        }
        if (!musicEnabled) {
            game?.audioManager?.toggleMusics()
            btnMuteMusics.text = getText(R.string.music_disabled)
        }

        /* Register managers to be used by the game states */
        game?.registerNetworkManager(networkManager) { oldState ->
            Log.d(TAG, "connection lost! What do we have to do now? Old state: $oldState")
            tryToReconnect()
        }
        /* Load game states inside the GameState Manager */
        game?.initGameStateManager()

        lifecycleScope.launch {

            // Show progress bar indicating that peer is waiting
            // to establishing the connection
            pbWaiting.visibility = View.VISIBLE

            // Try to connect to the other peer
            withContext(Dispatchers.IO) {
                networkManager.connect()
            }

            // The peers are connected! Start the game!
            if (networkManager.connected) {

                pbWaiting.visibility = View.INVISIBLE

                wasConnected = true
                networkManager.beginCommunication()

                gameContainer.addView(game, 0)
                game?.resume()
            }
            else {
                // The peers failed the connection, return to the lobby activity.
                finish()
            }
        }

    }

    private fun initWifiDirect() {
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiDirectChannel = wifiP2pManager.initialize(this, mainLooper, null)
    }

    private fun initializeWidgets() {

        pbWaiting = findViewById(R.id.pb_waiting)
        btnMuteMusics = findViewById(R.id.btn_mute_music)
        btnMuteSounds = findViewById(R.id.btn_mute_sound)
        gameContainer = findViewById(R.id.game_container)
        txtConnecting = findViewById(R.id.txt_connecting)

        btnMuteMusics.setOnClickListener(this)
        btnMuteSounds.setOnClickListener(this)
    }
    //endregion

    /***
     *  Called from the Game View to indicate the game ending...
     */
    fun endGame() {

        /* The game has ended! */
        gameEnded = true

        lifecycleScope.launch(Dispatchers.Main) {

            game?.pause()

            // After SHOW_END_GAME_DIALOG_DELAY milliseconds...
            delay(SHOW_END_GAME_DIALOG_DELAY)

            // Show an end dialog
            if (game?.networkManager?.isGameMaster == true) {
                showHostDialog()
            } else {
                showGuestDialog()
            }
        }
    }

    /***
     * Try to reconnect to the other peer.
     */
    private fun tryToReconnect() {
        lifecycleScope.launch {

            game?.networkManager?.disconnect()

            var attempts = MAX_ATTEMPTS_RECONNECTION

            // Show loading bar
            pbWaiting.visibility = View.VISIBLE
            txtConnecting.visibility = View.VISIBLE
            txtConnecting.text = getString(R.string.try_connection, attempts)

            while (attempts > 0 && game?.networkManager?.connected == false) {

                withContext(Dispatchers.IO) {
                    game?.networkManager?.connect()
                }

                if (game?.networkManager?.connected == false){
                    attempts--
                    txtConnecting.text = getString(R.string.try_connection, attempts)
                    delay(RECONNECTION_NEXT_ROUND_DELAY)
                }
            }

            // Did we fail?
            if (attempts == 0) {
                // Yes, we did. Let's terminate the activity
                finish()
            }
            else {

                pbWaiting.visibility = View.INVISIBLE
                txtConnecting.visibility = View.INVISIBLE

                // We are back! Let's begin the communication again
                game?.networkManager?.beginCommunication()
                game?.signalConnectionRecovery()

                if (gameEnded) {
                    lifecycleScope.launch {
                        // Show an end dialog
                        if (game?.networkManager?.isGameMaster == true) {
                            showHostDialog()
                        } else {
                            showGuestDialog()
                        }
                    }
                }
            }
        }
    }

    /***
     * Restart the game re-initializing it.
     */
    private fun restartGame() {
        gameEnded = false
        game?.restartGame()
        game?.resume()
    }

    /***
     * Show an alert dialog to ask if the host player would like
     * to play another game.
     */
    private fun showHostDialog() {

        val msg = ByteBuffer.allocate(Byte.SIZE_BYTES)

        AlertDialog.Builder(this@GameActivity).apply {
            setTitle(R.string.game_over)
            setMessage(R.string.play_again)
            setPositiveButton(R.string.yes) { dialog, _ ->

                /* The host would like to play again... */
                msg.put(NET_PLAY_AGAIN)
                game?.networkManager?.send(NetworkMessage(msg))

                dialog.dismiss()

                /* Restart the game... */
                restartGame()
            }
            setNegativeButton(R.string.no) { dialog, _ ->

                /* The host request the end game */
                msg.put(NET_END_GAME)
                game?.networkManager?.send(NetworkMessage(msg))

                dialog.dismiss()

                /* Terminate the activity and return to the lobby activity */
                finish()
            }
        }.create().show()
    }

    /***
     * Show an alert dialog to the guest player at the end of the game.
     */
    private suspend fun showGuestDialog() {
        // The guest must wait for the host decision
        val dialog = AlertDialog.Builder(this@GameActivity).apply {
            setTitle(R.string.game_over)
            setMessage(R.string.waiting_for_host)
            setCancelable(false)
            setFinishOnTouchOutside(false)
        }.create()

        dialog.show()

        withContext(Dispatchers.IO) {

            val messageFromHost = game?.networkManager?.receiveBlocking(10000)
            if (messageFromHost == null) {
                finish()
                return@withContext
            }

            when (messageFromHost.buffer?.get()) {
                NET_END_GAME -> {
                    /* Terminate the activity and return to the lobby activity */
                    finish()
                }
                NET_PLAY_AGAIN -> {
                    /* Restart the game... */
                    restartGame()
                }
                else -> {
                    Log.d(TAG, "endGame: unrecognized network message :/")
                }
            }

            // Dismiss the shown dialog
            dialog.dismiss()
        }
    }

    /***
     * Enable or disable music/sounds buttons.
     */
    private fun toggleButtons(enabled: Boolean) {
        btnMuteMusics.isEnabled = enabled
        btnMuteSounds.isEnabled = enabled
    }

}