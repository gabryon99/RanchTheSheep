package it.gabriele.androidware.game.ranchsheeps.states.client

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import it.gabriele.androidware.R
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.engine.network.NetworkManager
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.math.Vector2
import it.gabriele.androidware.game.ranchsheeps.RanchSheepsGame
import it.gabriele.androidware.game.ranchsheeps.game.Sheep
import it.gabriele.androidware.game.ranchsheeps.states.common.RanchTheSheepsState
import it.gabriele.androidware.game.view.StatefulNetworkGameView

class ClientWaitingState(
    networkManager: NetworkManager,
    audioManager: AudioManager,
    gameStateManager: GameStateManager,
    context: Context
): GameState(networkManager, audioManager, gameStateManager, context) {

    companion object {
        const val TAG = "[Game]::ClientWaitingState"
    }

    private var grassTexture: Bitmap? = null
    private var darkSheepSprite: Bitmap? = null

    private val transformMatrix = Matrix()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
        setShadowLayer(10f, 0f, 4f, Color.BLACK)
    }

    private var packetReceived: Boolean = false
    private var startTime = 0L
    private val sheeps: ArrayList<Sheep> = ArrayList()

    override fun onStateStart(oldState: Bundle?, args: Bundle?) {

        Log.d(TAG, "onStateStart: $oldState, $args")
        
        if (oldState != null) {
            startTime = 0L
            packetReceived = false
            sheeps.clear()
        }

        grassTexture = RenderUtilities.getBitmapFromCache(
            context.resources, R.drawable.grass,
            512, 512
        )
        darkSheepSprite = RenderUtilities.getBitmapFromCache(
            context.resources, R.drawable.my_dark_sheep,
            512, 512
        )

        audioManager.loadMusic(R.raw.bg_music)
        audioManager.playMusicInLoop(R.raw.bg_music, 0.5f)
    }

    override fun onStateEnd() {
        darkSheepSprite = null
    }

    override fun update(deltaTime: Float) {

        if (!packetReceived) {
            networkManager.receive()?.let { networkMessage ->

                if (networkMessage.buffer == null) return@let

                startTime = networkMessage.buffer.long

                Log.d(TAG, "update: startTime: ($startTime)")

                for (i in 0 until RanchSheepsGame.MAX_SHEEPS) {

                    val sheepColor = networkMessage.buffer.get()
                    val sheepVisibility = networkMessage.buffer.get()
                    val sheepPosX = networkMessage.buffer.float
                    val sheepPosY = networkMessage.buffer.float
                    val sheepVelX = networkMessage.buffer.float
                    val sheepVelY = networkMessage.buffer.float

                    sheeps.add(
                        Sheep(sheepColor, sheepVisibility, Vector2(sheepPosX , sheepPosY), Vector2(sheepVelX, sheepVelY))
                    )
                }

                packetReceived = true
            }
        }
        else {

            val now = (System.currentTimeMillis() / 1000)
            if (now >= (startTime / 1000)) {
                gameStateManager.switch(RanchSheepsGame.GAME_STATE, bundleOf(
                    RanchTheSheepsState.BUNDLE_SHEEPS to sheeps
                ))
            }
        }

    }

    override fun render(canvas: Canvas) {

        RenderUtilities.drawTiledBackground(canvas, grassTexture!!)
        canvas.drawARGB(96, 32, 32, 32)

        val loadingMessage = "${context.getText(R.string.loading)}\n${context.getText(R.string.catch_dark_sheep)}"

        val cx = (canvas.width / 2f) - (paint.measureText(loadingMessage) / 2f)
        val cy = (canvas.height / 2f) - (loadingMessage.length / 2f)

        canvas.drawText(loadingMessage, cx, cy, paint)

        val sx = (canvas.width / 2f) - (darkSheepSprite!!.width / 2f)
        val sy = (canvas.height / 2f) - (darkSheepSprite!!.height / 2f) + 128
        transformMatrix.setTranslate(sx, sy)

        canvas.drawBitmap(darkSheepSprite!!, transformMatrix, null)

    }


}