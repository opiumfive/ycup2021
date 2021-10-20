package com.opiumfive.ycupwars

import com.google.gson.annotations.SerializedName

data class SendData(val coordinates: Coordinates, val objects: Objects)

data class Coordinates(val lat: Double, val lon: Double)

data class Objects(@SerializedName("object") val obj: Object)

data class Object(val type: String, val count: Int)