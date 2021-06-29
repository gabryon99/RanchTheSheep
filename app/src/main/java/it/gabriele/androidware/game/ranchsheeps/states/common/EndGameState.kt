package it.gabriele.androidware.game.ranchsheeps.states.common

import android.content.Context
import android.graphics.*
import android.os.Bundle
import it.gabriele.androidware.R
import it.gabriele.androidware.activities.GameActivity
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.engine.network.NetworkManager
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.ranchsheeps.game.Sheep

class EndGameState(
    networkManager: NetworkManager,
    audioManager: AudioManager,
    gameStateManager: GameStateManager,
    context: Context
) : GameState(networkManager, audioManager, gameStateManager, context)  {

    companion object {
        const val BUNDLE_COLOR_WINNER = "EndGameState.BUNDLE_COLOR_WINNER"
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
    }
    private val transformMatrix = Matrix()

    private lateinit var endGameMessage: String
    private lateinit var sheepSprite: Bitmap

    override fun onStateStart(oldState: Bundle?, args: Bundle?) {

        if (args != null) {

            val color = args.getByte(BUNDLE_COLOR_WINNER)
            val drawableId = if (color == Sheep.WHITE_TYPE) R.drawable.my_sheep else R.drawable.my_dark_sheep
            val playerColor = if (networkManager.isGameMaster) Sheep.WHITE_TYPE else Sheep.DARK_TYPE

            sheepSprite = RenderUtilities.getBitmapFromCache(context.resources, drawableId, 256, 256)

            if (color == playerColor) {
                endGameMessage = context.getString(R.string.you_won)
                audioManager.loadSound(R.raw.win_sfx)
                audioManager.playSound(R.raw.win_sfx)
            } else {
                endGameMessage = context.getString(R.string.you_lose)
                audioManager.loadSound(R.raw.lose_sfx)
                audioManager.playSound(R.raw.lose_sfx)
            }

        }

        /* Tell to the activity to end the game! */
        if (context is GameActivity) {
            context.endGame()
        }
    }

    override fun onStateEnd() {
        audioManager.releaseMusicPlayer(R.raw.bg_music)
    }

    override fun render(canvas: Canvas) {

        canvas.drawARGB(255, 0, 0, 0)

        val cx = (canvas.width / 2f) - (paint.measureText(endGameMessage) / 2f)
        val cy = (canvas.height / 2f) - (endGameMessage.length / 2f)

        canvas.drawText(endGameMessage, cx, cy, paint)

        /* Draw the winner sheep */
        val sx = (canvas.width / 2f) - (sheepSprite.width / 2f)
        val sy = (canvas.height / 2f) - (sheepSprite.height / 2f) + 128
        transformMatrix.setTranslate(sx, sy)

        canvas.drawBitmap(sheepSprite, transformMatrix, null)
    }

}