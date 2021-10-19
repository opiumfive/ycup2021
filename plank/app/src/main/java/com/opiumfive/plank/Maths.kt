package com.opiumfive.plank

import android.graphics.PointF
import android.util.Log
import com.opiumfive.plank.data.BodyPart
import com.opiumfive.plank.data.Person
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class Maths {
    companion object {
        fun angle(a: PointF, b: PointF, c: PointF): Double {
            var ab: Double = sqrt((a.x - b.x).toDouble().pow(2.0) + (a.y - b.y).toDouble().pow(2.0))
            var ac: Double = sqrt((a.x - c.x).toDouble().pow(2.0) + (a.y - c.y).toDouble().pow(2.0))
            var bc: Double = sqrt((b.x - c.x).toDouble().pow(2.0) + (b.y - c.y).toDouble().pow(2.0))

            var cosValue = (ab * ab + bc * bc - ac * ac) / (2 * bc * ab)

            return acos(cosValue) * (180 / Math.PI)
        }

        fun inPlank(person: Person): Boolean {
            val ankle = person.keyPoints.find { (it.bodyPart == BodyPart.LEFT_ANKLE || it.bodyPart == BodyPart.RIGHT_ANKLE) && it.score > 0.5 }?.coordinate
            val knee = person.keyPoints.find { (it.bodyPart == BodyPart.LEFT_KNEE || it.bodyPart == BodyPart.RIGHT_KNEE) && it.score > 0.5 }?.coordinate
            val hip = person.keyPoints.find { (it.bodyPart == BodyPart.LEFT_HIP || it.bodyPart == BodyPart.RIGHT_HIP) && it.score > 0.5 }?.coordinate
            val shoulder = person.keyPoints.find { (it.bodyPart == BodyPart.LEFT_SHOULDER || it.bodyPart == BodyPart.RIGHT_SHOULDER) && it.score > 0.5 }?.coordinate
            val elbow = person.keyPoints.find { (it.bodyPart == BodyPart.LEFT_ELBOW || it.bodyPart == BodyPart.RIGHT_ELBOW) && it.score > 0.5 }?.coordinate
            var inPlank = false
            if (ankle != null && shoulder != null && knee != null && hip != null && elbow != null) {
                val angleHNA = angle(hip, knee, ankle)
                val angleSHN = angle(shoulder, hip, knee)
                val angleESH = angle(elbow, shoulder, hip)
                val angleSAX = angle(shoulder, ankle, elbow)

                val goodHNA = angleHNA in 155.0..180.0 || angleHNA in 0.0..20.0
                val goodSHN = angleSHN in 155.0..180.0 || angleSHN in 0.0..20.0
                val goodESH = angleESH in 35.0..115.0
                val goodSAX = angleSAX in 2.0..30.0

                inPlank = goodHNA && goodSHN && goodESH && goodSAX

                // HNA <= 15, SHN <=15, ESH 75-105, SAX <=15
                // HNA = 173.21613187255974, SHN = 163.96072460527915, ESH = 83.22569979090369, SAX = 7.009966735785659
                //в планке

                // HNA = 178.60112506176242, SHN = 173.89020135612503, ESH = 12.035738469412339, SAX = 86.92996759123793
                // просто стоит
                Log.e("PERSON", "inPlank = $inPlank, HNA = $angleHNA, SHN = $angleSHN, ESH = $angleESH, SAX = $angleSAX")
            } else {
                Log.e("PERSON", "BAD STATE")
            }
            return inPlank
        }
    }
}