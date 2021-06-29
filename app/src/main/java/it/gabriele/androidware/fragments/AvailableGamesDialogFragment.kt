package it.gabriele.androidware.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import it.gabriele.androidware.R
import it.gabriele.androidware.activities.LobbyActivity

/**
 * A fragment representing a list of Items.
 */
class AvailableGamesDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "PeersListFragment"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var listAdapter: AvailableGamesListRecyclerViewAdapter

    fun updateList(devices: List<WifiP2pDevice>) {

        if (devices.isEmpty()) {
            recyclerView.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }
        else {
            recyclerView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }

        listAdapter.updateList(devices)

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val activity = requireActivity()
        val view = activity.layoutInflater.inflate(R.layout.fragment_peers_list, null)

        recyclerView = view.findViewById<RecyclerView>(R.id.available_games_list)
        progressBar = view.findViewById<ProgressBar>(R.id.progress_peers)
        listAdapter = AvailableGamesListRecyclerViewAdapter { device ->
            /* Let's connect to the device when clicking a list item! */
            if (activity is LobbyActivity) {
                activity.connect(device)
            }
            else {
                Log.w(TAG, "onCreateDialog: unrecognized activity for this fragment!")
            }
        }

        recyclerView.adapter = listAdapter

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.finding_players)
            .setNegativeButton(R.string.cancel) { _, _ ->
                /* When the user dismiss the dialog then stop the game discovery */
                if (activity is LobbyActivity) {
                    activity.stopGameDiscovery()
                }
                else {
                    Log.w(TAG, "onCreateDialog: unrecognized activity for this fragment!")
                }
            }
            .setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            }
            .setView(view)
            .create().apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
    }

}