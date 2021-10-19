package com.opiumfive.plank.list

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Ex(val name: String, val start: Date, val end: Date) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        Date(parcel.readLong()),
        Date(parcel.readLong())
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeLong(start.time)
        parcel.writeLong(end.time)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Ex> {
        override fun createFromParcel(parcel: Parcel): Ex {
            return Ex(parcel)
        }

        override fun newArray(size: Int): Array<Ex?> {
            return arrayOfNulls(size)
        }
    }
}