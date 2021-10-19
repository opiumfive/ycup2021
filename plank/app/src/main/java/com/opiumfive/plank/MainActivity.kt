package com.opiumfive.plank

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.opiumfive.plank.camera.CameraSource
import com.opiumfive.plank.data.Device
import com.opiumfive.plank.list.Ex
import com.opiumfive.plank.ml.MoveNet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    private lateinit var surfaceView: SurfaceView
    private var modelPos = 0

    /** Default device is GPU */
    private var device = Device.GPU

    var soundPool: SoundPool? = null
    var soundPoolMap: HashMap<Int, Int>? = null
    var soundID = 1
    var detectedCount = 0
    var undetectedCount = 0
    var startedTracking = false
    var startedTrackingDate: Date? = null

    private lateinit var tvScore: TextView
    private lateinit var tvFPS: TextView
    private var cameraSource: CameraSource? = null
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }
    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            changeModel(position)
        }
    }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)
        tvFPS = findViewById(R.id.tvFps)
        surfaceView = findViewById(R.id.surfaceView)

        soundPool = SoundPool(4, AudioManager.STREAM_MUSIC, 100)
        soundPoolMap = HashMap()
        soundPoolMap!![soundID] = soundPool!!.load(this, R.raw.ok, 1)

        if (!isCameraPermissionGranted()) {
            requestPermission()
        }
    }

    fun playSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        val leftVolume = curVolume / maxVolume
        val rightVolume = curVolume / maxVolume
        val priority = 1
        val no_loop = 0
        val normal_playback_rate = 1f
        soundPool?.play(soundID, leftVolume, rightVolume, priority, no_loop, normal_playback_rate)
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?,
                            inPlank: Boolean
                        ) {
                            if (inPlank) detectedCount++

                            if (detectedCount > 10 && !startedTracking) {
                                playSound()
                                startedTracking = true
                                startedTrackingDate = Date()
                            }

                            if (!inPlank && startedTracking) {
                                undetectedCount++
                                if (undetectedCount > 10) {
                                    playSound()
                                    if (startedTrackingDate != null) {
                                        val ex = Ex("Планка", startedTrackingDate!!, Date())
                                        setResult(RESULT_OK, Intent().apply { putExtra("item", ex) })
                                    } else {
                                        setResult(RESULT_CANCELED)
                                    }
                                    finish()

                                }
                            } else if (!inPlank) {
                                detectedCount = 0
                                undetectedCount = 0
                            }
                            if (startedTracking && startedTrackingDate != null) {
                                val dur = (Date().time - startedTrackingDate!!.time) / 1000
                                tvScore.text = "Длительность $dur"
                            } else {
                                tvScore.text = "Пока нет данных.. встаньте в планку"
                            }


                        }

                    }).apply {
                        prepareCamera()
                    }
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    // change model when app is running
    private fun changeModel(position: Int) {
        if (modelPos == position) return
        modelPos = position
        createPoseEstimator()
    }

    // change device type when app is running
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device)
        cameraSource?.setDetector(poseDetector)
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
