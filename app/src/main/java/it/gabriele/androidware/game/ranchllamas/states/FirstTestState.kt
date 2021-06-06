package it.gabriele.androidware.game.ranchllamas.states

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import it.gabriele.androidware.R
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.ranchllamas.LLamaGame

class FirstTestState(
    gameStateManager: GameStateManager,
    context: Context
): GameState(gameStateManager, context) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 64f
    }

    private var grassTexture: Bitmap? = null

    private var elapsedTime = 0f

    override fun onStateStart(oldState: Bundle?) {

        if (oldState != null) {
            elapsedTime = oldState.getFloat("__elapsed_time", elapsedTime)
            Log.d("LoadingScreen", "onStateStart: $elapsedTime seconds passed...")
        }

        grassTexture = GameContext.getBitmapFromCache(context.resources, R.drawable.grass, 512, 512)
    }

    override fun onStateSave(state: Bundle) {
        state.putFloat("__elapsed_time", elapsedTime)
    }

    override fun onStateEnd() {
        grassTexture = null
    }

    override fun update(deltaTime: Float) {

        elapsedTime += deltaTime

        if (elapsedTime >= 10f) {
            mGameStateManager.switch(LLamaGame.TEST)
        }
    }

    override fun render(canvas: Canvas) {

        RenderUtilities.drawTiledBackground(canvas, grassTexture!!)

        // draw a black layer
        canvas.drawARGB(200, 0, 0, 0)

        // waiting for player
        val text = "Loading..."

        val cx = ((canvas.width - textPaint.measureText(text)) / 2f)
        val ch = ((canvas.height - textPaint.textSize) / 2f)

        canvas.drawText(text, cx, ch, textPaint)
    }

}