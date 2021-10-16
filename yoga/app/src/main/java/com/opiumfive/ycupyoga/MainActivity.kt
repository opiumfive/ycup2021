/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opiumfive.ycupyoga

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import com.airbnb.lottie.LottieAnimationView
import com.opiumfive.ycupyoga.databinding.ActivityMainBinding
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.text.DateFormat
import java.util.*


class MainActivity : AppCompatActivity() {


  private var audioClassifier: AudioClassifier? = null
  private var audioRecord: AudioRecord? = null
  private var classificationInterval = 50L // how often should classification run in milli-secs
  private lateinit var handler: Handler // background thread handler to run classification
  private var working = false
  private var descText: TextView? = null
  private var listOfB: TextView? = null
  private var list = mutableListOf<Hale>()
  private val haleFilter = HaleFilter()
  private var lottie: LottieAnimationView? = null
  private var started = false
  private var lastDate: Date? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    with(binding) {
      keepScreenOn(true)
      listOfB = desc2
      listOfB?.movementMethod = ScrollingMovementMethod()
      lottie = animationView
      button.setOnClickListener {
        if (button.text == "Начать сеанс") {
          button.text = "Окончить"
          lottie?.visibility = View.VISIBLE
          started = true
          listOfB?.visibility = View.VISIBLE
          send.visibility = View.GONE
          list.clear()
          listOfB?.text = ""
          haleFilter.clear()
          lastDate = null
        } else {
          button.text = "Начать сеанс"
          lottie?.visibility = View.GONE
          started = false
          send.visibility = View.VISIBLE

          // TODO показать результат
        }
      }
      send.setOnClickListener {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "message/rfc822"
        i.putExtra(Intent.EXTRA_EMAIL, arrayOf("example@yandex.ru"))
        i.putExtra(Intent.EXTRA_SUBJECT, "Сеанс")
        var lastDate: Date? = null
        i.putExtra(Intent.EXTRA_TEXT, list.map {
          val timeBetween = if (lastDate == null) "" else ((it.time.time - lastDate!!.time) /1000.0).toString() + " сек. -> "
          lastDate = it.time
          "$timeBetween${if (it.type == State.IN) "Вдох" else "Выдох"}"
        }.joinToString(" -> "))
        try {
          startActivity(Intent.createChooser(i, "Send mail..."))
        } catch (ex: ActivityNotFoundException) {
          Toast.makeText(this@MainActivity, "Нет установленных email клиентов.", Toast.LENGTH_SHORT).show()
        }
      }
    }

    val handlerThread = HandlerThread("backgroundThread")
    handlerThread.start()
    handler = HandlerCompat.createAsync(handlerThread.looper)


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestMicrophonePermission()
    } else {
      startAudioClassification()
    }
  }

  private fun startAudioClassification() {
    if (audioClassifier != null) return;


    val classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
    val audioTensor = classifier.createInputTensorAudio()


    val record = classifier.createAudioRecord()
    record.startRecording()


    val run = object : Runnable {
      override fun run() {
        if (working) return
        working = true
        val startTime = System.currentTimeMillis()

        audioTensor.load(record)
        val output = classifier.classify(audioTensor)

        val finishTime = System.currentTimeMillis()

        Log.d(TAG, "Latency = ${finishTime - startTime}ms")
        working = false

        runOnUiThread {
          var vv = output[0].categories.find { it.label == "Breathing" }!!.score * 100
          var silence = output[0].categories.find { it.label == "Silence" }!!.score * 100

          val res = haleFilter.registerInput(vv, silence)

          if (res != null && started) {
            list.add(res)
            val currentDateTimeString = DateFormat.getDateTimeInstance().format(res.time)
            val timeBetween = if (lastDate == null) "" else ((res.time.time - lastDate!!.time) /1000.0).toString() + " сек. -> "
            lastDate = res.time
            listOfB!!.text = listOfB!!.text.toString() + "\n" + "$timeBetween${if (res.type == State.IN) "Вдох" else "Выдох"}"
          }
        }

        handler.postDelayed(this, classificationInterval)
      }
    }

    handler.post(run)

    audioClassifier = classifier
    audioRecord = record
  }

  private fun stopAudioClassification() {
    handler.removeCallbacksAndMessages(null)
    audioRecord?.stop()
    audioRecord = null
    audioClassifier = null
  }

  override fun onRequestPermissionsResult(
          requestCode: Int,
          permissions: Array<out String>,
          grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_RECORD_AUDIO) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Audio permission granted :)")
        startAudioClassification()
      } else {
        Log.e(TAG, "Audio permission not granted :(")
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private fun requestMicrophonePermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) {
      startAudioClassification()
    } else {
      requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
    }
  }

  private fun keepScreenOn(enable: Boolean) =
    if (enable) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

  companion object {
    const val REQUEST_RECORD_AUDIO = 1337
    private const val TAG = "YCupYoga"
    private const val MODEL_FILE = "yamnet.tflite"
  }
}

data class Hale(val type: State, val time: Date)
enum class State { IN, OUT }