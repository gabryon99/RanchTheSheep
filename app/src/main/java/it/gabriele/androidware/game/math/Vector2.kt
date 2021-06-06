package it.gabriele.androidware.game.math

import android.os.Parcel
import android.os.Parcelable
import kotlin.math.pow
import kotlin.math.sqrt

class Vector2(
    x: Float = 0.0f,
    y: Float = 0.0f
): Parcelable {

    var x: Float = 0.0f
    var y: Float = 0.0f

    constructor(parcel: Parcel) : this() {
        x = parcel.readFloat()
        y = parcel.readFloat()
    }

    init {
        this.x = x
        this.y = y
    }

    fun plus(v: Vector2) {
        this.x += v.x
        this.y += v.y
    }

    fun plus(x: Float, y: Float) {
        this.x += x
        this.y += y
    }

    fun minus(v: Vector2) {
        this.x -= v.x
        this.y -= v.y
    }

    fun minus(x: Float, y: Float) {
        this.x -= x
        this.y -= y
    }

    fun normalize() {
        this.x /= this.x
        this.y /= this.y
    }

    fun magnitude(): Float {
        return sqrt(this.x.pow(2) + this.y.pow(2))
    }

    fun times(n: Float) {
        this.x *= n
        this.y *= n
    }

    fun div(n: Float) {
        this.x /= n
        this.y /= n
    }

    override fun toString(): String {
        return "(x: $x, y: $y)"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeFloat(x)
        dest.writeFloat(y)
    }

    companion object CREATOR : Parcelable.Creator<Vector2> {
        override fun createFromParcel(parcel: Parcel): Vector2 {
            return Vector2(parcel)
        }

        override fun newArray(size: Int): Array<Vector2?> {
            return arrayOfNulls(size)
        }
    }

}