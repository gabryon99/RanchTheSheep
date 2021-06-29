package it.gabriele.androidware.game.engine.render

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import android.util.LruCache
import androidx.annotation.DrawableRes
import it.gabriele.androidware.game.engine.GameContext

object RenderUtilities {

    const val TAG = "[Game]::RenderUtilities"

    private val MAX_MEMORY = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    /* Lets use 1/8 of the max memory available to store bitmaps */
    private val CACHE_BITMAP = object : LruCache<@DrawableRes Int, Bitmap>(MAX_MEMORY / 8) {
        override fun sizeOf(key: Int?, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getBitmapFromCache(res: Resources, @DrawableRes bitmapId: Int, width: Int = 0, height: Int = 0): Bitmap {

        val bitmap: Bitmap? = CACHE_BITMAP[bitmapId]

        /* Is the bitmap already loaded inside the cache? */
        if (bitmap != null) {
            if (bitmap.width == width && bitmap.height == height) {
                return bitmap
            }
        }

        /* If not then load it from the resources... */
        var newBitmap: Bitmap = BitmapFactory.decodeResource(res, bitmapId)

        /* ... and if custom dimensions were set then scale the loaded bitmap ...  */
        if (width != 0 && height != 0) {
            newBitmap = Bitmap.createScaledBitmap(newBitmap, width, height, true)
        }

        /* ... finally, save the loaded bitmap inside the LRU Cache. */
        CACHE_BITMAP.put(bitmapId, newBitmap)

        return newBitmap
    }

    /***
     * Draw the texture inside the canvas.
     */
    fun drawTiledBackground(canvas: Canvas, texture: Bitmap) {

        val textureMatrix = Matrix()

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