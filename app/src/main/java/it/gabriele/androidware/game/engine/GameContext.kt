package it.gabriele.androidware.game.engine

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.media.MediaPlayer
import android.util.Log
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

object GameContext {

    const val TAG = "GameContext"

    //region Game Constants
    const val MAX_FPS: Int = 60
    const val NET_PORT: Int = 53127

    const val LOGICAL_PORTRAIT_WIDTH = 320
    const val LOGICAL_PORTRAIT_HEIGHT = 480

    var SCALE_X = 1
    var SCALE_Y = 1

    //endregion

    //region Preferences Field
    const val GAME_PREFS = "__game_prefs"
    const val GAME_PREFS_PLAYER_NAME = "${GAME_PREFS}_player_name"
    const val GAME_PREFS_SOUNDS_ENABLED = "${GAME_PREFS}_sounds_enabled"
    const val GAME_PREFS_MUSIC_ENABLED = "${GAME_PREFS}_music_enabled"
    //endregion

}