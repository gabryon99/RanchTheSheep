package it.gabriele.androidware.game.engine.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import it.gabriele.androidware.game.engine.GameContext

class AudioManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "[Game]::AudioManager"
    }

    var musicEnabled = true
        private set
    var soundsEnabled = true
        private set

    private val musicVolumes = mutableMapOf<@RawRes Int, Float>()
    private val musicPlayers = mutableMapOf<@RawRes Int, MediaPlayer>()
    private val soundsPlayers = mutableMapOf<@RawRes Int, MediaPlayer>()

    fun loadMusic(@RawRes musicId: Int) {
        loadAudio(musicPlayers, musicId)
    }

    fun loadSound(@RawRes soundId: Int) {
        loadAudio(soundsPlayers, soundId)
    }

    private fun loadAudio(players: MutableMap<Int, MediaPlayer>, @RawRes audioId: Int) {
        if (musicPlayers.containsKey(audioId)) {
            Log.i(TAG, "loadMusic: the audio ($audioId) is already loaded.")
            return
        }
        players[audioId] = MediaPlayer.create(context, audioId)
    }

    fun playMusic(@RawRes musicId: Int, volume: Float = 1f) {

        if (!musicPlayers.containsKey(musicId)) {
            Log.e(TAG, "playMusicInLoop: the audio ($musicId) hasn't been loaded!")
            return
        }

        musicVolumes[musicId] = volume

        if (musicEnabled) {
            play(musicPlayers, musicId, 0f)
        }
        else {
            play(musicPlayers, musicId, volume)
        }
    }

    fun playSound(@RawRes soundId: Int, volume: Float = 1f) {

        if (!soundsPlayers.containsKey(soundId)) {
            Log.e(TAG, "playMusicInLoop: the audio ($soundId) hasn't been loaded!")
            return
        }

        if (!soundsEnabled) {
            Log.d(TAG, "playSound: sounds muted!")
            return
        }
        else {
            play(soundsPlayers, soundId, volume)
        }
    }

    fun playOneShootSound(@RawRes soundId: Int, volume: Float = 1f) {

        if (!soundsEnabled) {
            return
        }

        MediaPlayer.create(context, soundId).apply {
            setVolume(volume, volume)
            setOnCompletionListener {
                it.release()
            }
        }.start()

    }

    private fun play(players: MutableMap<Int, MediaPlayer>, @RawRes audioId: Int, volume: Float) {
        players[audioId]?.apply {
            setVolume(volume, volume)
        }?.start()
    }

    fun playMusicInLoop(@RawRes musicId: Int, volume: Float = 1f) {

        if (!musicPlayers.containsKey(musicId)) {
            Log.e(TAG, "playMusicInLoop: the audio ($musicId) hasn't been loaded!")
            return
        }

        musicVolumes[musicId] = volume

        var realVolume = volume

        if (!musicEnabled) {
            realVolume = 0F
        }

        musicPlayers[musicId]?.apply {
            setVolume(realVolume, realVolume)
            isLooping = true
        }?.start()
    }

    fun resumeMusicPlayers() {
        musicPlayers.forEach { (_, player) -> player.start() }
    }

    fun pauseMusicPlayers() {
        musicPlayers.forEach { (_, player) -> player.pause() }
    }

    fun releaseMusicPlayer(@RawRes musicId: Int) {
        releasePlayer(musicPlayers, musicId)
    }

    fun releaseSoundPlayer(@RawRes soundId: Int) {
        releasePlayer(soundsPlayers, soundId)
    }

    private fun releasePlayer(players: MutableMap<Int, MediaPlayer>, @RawRes id: Int) {
        if (!players.containsKey(id)) {
            Log.e(TAG, "releasePlayer: the audio ($id) hasn't been loaded!")
            return
        }
        players.remove(id)?.release()
    }

    fun releaseAllMediaPlayers() {
        musicPlayers.forEach { (_, player) -> player.release() }
        soundsPlayers.forEach { (_, player) -> player.release() }
    }

    fun toggleMusics() {

        musicEnabled = !musicEnabled

        if (!musicEnabled) {
            musicPlayers.forEach { (_, player) -> player.setVolume(0f, 0f) }
        }
        else {
            musicPlayers.forEach { (id, player) ->
                val volume = musicVolumes[id] ?: 1f
                player.setVolume(volume, volume)
            }
        }

    }

    fun toggleSounds() {
        soundsEnabled = !soundsEnabled
    }

}