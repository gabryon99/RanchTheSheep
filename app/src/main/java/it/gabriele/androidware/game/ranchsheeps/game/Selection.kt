package it.gabriele.androidware.game.ranchsheeps.game

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

data class Selection(
    val color: Int = Color.RED,
    var x1: Float = 0f,
    var y1: Float = 0f,
    var x2: Float = 0f,
    var y2: Float = 0f
) {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isDither = true
        strokeWidth = 12f
    }

    val path = Path()

    val rect = RectF()

    fun setStartingPoints(x: Float, y: Float) {
        this.x1 = x
        this.y1 = y
    }

    fun setEndingPoint(x: Float, y: Float) {
        this.x2 = x
        this.y2 = y
    }

    fun clear() {
        paint.style = Paint.Style.STROKE
        paint.color = color
        path.reset()
    }

    private fun setRect() {

        var left = 0f
        var right = 0f
        var bottom = 0f
        var top = 0f

        // from top right to bottom left
        if (y2 > y1 && x2 > x1) {
            left = x1; top = y1
            right = x2; bottom = y2
        }
        // from top left to bottom right
        else if (y2 > y1 && x2 < x1) {
            left = x2; top = y1
            right = x1; bottom = y2
        }
        // from bottom right to top left
        else if (y2 < y1 && x2 > x1) {
            left = x1; top = y2
            right = x2; bottom = y1
        }
        // from bottom right to top left
        else {
            left = x2; top = y2
            right = x1; bottom = y1
        }

        rect.left = left
        rect.right = right
        rect.bottom = bottom
        rect.top = top

    }

    fun make() {
        setRect()
        path.addRect(rect, Path.Direction.CW)
    }

    override fun toString(): String {
        return "($x1, $y1, $x2, $y2)"
    }

}