package com.opiumfive.ycupwifi

import android.R.attr.textColor
import android.R.attr.textSize
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture


data class Reading(var strength: Int, var node: TransformableNode?)

// TODO permissions

class MainActivity : AppCompatActivity() {
    private var materialColors: MutableList<Material> = mutableListOf()
    private var materialColorsInt: MutableList<Color> = mutableListOf()

    private val heatMap = SparseArray<SparseArray<Reading>>()

    private var arFragment: ArFragment? = null
    private var started = false

    private val handler = Handler()
    private val viewUpdateTask = Runnable {
        updateAndSchedule()
    }

    private var gridView: GridView? = null
    private val mainData = MainData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        loadData()

        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arFragment?.arSceneView?.scene?.addOnUpdateListener(this::onSceneUpdate)
        arFragment?.setOnTapArPlaneListener(this::onTapArPlaneListener)

        gridView = GridView(this, mainData)
        gridViewFrameLayout.addView(gridView)

        button.setOnClickListener {
            if (!started) {
                button.text = "Закончить"
                wifi_ssid_strength.visibility = View.VISIBLE
                gridViewFrameLayout.visibility = View.VISIBLE
                instructions_view.visibility = View.GONE
                started = true
                val ar = arFragment ?: return@setOnClickListener
                if (anchorNode == null && ar.arSceneView.arFrame.camera.trackingState === TrackingState.TRACKING) {
                    val ar = arFragment ?: return@setOnClickListener
                    val cameraPos = ar.arSceneView.scene.camera.worldPosition


                    val pos = floatArrayOf(cameraPos.x, 0f, cameraPos.z)
                    val rotation = floatArrayOf(0f, 0f, 0f, 1f)
                    val anchor = ar.arSceneView.session.createAnchor(Pose(pos, rotation))
                    anchorNode = AnchorNode(anchor)
                    anchorNode?.setParent(ar.arSceneView.scene)

                    rootNode = Node()
                    rootNode?.setParent(anchorNode)
                }
            } else {
                started = false
                //TODO стоп и построить карту
                gridViewFrameLayout?.layoutParams?.height = 1000
                gridViewFrameLayout?.layoutParams?.width = 1000
                gridView?.isShowNumbers = true
                gridView?.requestLayout()
                button.visibility = View.GONE
            }
        }
    }

    private fun onSceneUpdate(frameTime: FrameTime) {
        val ar = arFragment ?: return

        if (ar.arSceneView.arFrame.camera.trackingState === TrackingState.TRACKING) {
            if (anchorNode == null) {
                instructions_view.setText(R.string.instructions_step_2)
                button.visibility = View.VISIBLE
                instructions_view.visibility = View.VISIBLE
            } else {
                instructions_view.visibility = View.GONE
            }
        } else {
            instructions_view.setText(R.string.instructions_step_1)
            instructions_view.visibility = View.VISIBLE
        }
    }

    private fun loadData() {
        val start = 0xFFFF0000.toInt()
        val end = 0xFF00FF00.toInt()

        val colors = mutableListOf<CompletableFuture<Material>>()
        val max = 20
        for (ii in 0..max) {
            val colorInt = ColorUtils.blendARGB(start, end, (ii.toFloat() / (max - 1).toFloat()))

            val color = Color()
            color.set(colorInt)

            materialColorsInt.add(color)
            val materialFuture = MaterialFactory.makeOpaqueWithColor(this, color)
            colors.add(materialFuture)
        }

        CompletableFuture.allOf(*colors.toTypedArray())
            .handle { _, _ ->

                colors.forEach {
                    materialColors.add(it.get())
                }
            }
    }

    private fun setReading(x: Float, z: Float, strength: Int): Boolean {
        val intX = (x * 10f).toInt()
        val intZ = (z * 10f).toInt()

        var col = heatMap[intZ]
        if (col == null) {
            heatMap.put(intZ, SparseArray())
            col = heatMap[intZ]
        }

        val reading = col[intX]
        val existingRenderable: TransformableNode? = reading?.node

        return if (reading == null || reading.strength != strength) {
            existingRenderable?.setParent(null)

            val renderable = createReadingRenderable(x, z, strength)

            col.put(intX, Reading(strength, renderable))
            true
        } else {
            false
        }
    }

    /*
        private fun getReading(x: Int, y: Int): Int
        {
            val col = heatMap[y]
            return if(col != null)
            {
                col[x] ?: 0
            }
            else
            {
                0
            }
        }
    */
    private fun wifiManager() = getSystemService(Context.WIFI_SERVICE) as WifiManager

    override fun onStart() {
        super.onStart()

        updateAndSchedule()
    }

    override fun onStop() {
        super.onStop()

        handler.removeCallbacks(viewUpdateTask)
    }

    private fun createBlock(level: Int): ModelRenderable? {
        val signalStrength = WifiManager.calculateSignalLevel(level, MAX_SIGNAL_STRENGTH)
        val scaleFactor = signalStrength.toFloat() / MAX_SIGNAL_STRENGTH.toFloat()
        //val scaleFactor = WifiManager.calculateSignalLevel(level, 100)
        //val normalizedLevel = (level * -1) - 50
        //val scaleFactor = 1f - (normalizedLevel.toFloat() / 46f)

        Log.d("adam", "scaleFactor: $scaleFactor level: $level")

        val height = 0.25f //* scaleFactor
        val size = 0.05f

        val material = getBarColor(scaleFactor)

        return ShapeFactory.makeSphere(
            size,
            Vector3(0f, height, 0f),
            material
        )
    }

    fun logscale(value: Float): Float {
        return if (value > 1.0f)
            Math.log(value.toDouble()).toFloat()
        else
            value - 1.0f
    }

    private fun getBarColor(ratio: Float): Material {
        val colorIndex = ((materialColors.size - 1) * ratio).toInt()
        Log.d("adam", "colorIndex: $colorIndex ratio: $ratio")
        return materialColors[colorIndex]
    }

    private fun getBarColor2(level: Int, ratio: Float, listener: (Material) -> Unit) {
        val builder = Texture.builder()
        val colorIndex = ((materialColors.size - 1) * ratio).toInt()
        val matColor = materialColorsInt[colorIndex]


        val text = level.toString()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 10f
        paint.color = android.graphics.Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        val baseline: Float = -paint.ascent() // ascent() is negative
        val image = Bitmap.createBitmap((paint.measureText(text) + 0.5f).toInt(), (baseline + paint.descent() + 0.5f).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, 0f, baseline, paint)

        matColor.g
        builder.setSource(image)
        builder.build().thenAccept {
            MaterialFactory.makeOpaqueWithTexture(this, it).thenAccept {
                listener.invoke(it)
            }
        }
    }

    private var anchorNode: AnchorNode? = null
    private var rootNode: Node? = null
    private var lastLocation: Vector3? = null

    private fun onTapArPlaneListener(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        val ar = arFragment ?: return
    }

    private fun createReadingRenderable(x: Float, z: Float, strength: Int): TransformableNode? {
        val ar = arFragment ?: return null
        rootNode ?: return null

        // Create the transformable sphere and add it to the anchor
        //val base = Node()
        val node = TransformableNode(ar.transformationSystem)
        node.setParent(rootNode)

        val posX = x
        val posY = 0f
        val posZ = z

        //sphere.localPosition = sphere.worldToLocalPoint(Vector3(posX, posY, posZ))
        node.localPosition = Vector3(posX, posY, posZ)
        node.renderable = createBlock(strength)

        return node
    }

    private fun getGridPosition(): Vector3 {
        val ar = arFragment ?: return Vector3()

        val cameraPos = ar.arSceneView.scene.camera.worldPosition

        info.text = cameraPos.toString()

        var x = roundToHalf(cameraPos.x.toDouble())
        var z = roundToHalf(cameraPos.z.toDouble())
        return Vector3(x.toFloat(), 10f, z.toFloat())
    }

    fun roundToHalf(d: Double): Double = Math.round(d * 4) / 4.0

    private val MAX_SIGNAL_STRENGTH = 300

    private fun getSignalStrength(): Int {
        val wifiInfo = wifiManager().connectionInfo
        return wifiInfo.rssi
    }

    private fun updateAndSchedule() {
        updateViews()
        handler.postDelayed(viewUpdateTask, 250)
    }

    private fun updateViews() {
        if (wifiManager().connectionInfo != null) {
            val strength = getSignalStrength()

            val gridPosition = getGridPosition()

            if (started) {
                lastLocation = getGridPosition()

                lastLocation?.let {
                    if (!mainData.isStarted) mainData.startMeasurement(Location.fromVector(it))
                    mainData.addMeasurement(Location.fromVector(it), strength)
                    gridView?.update(Location.fromVector(it))

                }
            }

            setReading(gridPosition.x, gridPosition.z, strength)
            wifi_ssid_strength.text = getString(R.string.signal_strength, strength)
        } else {
            wifi_ssid_strength.text = ""
        }

        if (wifiManager().connectionInfo != null && rootNode != null) {
            //wifi_found_view.visibility = View.VISIBLE
        } else {
            //wifi_found_view.visibility = View.GONE
        }
    }

    companion object {
        const val TAG = "main"
    }

    private val wifi: WifiManager
        get() = getSystemService(Context.WIFI_SERVICE) as WifiManager
}
