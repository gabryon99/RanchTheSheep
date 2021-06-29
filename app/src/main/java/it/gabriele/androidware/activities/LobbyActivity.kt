package it.gabriele.androidware.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import it.gabriele.androidware.R
import it.gabriele.androidware.activities.receivers.WifiP2StatusReceiver
import it.gabriele.androidware.fragments.AvailableGamesDialogFragment
import it.gabriele.androidware.game.engine.GameContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class LobbyActivity : AppCompatActivity(R.layout.activity_lobby), View.OnClickListener,
    View.OnFocusChangeListener, View.OnKeyListener {

    companion object {

        const val TAG = "LobbyActivity"

        const val SERVICE_UUID = "c14122c6-a0f8-4c1f-93bd-dcd3b09601a5"
        const val SSDP_SEARCH_TARGET = "uuid:${SERVICE_UUID}"
        const val UPNP_DEVICE_URN = "urn:schemas-upnp-org:device:1"

        private const val DISCOVERY_GAMES_REQUEST_CODE = 6000
        private const val CREATE_GAME_REQUEST_CODE = 6001
        private const val CONNECT_GAME_REQUEST_CODE = 6002

        private const val STATE_GAME_STARTED = "LobbyActivity.STATE_GAME_STARTED"

    }

    //region WiFi-P2P Fields

    private val statusIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var wifiP2pStatusReceiver: WifiP2StatusReceiver? = null

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var wifiDirectChannel: WifiP2pManager.Channel

    private var upnpServiceOffered: WifiP2pUpnpServiceInfo? = null
    private var upnpServiceRequest: WifiP2pUpnpServiceRequest? = null

    private var newGameName: String? = null
    private var alreadyConnected = false

    //endregion

    //region Activity's View, Widgets and Dialogs

    private lateinit var alreadyConnectedContainer: View
    private lateinit var lobbyContainer: View

    private lateinit var btnFindGame: Button
    private lateinit var btnStartGame: Button
    private lateinit var btnConnect: Button

    private lateinit var txtPlayerName: EditText

    private var snackBarNewGame: Snackbar? = null

    //endregion

    //region Other fields
    private lateinit var sharedPrefs: SharedPreferences
    //endregion

    //region Activity's Method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lobby)

        sharedPrefs = getSharedPreferences(GameContext.GAME_PREFS, Context.MODE_PRIVATE)

        /* Find widgets contained inside the view */
        initWidgets()
        /* Initialize WiFi P2P Manager and Channel */
        initWifiDirect()
    }

    private fun initWidgets() {
        lobbyContainer = findViewById(R.id.container_lobby_activity)
        alreadyConnectedContainer = findViewById(R.id.container_already_connected)
        txtPlayerName = findViewById<EditText>(R.id.txt_player_name).apply {
            onFocusChangeListener = this@LobbyActivity
            setOnKeyListener(this@LobbyActivity)
            setText(
                sharedPrefs.getString(
                    GameContext.GAME_PREFS_PLAYER_NAME,
                    getText(R.string.dummy_player_name).toString()
                )
            )
        }
        btnFindGame = findViewById<Button>(R.id.btn_find_game).apply {
            setOnClickListener(this@LobbyActivity)
        }
        btnStartGame = findViewById<Button>(R.id.btn_start_game).apply {
            setOnClickListener(this@LobbyActivity)
        }
        btnConnect = findViewById<Button>(R.id.btn_connect).apply {
            setOnClickListener(this@LobbyActivity)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            DISCOVERY_GAMES_REQUEST_CODE -> {

                /* If the user gave the access to fine location we can trigger the
                * peers discovery. */
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onBtnFindGameClick()
                }
                else {
                    Snackbar.make(lobbyContainer, R.string.permissions_required, Snackbar.LENGTH_LONG).show()
                }
            }
            CREATE_GAME_REQUEST_CODE -> {

                /* If the user gave the access to fine location we can trigger the
               * peers discovery. */
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onBtnStartGameClick()
                }
                else {
                    Snackbar.make(lobbyContainer, R.string.permissions_required, Snackbar.LENGTH_LONG).show()
                }
            }
            CONNECT_GAME_REQUEST_CODE -> {
                /* If the user gave the access to fine location we connect to the other peer! */
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onBtnConnectClick()
                }
                else {
                    Snackbar.make(lobbyContainer, R.string.permissions_required, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wifiP2pStatusReceiver = WifiP2StatusReceiver(wifiP2pManager, wifiDirectChannel, connectionStateChanged = {
            if (it.groupFormed) {
                startGame(it)
            }
            else {
                alreadyConnectedContainer.visibility = View.INVISIBLE
                alreadyConnected = false
            }
        }, statusStateChanged = { status ->
            if (status == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                alreadyConnectedContainer.visibility = View.INVISIBLE
                alreadyConnected = false
            }
        })
        registerReceiver(wifiP2pStatusReceiver, statusIntentFilter)

        /* If there is already a group show the Connect button! */
        wifiP2pManager.requestConnectionInfo(wifiDirectChannel) { connectionInfo ->
            if (connectionInfo.groupFormed) {
                alreadyConnected = true
                alreadyConnectedContainer.visibility = View.VISIBLE
            }
            else {
                alreadyConnected = false
                alreadyConnectedContainer.visibility = View.INVISIBLE
            }
        }

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiP2pStatusReceiver)
        wifiP2pStatusReceiver = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        /* If the player created a new game save it's name */
        if (newGameName != null) {
            Log.d(TAG, "onSaveInstanceState: $newGameName")
            outState.putString(STATE_GAME_STARTED, newGameName)
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        /* Restore the previous new game advertisement */
        savedInstanceState.getString(STATE_GAME_STARTED)?.let {
            startNewGame(it)
        }

    }

    //endregion

    //region WiFi-P2P Methods

    /***
     * Init the WiFi P2P Framework and register to the application context a broadcast receiver
     * to handle the framework's status.
     */
    private fun initWifiDirect() {
        
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiDirectChannel = wifiP2pManager.initialize(this, mainLooper, null)

        cleanupWifiP2pServicesAndRequests()
    }

    /***
     * Cleanup WiFi P2P services and request previously created.
     */
    private fun cleanupWifiP2pServicesAndRequests() {

        // Do some cleanup
        wifiP2pManager.clearServiceRequests(wifiDirectChannel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "onSuccess cleanup: clearServiceRequests")
            }

            override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

        })

        wifiP2pManager.clearLocalServices(wifiDirectChannel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "onSuccess cleanup: removeLocalService")
            }

            override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

        })
    }

    /***
     * Discover new available games near the player using the Universal Plug and Play
     * framework offered by the Wi-Fi P2P Service.
     */
    private fun discoverGames() {

        val permAccessFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
            val requiredPermission = Array(1) { Manifest.permission.ACCESS_FINE_LOCATION }
            ActivityCompat.requestPermissions(this, requiredPermission, DISCOVERY_GAMES_REQUEST_CODE)
            return
        }

        // Let's be sure to be disconnected from a connected peer
        disconnectIfConnected()

        wifiP2pManager.setUpnpServiceResponseListener(wifiDirectChannel) { uniqueServiceNames, srcDevice ->

            val devices = mutableMapOf<String, WifiP2pDevice>()
            uniqueServiceNames.forEach {serviceName ->

                Log.d(TAG, "discoverGames: $serviceName/${srcDevice.deviceName}/${srcDevice.deviceAddress}")

                // Did we find a game?
                if (serviceName.contains(SERVICE_UUID, true) && serviceName.contains("Game")) {

                    // Yes, we did! Let's grab the game's name to show it to the player
                    val gameName = serviceName.substringAfterLast(":")
                    if (!devices.containsKey(gameName)) {
                        srcDevice.deviceName = gameName
                        devices[gameName] = srcDevice
                    }

                }
            }

            /* Update the list of the services available */
            supportFragmentManager.findFragmentByTag(AvailableGamesDialogFragment.TAG)?.apply {
                if (this is AvailableGamesDialogFragment) {
                    this.updateList(devices.values.toList())
                }
            }
        }

        upnpServiceRequest = WifiP2pUpnpServiceRequest.newInstance(SSDP_SEARCH_TARGET)

        /* Add a service request to find games nearby */
        wifiP2pManager.addServiceRequest(wifiDirectChannel, upnpServiceRequest, object: WifiP2pManager.ActionListener {

            @SuppressLint("MissingPermission")
            override fun onSuccess() {

                Log.d(TAG, "onSuccess: new service request added successfully!")

                /* Now let's discover nearby services that match our request */
                wifiP2pManager.discoverServices(wifiDirectChannel, object : WifiP2pManager.ActionListener {

                    override fun onSuccess() {
                        Log.d(TAG, "onSuccess: discovering new services near us...")

                        toggleControls(false)

                        /* Show the available games dialog. */
                        AvailableGamesDialogFragment().apply{
                            show(supportFragmentManager, AvailableGamesDialogFragment.TAG)
                        }

                    }

                    override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

                })
            }

            override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

        })
    }

    private fun disconnectIfConnected() {
        if (alreadyConnected) {
            wifiP2pManager.removeGroup(wifiDirectChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    alreadyConnectedContainer.visibility = View.INVISIBLE
                    alreadyConnected = false
                }
                override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)
            })
        }
    }

    /***
     * Stop the discovery for a new game nearby the player if there is an active
     * game discovery request. On success the method enable the view's controls.
     */
    fun stopGameDiscovery() {
        if (upnpServiceRequest != null) {
            
            wifiP2pManager.removeServiceRequest(wifiDirectChannel, upnpServiceRequest, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    upnpServiceRequest = null
                    toggleControls(true)
                    Log.d(TAG, "onSuccess: removeServiceRequest")
                }

                override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

            })
            
        }
    }

    /**
     * Initiate a connection process to the other device. If the connection is successful
     * then a Broadcast Intent, with the action `WIFI_P2P_CONNECTION_CHANGED_ACTION`,
     * will be broadcast to the WifiP2pStatusReceiver instance, so the game can start.
     * For further details see `WifiP2StatusReceiver.onReceive` method. On success this method
     * stops the game discovery and close the active dialog showing the peers.
     */
    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        Log.d(TAG, "connect: connecting to the device ($device)")

        wifiP2pManager.connect(wifiDirectChannel, config, object : WifiP2pManager.ActionListener {
            
            override fun onSuccess() {

                Log.d(TAG, "onSuccess: connect")

                stopGameDiscovery()
                /* Use the support fragment manager to find the reference of the active game dialog */
                closeActiveGameDialog()

                AlertDialog.Builder(this@LobbyActivity).apply{
                    setTitle(R.string.attempt_to_connect)
                    setMessage(getString(R.string.be_patient, device.deviceName))
                    setNegativeButton(R.string.cancel) { _, _ ->
                        cancelConnect()
                    }
                }.create().apply {
                    setCanceledOnTouchOutside(false)
                    setCancelable(false)
                }.show()

            }

            override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

        })
    }

    private fun closeActiveGameDialog() {
        supportFragmentManager.findFragmentByTag(AvailableGamesDialogFragment.TAG)?.apply {
            if (this is DialogFragment) {
                this.dismiss()
            }
        }
    }

    /***
     * Cancel the connect process to another peer. On success re-enables the view's controls.
     */
    private fun cancelConnect() {

        wifiP2pManager.cancelConnect(wifiDirectChannel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "onSuccess: cancelConnect")
                toggleControls(true)
            }

            override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

        })
    }

    /***
     * Start a new game advertisement in the network to be discovered by any nearby devices.
     */
    private fun startNewGame(gameName: String? = null) {

        val permAccessFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
            val requiredPermission = Array(1) { Manifest.permission.ACCESS_FINE_LOCATION }
            ActivityCompat.requestPermissions(this, requiredPermission, CREATE_GAME_REQUEST_CODE)
            return
        }

        // Let's be sure to be disconnected from a connected peer
        disconnectIfConnected()

        val playerName = txtPlayerName.text.trim().toString().replace(" ", "_")
        val realGameName = gameName ?: String.format("%s#%04d", playerName, (0..9999).random())
        newGameName = realGameName

        val services = listOf("urn:schemas-upnp-org:service:Game:${realGameName}")
        upnpServiceOffered = WifiP2pUpnpServiceInfo.newInstance(SERVICE_UUID, UPNP_DEVICE_URN, services)
        wifiP2pManager.addLocalService(wifiDirectChannel, upnpServiceOffered, object : WifiP2pManager.ActionListener {
            
            override fun onSuccess() {

                Log.d(TAG, "onSuccess: addLocalService")
                toggleControls(false)

                snackBarNewGame = Snackbar.make(lobbyContainer, getString(R.string.waiting_for_player, realGameName), Snackbar.LENGTH_INDEFINITE).apply {
                    setAction(R.string.cancel) {
                        toggleControls(true)
                        stopNewGame()
                        this.dismiss()
                    }
                    show()
                }

            }

            override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

        })
        
    }

    /**
     * Stop the new game advertisement if there is an active game advertisement on the going.
     * On success re-enables the view's controls.
     */
    private fun stopNewGame() {
        
        if (upnpServiceOffered != null) {
            
            wifiP2pManager.removeLocalService(wifiDirectChannel, upnpServiceOffered, object : WifiP2pManager.ActionListener {
                
                override fun onSuccess() {
                    Log.d(TAG, "onSuccess: removeLocalService")
                    upnpServiceOffered = null
                    newGameName = null
                    toggleControls(true)
                }

                override fun onFailure(reason: Int) = handleActionListenerOnFailure(reason)

            })
            
        }
    }

    /***
     * Show a snackbar containing the error indicated by the 'reason' parameter.
     */
    private fun handleActionListenerOnFailure(reason: Int) {

        val error = when (reason) {
            WifiP2pManager.P2P_UNSUPPORTED -> {
                Log.e(TAG, "handleOnFailure: WiFi P2P is unsupported...")
                getString(R.string.p2p_unsupported)
            }
            WifiP2pManager.ERROR -> {
                Log.e(TAG, "handleOnFailure: internal error")
                getString(R.string.p2p_error)
            }
            WifiP2pManager.BUSY -> {
                Log.e(TAG, "handleOnFailure: the framework is busy")
                getString(R.string.p2p_busy)
            }
            else -> {
                "Unknown"
            }
        }

        val errorMsg = getString(R.string.generic_error, error)
        Snackbar.make(lobbyContainer, errorMsg, Snackbar.LENGTH_SHORT).show()
    }

    //endregion

    //region Widgets Listener Methods

    override fun onFocusChange(v: View?, hasFocus: Boolean) {

        if (v == txtPlayerName && !hasFocus) {

            // When the edit text loses its focus let's save the player's name
            // inside the shared preferences.
            Log.d(TAG, "onFocusChange: saving player's name into preferences...")
            savePlayerName()
        }

    }

    override fun onClick(v: View?) {
        when (v) {
            btnFindGame -> onBtnFindGameClick()
            btnStartGame -> onBtnStartGameClick()
            btnConnect -> onBtnConnectClick()
        }
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        return true
    }

    //endregion
    
    //region Buttons Methods

    private fun savePlayerName() {
        lifecycleScope.launch(Dispatchers.IO) {
            sharedPrefs.edit(commit = true) {
                putString(GameContext.GAME_PREFS_PLAYER_NAME, txtPlayerName.text.trim().toString())
            }
        }
    }

    private fun onBtnConnectClick() {

        val permAccessFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
            val requiredPermission = Array(1) { Manifest.permission.ACCESS_FINE_LOCATION }
            ActivityCompat.requestPermissions(this, requiredPermission, CONNECT_GAME_REQUEST_CODE)
            return
        }

        wifiP2pManager.requestConnectionInfo(wifiDirectChannel) { connectionInfo ->
            if (connectionInfo.groupFormed) {
                startGame(connectionInfo)
            }
            else {
                Snackbar.make(lobbyContainer, R.string.not_connected_anymore, Snackbar.LENGTH_LONG).show()
                alreadyConnectedContainer.visibility = View.INVISIBLE
            }
        }
    }

    private fun onBtnFindGameClick() {
        discoverGames()
    }

    private fun onBtnStartGameClick() {
        startNewGame()
    }

    private fun toggleControls(enabled: Boolean = false) {
        btnStartGame.isEnabled = enabled
        btnFindGame.isEnabled = enabled
        btnConnect.isEnabled = enabled
        txtPlayerName.isEnabled = enabled
    }

    private fun startGame(connectionInfo: WifiP2pInfo) {

        // only the guest has the game dialog shown
        if (!connectionInfo.isGroupOwner) {
            closeActiveGameDialog()
            stopGameDiscovery()
            toggleControls(true)
        }
        else {
            // Stop the new game advertisement if we are the party's owner
            stopNewGame()
            snackBarNewGame?.dismiss()
            toggleControls(true)
        }

        val dialog = AlertDialog.Builder(this)
            .create()
            .apply {
                setTitle(R.string.starting_game)
                setMessage(getText(R.string.starting_game_message))
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }

        lifecycleScope.launch {
            /* After 500 ms close the open dialog */
            delay(500L)
            dialog.dismiss()
        }

        /* Let's start the game activity */
        val startGameIntent = Intent(this, GameActivity::class.java)
        startGameIntent.putExtra(GameActivity.INTENT_NETWORK_INFO_EXTRA, connectionInfo)
        startActivity(startGameIntent)

    }

    //endregion

}