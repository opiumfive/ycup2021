package com.opiumfive.ycupwars

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.android.material.math.MathUtils.floorMod
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion
import kotlin.math.abs

enum class PiuState {HIT, MISS, NONE}

class OverlayView @JvmOverloads constructor(context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePaint: Paint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val textPaint: Paint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 30f
    }

    private val points = mutableListOf<PPoint>()
    private var hittingNow: PPoint? = null
    private var counter = 0
    private var isPiu = PiuState.NONE

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (isPiu) {
            PiuState.HIT -> canvas.drawColor(0x7700FF00)
            PiuState.MISS -> canvas.drawColor(0x77FF0000)
            else -> canvas.drawColor(0x33000000)
        }
        var hitted = false
        points.forEach {
            if (!hitted && it.p.x >= width * 0.45 && it.p.x <= width * 0.55 && it.p.y >= height * 0.45 && it.p.y <= height * 0.55) {
                hittingNow = it
                hitted = true
            }
            canvas.drawCircle(it.p.x, it.p.y, 50f, wavePaint)
            canvas.drawText(it.name, it.p.x, it.p.y, textPaint)
        }

        if (!hitted) {
            hittingNow = null
        }

        //canvas.drawText(points.toString(), 10f, 30f, textPaint)
    }

    fun setData(angle: Quaternion, myData: UserData, others: List<UserData>) {

        val angles: DoubleArray = angle.toEulerAngles()
        val tilt1 = Math.toDegrees(angles[0])
        val tilt2 = Math.toDegrees(angles[2])

        val pixelsPerDegree = width / 30f

        points.clear()
        points.addAll(others.filter { it.alive == true }.map {
            val bearing = Maths.angleFromCoordinate3(
                myData.lat ?: 0.0,
                myData.lng ?: 0.0,
                it.lat ?: 0.0,
                it.lng ?: 0.0
            )
            val diffAngle = Maths.angleDiff(tilt1.toFloat(), bearing.toFloat())
            val p = PointF()
            p.x = -diffAngle * pixelsPerDegree
            var deg = (floorMod((tilt2.toFloat() - 90), 180))
            if (deg > 90) deg -= 180
            p.y = height / 2f + deg * pixelsPerDegree //height / 2f//
            if (counter > 10) {
                counter = 0
                Log.d("mylog", "my = $deg")
            }
            PPoint(p, it.name ?: "", it.uid ?: "", "n = ${it.name}, t1 = $tilt1, b = $bearing, d = $diffAngle")
        })

        counter ++

        invalidate()
    }

    fun piu(state: PiuState? = null): PPoint? {
        var st: PiuState? = null
        if (state == null) {
            if (hittingNow != null) {
                st = PiuState.HIT
            } else {
                st = PiuState.MISS
            }
        } else {
            st = state
        }
        if (isPiu == PiuState.NONE && st != null) {
            isPiu = st
            invalidate()
            postDelayed({
                isPiu = PiuState.NONE
                postInvalidateOnAnimation()
            }, 125)
        }
        return hittingNow
    }
}

data class PPoint(val p: PointF, val name: String, val uid: String, val log: String) {
    override fun toString(): String {
        return log
    }
}