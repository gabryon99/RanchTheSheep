package it.gabriele.androidware.game.ranchsheeps.game

import android.os.Parcel
import android.os.Parcelable
import it.gabriele.androidware.game.math.Vector2

/***
 * Size: 1bytes + 8bytes + 8bytes: 17bytes
 */
data class Sheep(
    val color: Byte = WHITE_TYPE,
    var visible: Byte = VISIBLE,
    val position: Vector2? = Vector2(0f, 0f),
    val velocity: Vector2? = Vector2(0f, 0f)
) : Parcelable {

    companion object CREATOR : Parcelable.Creator<Sheep> {

        const val WHITE_TYPE =  (0x00000000).toByte()
        const val DARK_TYPE =   (0x00000001).toByte()

        const val VISIBLE =     (0x00000001).toByte()
        const val INVISIBLE =   (0x00000000).toByte()

        const val SIZE_BYTES = ((Byte.SIZE_BYTES * 2) + (Vector2.SIZE_BYTES * 2)) /* Bytes */

        override fun createFromParcel(parcel: Parcel): Sheep {
            return Sheep(parcel)
        }

        override fun newArray(size: Int): Array<Sheep?> {
            return arrayOfNulls(size)
        }
    }


    constructor(parcel: Parcel) : this(
        parcel.readByte(),
        parcel.readByte(),
        parcel.readParcelable(Vector2::class.java.classLoader),
        parcel.readParcelable(Vector2::class.java.classLoader)
    ) {
    }

    override fun toString(): String {
        val color = if (color == WHITE_TYPE) "White" else "Dark"
        return "Sheep {${color}, $position, $velocity}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(color)
        parcel.writeParcelable(position, flags)
        parcel.writeParcelable(velocity, flags)
    }

    override fun describeContents(): Int {
        return 0
    }


}