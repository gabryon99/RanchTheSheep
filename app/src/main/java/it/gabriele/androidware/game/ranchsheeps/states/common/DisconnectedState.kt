package it.gabriele.androidware.game.ranchsheeps.states.common

import android.content.Context
import android.graphics.*
import android.os.Bundle
import it.gabriele.androidware.R
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.engine.network.NetworkManager
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager

class DisconnectedState(
    networkManager: NetworkManager,
    audioManager: AudioManager,
    gameStateManager: GameStateManager,
    context: Context
) : GameState(networkManager, audioManager, gameStateManager, context) {

    companion object {
        const val TAG = "[Game]::DisconnectedState"
    }

    private var grassTexture: Bitmap? = null
    private var darkSheepSprite: Bitmap? = null

    private val transformMatrix = Matrix()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
        setShadowLayer(10f, 0f, 4f, Color.BLACK)
    }

    override fun onStateStart(oldState: Bundle?, args: Bundle?) {

        grassTexture = RenderUtilities.getBitmapFromCache(
            context.resources, R.drawable.grass,
            512, 512
        )
        darkSheepSprite = RenderUtilities.getBitmapFromCache(
            context.resources, R.drawable.sad_sheep,
            512, 512
        )

    }

    override fun onStateEnd() {
        darkSheepSprite = null
    }

    override fun update(deltaTime: Float) {

    }

    override fun render(canvas: Canvas) {

        RenderUtilities.drawTiledBackground(canvas, grassTexture!!)
        canvas.drawARGB(96, 32, 32, 32)

        val disconnectedMessage = context.getString(R.string.disconnected)

        val cdx = (canvas.width / 2f) - (paint.measureText(disconnectedMessage) / 2f)
        val cdy = (canvas.height / 2f) - (disconnectedMessage.length / 2f)

        canvas.drawText(disconnectedMessage, cdx, cdy, paint)

        val sx = (canvas.width / 2f) - (darkSheepSprite!!.width / 2f)
        val sy = (canvas.height / 2f) - (darkSheepSprite!!.height / 2f) + 128
        transformMatrix.setTranslate(sx, sy)

        canvas.drawBitmap(darkSheepSprite!!, transformMatrix, null)

    }

}