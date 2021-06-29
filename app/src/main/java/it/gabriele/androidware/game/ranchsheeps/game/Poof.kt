package it.gabriele.androidware.game.ranchsheeps.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.math.Vector2

data class Poof(
    val position: Vector2,
    val sprite: Bitmap
) {

    companion object {
        private const val FRAMES_SPEED = 8
        private const val FRAMES_ON_X_AXIS = 5
        private const val FRAMES_ON_Y_AXIS = 5
        private const val UPPER_BOUND = (GameContext.MAX_FPS / FRAMES_SPEED)
    }

    private var currentFrameX = 0
    private var currentFrameY = 0
    private var framesCounter = 0

    private val widthSlice = sprite.width / 5
    private val heightSlice = sprite.height / 5

    private val frameRec = Rect()

    var visible = false

    fun update(deltaTime: Float) {

        framesCounter++

        if (framesCounter >= UPPER_BOUND) {

            framesCounter = 0

            currentFrameX++

            if (currentFrameX > FRAMES_ON_X_AXIS) {
                currentFrameX = 0
                currentFrameY++
            }
            if (currentFrameY > FRAMES_ON_Y_AXIS) {
                currentFrameX = 0
                currentFrameY = 0

                // the animation ended
                visible = false
            }

            // left=x1: currentFrameX * widthSlice, top=y1: currentFrameY * heightSlice
            // right=x2: widthSlice * (currentFrameX + 1), bottom=y2: heightSlice * (currentFrameY + 1)

            frameRec.left = currentFrameX * widthSlice
            frameRec.top = currentFrameY * heightSlice
            frameRec.right = widthSlice * (currentFrameX + 1)
            frameRec.bottom = heightSlice * (currentFrameY + 1)
        }


    }

    fun reset() {
        framesCounter = 0
        currentFrameY = 0
        currentFrameX = 0
        visible = true
    }

    fun render(canvas: Canvas) {
        if (visible) {
            val finalPos = RectF(position.x, position.y, position.x + frameRec.width(), position.y + frameRec.height())
            canvas.drawBitmap(sprite, frameRec, finalPos, null)
        }
    }

}