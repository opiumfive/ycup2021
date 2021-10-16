package com.opiumfive.ycupyoga

import java.text.DateFormat
import java.util.*

class HaleFilter {

    private var yMinFilterBuff = 0.0
    private val filterFactor = 0.1
    private val filterNoise = 25
    private var target = 0.0
    private var state = State.OUT
    private var canChangeState = false
    private var counter = 0

    fun clear() {
        state = State.OUT
        canChangeState = false
        counter = 0
    }

    fun registerInput(haleScore: Float, silenceScore: Float): Hale? {
        val yMinFilterPrediction = yMinFilterBuff + filterFactor
        val factor = (yMinFilterPrediction / (yMinFilterPrediction + filterNoise)).toInt()
        target = haleScore + factor * (target - haleScore)
        yMinFilterBuff = (1f - factor) * yMinFilterPrediction

        if (target >= 40) {
            // TODO counter for input? test on some devices
            if (canChangeState) {
                canChangeState = false
                if (state != State.IN) {
                    state = State.IN
                    return Hale(State.IN, Date())
                } else {
                    state = State.OUT
                    return Hale(State.OUT, Date())
                }
            }
        } else if (target < 6) {
            if (++counter > 10) {
                canChangeState = true
                counter = 0
            }
        }
        return null
    }
}