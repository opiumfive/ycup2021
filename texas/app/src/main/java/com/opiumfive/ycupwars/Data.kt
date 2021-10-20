package com.opiumfive.ycupwars

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Data(
    @PrimaryKey(autoGenerate = true) var uid: Int? = null,
    @ColumnInfo(name = "type") var type: String? = null,
    @ColumnInfo(name = "count") var count: Int? = null,
    @ColumnInfo(name = "lat") var lat: Double? = null,
    @ColumnInfo(name = "lng") var lng: Double? = null
)