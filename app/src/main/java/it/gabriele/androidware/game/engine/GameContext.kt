package it.gabriele.androidware.game.engine

import android.content.res.Resources
import android.graphics.*
import android.util.LruCache
import androidx.annotation.DrawableRes

object GameContext {

    const val MAX_FPS: Int = 60
    const val NET_PORT: Int = 53127

    private val MAX_MEMORY = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    /* Lets use 1/8 of the max memory available to store bitmaps */
    private val CACHE_BITMAP = object : LruCache<@DrawableRes Int, Bitmap>(MAX_MEMORY / 8) {
        override fun sizeOf(key: Int?, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /* Variable indicating if the host is the Game Master (the Server) */
    var IS_GAME_MASTER = false

    fun getBitmapFromCache(res: Resources, @DrawableRes bitmapId: Int, width: Int = 0, height: Int = 0): Bitmap {

        val bitmap: Bitmap? = CACHE_BITMAP[bitmapId]

        /* Is the bitmap already loaded inside the cache? */
        if (bitmap != null) {
            return bitmap
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

}