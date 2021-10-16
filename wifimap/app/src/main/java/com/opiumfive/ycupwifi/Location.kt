package com.opiumfive.ycupwifi

import com.google.ar.sceneform.math.Vector3

data class Location(val x: Float, val y: Float) {
    companion object {
        fun fromVector(vec: Vector3) = Location(vec.x, vec.z)
    }
}