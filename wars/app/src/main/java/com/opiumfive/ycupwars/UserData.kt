package com.opiumfive.ycupwars

data class UserData(
    var uid: String? = null,
    var name: String? = null,
    var online: Boolean? = null,
    var lat: Double? = null,
    var lng: Double? = null,
    var alive: Boolean? = null,
    var pts: Int? = 0,
    var killedBy: String? = null
)