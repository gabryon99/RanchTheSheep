package it.gabriele.androidware.game.engine.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix

object RenderUtilities {

    private val textureMatrix = Matrix()

    fun drawTiledBackground(canvas: Canvas, texture: Bitmap) {

        val canvasWidth = canvas.width
        val canvasHeight = canvas.height

        val textureWidth = texture.width
        val textureHeight = texture.height

        val tilesInWidth = (canvasWidth / textureWidth)
        val tilesInHeight = (canvasHeight / textureHeight)

        for (y in 0..tilesInHeight) {
            for (x in 0..tilesInWidth) {
                textureMatrix.setTranslate(
                    (x * textureWidth).toFloat(),
                    (y * textureHeight).toFloat()
                )
                canvas.drawBitmap(texture, textureMatrix, null)
            }
        }
    }

}