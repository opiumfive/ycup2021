package com.opiumfive.plank.ml

import android.graphics.Bitmap
import com.opiumfive.plank.data.Person

interface PoseDetector : AutoCloseable {

    fun estimateSinglePose(bitmap: Bitmap): Person

    fun lastInferenceTimeNanos(): Long
}
