package it.gabriele.androidware.fragments

import android.net.wifi.p2p.WifiP2pDevice
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import it.gabriele.androidware.R

class AvailableGamesListRecyclerViewAdapter(
    private val onGameClickListener: OnGameClickListener,
) : RecyclerView.Adapter<AvailableGamesListRecyclerViewAdapter.ViewHolder>() {

    private var availablePlayers: List<WifiP2pDevice> = listOf()

    fun updateList(devices: List<WifiP2pDevice>) {
        availablePlayers = devices
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_peer_list, parent, false),
            onGameClickListener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = availablePlayers[position]
        holder.txtDeviceName.text = device.deviceName
    }

    override fun getItemCount(): Int = availablePlayers.size

    inner class ViewHolder(
        view: View,
        private val mGameClickListener: OnGameClickListener
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val txtDeviceName: TextView = view.findViewById(R.id.txt_game_name)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mGameClickListener.onGameClick(availablePlayers[absoluteAdapterPosition])
        }
    }

}