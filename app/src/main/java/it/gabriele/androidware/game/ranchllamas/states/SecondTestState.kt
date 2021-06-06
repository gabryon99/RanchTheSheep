package it.gabriele.androidware.game.ranchllamas.states

import android.content.Context
import android.graphics.*
import android.os.Bundle
import it.gabriele.androidware.R
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

class SecondTestState(
    gameStateManager: GameStateManager,
    context: Context
): GameState(gameStateManager, context) {

    private var grassTexture: Bitmap? = null

    private var sheepSprite: Bitmap? = null

    private val sheepPosition = Vector2()
    private val transformMatrix = Matrix()

    override fun onStateStart(oldState: Bundle?) {

        sheepSprite = GameContext.getBitmapFromCache(context.resources, R.drawable.sheep, 128, 128)
        grassTexture = GameContext.getBitmapFromCache(context.resources, R.drawable.grass, 512, 512)

    }

    override fun onStateSave(state: Bundle) {

    }

    override fun onStateEnd() {
        grassTexture = null
    }

    override fun update(deltaTime: Float) {

    }

    override fun render(canvas: Canvas) {

        RenderUtilities.drawTiledBackground(canvas, grassTexture!!)

        transformMatrix.setTranslate(sheepPosition.x, sheepPosition.y)
        canvas.drawBitmap(sheepSprite!!, transformMatrix, null)
    }

}