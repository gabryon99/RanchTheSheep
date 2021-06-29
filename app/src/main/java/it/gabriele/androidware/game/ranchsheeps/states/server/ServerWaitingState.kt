package it.gabriele.androidware.game.ranchsheeps.states.server

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import it.gabriele.androidware.R
import it.gabriele.androidware.game.engine.network.NetworkManager
import it.gabriele.androidware.game.engine.network.NetworkMessage
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.math.Vector2
import it.gabriele.androidware.game.ranchsheeps.RanchSheepsGame
import it.gabriele.androidware.game.ranchsheeps.game.Sheep
import it.gabriele.androidware.game.ranchsheeps.states.common.RanchTheSheepsState
import java.nio.ByteBuffer
import kotlin.random.Random
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.view.StatefulNetworkGameView

class ServerWaitingState(
    networkManager: NetworkManager,
    audioManager: AudioManager,
    gameStateManager: GameStateManager,
    context: Context
) : GameState(networkManager, audioManager, gameStateManager, context) {

    companion object {
        const val TAG = "[Game]::ServerWaitingState"
        private const val DELTA_SECONDS = 5
    }

    private val transformMatrix = Matrix()
    private var grassTexture: Bitmap? = null
    private var whiteSheepSprite: Bitmap? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
        setShadowLayer(10f, 0f, 4f, Color.BLACK)
    }

    private val sheeps: ArrayList<Sheep> = ArrayList(RanchSheepsGame.MAX_SHEEPS)
    private var startTime = 0L

    override fun onStateStart(oldState: Bundle?, args: Bundle?) {

        if (oldState != null) {
            startTime = 0
            sheeps.clear()
        }

        grassTexture = RenderUtilities.getBitmapFromCache(
            context.resources,
            R.drawable.grass,
            512, 512
        )
        whiteSheepSprite = RenderUtilities.getBitmapFromCache(
            context.resources,
            R.drawable.my_sheep,
            512, 512
        )

        var index = 0
        repeat(RanchSheepsGame.MAX_SHEEPS) {

            val sheepColor = index.mod(2).toByte()

            val sheepPosition = Vector2().apply {
                x = Random.nextFloat()
                y = Random.nextFloat()
            }

            val sheepVelocity = Vector2().apply {
                x = (Random.nextFloat() + RanchSheepsGame.MIN_SHEEP_SPEED).mod(RanchSheepsGame.MAX_SHEEP_SPEED)
                y = (Random.nextFloat() + RanchSheepsGame.MIN_SHEEP_SPEED).mod(RanchSheepsGame.MAX_SHEEP_SPEED)
            }

            sheeps.add(Sheep(sheepColor, Sheep.VISIBLE, sheepPosition, sheepVelocity))
            index++
        }

        val messageSizeBytes = Long.SIZE_BYTES + (Sheep.SIZE_BYTES * RanchSheepsGame.MAX_SHEEPS)
        val messageToSend = ByteBuffer.allocate(messageSizeBytes)

        // Send Start Time
        startTime = (System.currentTimeMillis() + DELTA_SECONDS * 1000)
        messageToSend.putLong(startTime)

        // Send Sheeps Positions
        sheeps.forEach { sheep ->
            messageToSend.apply {
                put(sheep.color)
                put(sheep.visible)
                putFloat(sheep.position!!.x)
                putFloat(sheep.position.y)
                putFloat(sheep.velocity!!.x)
                putFloat(sheep.velocity.y)
            }
        }

        networkManager.send(NetworkMessage(messageToSend))

        Log.d(TAG, "onStateStart: startTime($startTime)")

        audioManager.loadMusic(R.raw.bg_music)
        audioManager.playMusicInLoop(R.raw.bg_music, 0.5f)

        Log.d(TAG, "onStateStart: sheeps info, start time ($startTime)")

    }

    override fun onStateEnd() = Unit

    override fun update(deltaTime: Float) {

        val now = (System.currentTimeMillis() / 1000)
        if (now >= (startTime / 1000)) {
            gameStateManager.switch(RanchSheepsGame.GAME_STATE, bundleOf(
                RanchTheSheepsState.BUNDLE_SHEEPS to sheeps
            ))
        }
    }

    override fun render(canvas: Canvas) {

        RenderUtilities.drawTiledBackground(canvas, grassTexture!!)
        canvas.drawARGB(96, 32, 32, 32)

        val loadingMessage = "${context.getText(R.string.loading)}\n${context.getText(R.string.catch_white_sheep)}"

        val cx = (canvas.width / 2f) - (paint.measureText(loadingMessage) / 2f)
        val cy = (canvas.height / 2f) - (loadingMessage.length / 2f)

        canvas.drawText(loadingMessage, cx, cy, paint)

        val sx = (canvas.width / 2f) - (whiteSheepSprite!!.width / 2f)
        val sy = (canvas.height / 2f) - (whiteSheepSprite!!.height / 2f) + 128
        transformMatrix.setTranslate(sx, sy)

        canvas.drawBitmap(whiteSheepSprite!!, transformMatrix, null)


    }


}