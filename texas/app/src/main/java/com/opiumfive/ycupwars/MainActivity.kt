package com.opiumfive.ycupwars


import android.content.Context
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.work.*
import com.birjuvachhani.locus.*
import com.fxn.cue.Cue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mcdev.quantitizerlibrary.HorizontalQuantitizer
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion
import java.lang.Exception
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var name: String? = null
    val enemyMarkers = mutableListOf<Marker>()
    var text: TextView? = null
    var tiltProvider: TiltProvider? = null
    var mMap: GoogleMap? = null
    var myMarker: Marker? = null
    var declination: Double = 0.0
    var location: Location? = null
    var currentAngle: Quaternion? = null
    var fab: FloatingActionButton? = null
    var db: AppDatabase? = null
    var tempType: Type? = null
    var tempQuantity: Int? = null
    var emailView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mydb"
        ).build()

        text = findViewById(R.id.text)

        Locus.startLocationUpdates(this) { result ->
            result.location?.let {
                location = it

                val geomagneticField = GeomagneticField(
                    it.latitude.toFloat(),
                    it.longitude.toFloat(),
                    it.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                declination = Math.toRadians(geomagneticField.declination.toDouble())

                position()
            }
            result.error?.let {
                when {
                    it.isDenied -> { /* Permission denied */ }
                    it.isPermanentlyDenied -> { /* Permission is permanently denied */ }
                    it.isFatal -> { /* Something else went wrong! */ }
                    it.isSettingsDenied -> { /* Settings resolution denied by the user */ }
                    it.isSettingsResolutionFailed -> { /* Settings resolution failed! */ }
                }
            }
        }

        tiltProvider = TiltProvider(this)
        tiltProvider?.start { angle ->

            runOnUiThread {
                currentAngle = angle
                //text?.text = "$gameData\n\nMyData = $myData\n\n$currentAngle"

                currentAngle?.let {
                    position()
                }
            }

        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fab = findViewById<FloatingActionButton>(R.id.fab)
        fab?.setOnClickListener {
            val items = Type.values().map { it.nn }.toTypedArray()
            AlertDialog.Builder(this)
                .setSingleChoiceItems(items, 0, null)
                .setPositiveButton("Текущ. место") { dialog, whichButton ->
                    dialog.dismiss()
                    val selectedPosition = (dialog as AlertDialog).listView.checkedItemPosition
                    val type = Type.values()[selectedPosition]

                    showDialogWithQuantity {
                        if (location != null) {
                            addItem(type, it, location!!)
                            toast("Элемент добавлен")
                        } else {
                            toast("Не получено местоположение")
                        }
                    }

                }
                .setNegativeButton("На карте") { dialog, whichButton ->
                    dialog.dismiss()
                    val selectedPosition = (dialog as AlertDialog).listView.checkedItemPosition
                    val type = Type.values()[selectedPosition]
                    tempType = type
                    showDialogWithQuantity {
                        tempQuantity = it
                        toast("Выберите точку на карте")
                    }
                }
                .setNeutralButton("Отмена") { dialog, whichButton ->
                    dialog.dismiss()

                }
                .show()
        }

        findViewById<FloatingActionButton>(R.id.fabList).setOnClickListener {
            thread {
                val list = db?.dataDao()?.getAll()
                if (list.isNullOrEmpty()) {
                    runOnUiThread {
                        toast("Текущий список пуст")
                    }
                } else {
                    runOnUiThread {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Очередь отправки")

                        val animals = list.map { Type.valueOf(it.type!!).nn + " [" + it.count + " шт.] (" + it.lat + " " + it.lng + ")" }.toTypedArray()
                        builder.setItems(animals) { dialog, which ->
                            when (which) {
                                0, 1, 2, 3, 4 -> {
                                }
                            }
                        }
                        val dialog = builder.create()
                        dialog.show()
                    }
                }
            }
        }

        emailView = findViewById(R.id.email)

        val sharedPref = getSharedPreferences("mypfefs", Context.MODE_PRIVATE)
        val curEmail = sharedPref.getString("email", "")
        emailView?.setOnClickListener {
            showDialogWithEmail(curEmail ?: "") {
                emailView?.text = "Email указан"
                saveEmail(it)
            }
        }

        if (curEmail?.isNotEmpty() == true) {
            emailView?.text = "Email указан"
        }

        checkAndShedule()
    }

    fun saveEmail(email: String) {
        val sharedPref = getSharedPreferences("mypfefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("email", email).apply()
    }

    fun checkAndShedule() {
        thread {
            try {
                val d = db?.dataDao()?.getAll()

                if (d?.isNotEmpty() == true) {

                    val uploadWorkRequest: WorkRequest =
                        OneTimeWorkRequest.Builder(UploadWorker::class.java)
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .build()

                    WorkManager
                        .getInstance(this)
                        .enqueue(uploadWorkRequest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

    fun addItem(type: Type, count: Int, location: Location) {
        thread {
            db?.dataDao()?.insert(Data(null, type.name, count, location.latitude, location.longitude))
        }

        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        WorkManager
            .getInstance(this)
            .enqueue(uploadWorkRequest)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setOnMapClickListener {
            if (tempQuantity != null && tempType != null) {
                val loc = Location("loc").apply {
                    latitude = it.latitude
                    longitude = it.longitude
                }
                addItem(tempType!!, tempQuantity!!, loc)
                toast("Элемент добавлен")

                tempQuantity = null
                tempType = null
            } else {
                toast("Возникла ошибка")
                tempQuantity = null
                tempType = null
            }
        }
        fab?.visibility = View.VISIBLE
        position()
    }

    fun position() {
        if (location == null) return
        if (tempQuantity != null) return
        if (location?.latitude != 0.0) {
            val sydney = LatLng(location?.latitude!!, location?.longitude!!)
            if (myMarker == null) {
                myMarker = mMap?.addMarker(MarkerOptions().position(sydney).title("ME"))
                myMarker?.showInfoWindow()
            } else {
                myMarker?.position = sydney
            }

            var bearing = Math.toDegrees(currentAngle?.toEulerAngles()!![0]).toFloat()
            bearing += Math.toDegrees(declination).toFloat()
            if (bearing < 0) bearing += 360;

            val currentPlace = CameraPosition.Builder()
                .target(sydney)
                .bearing(bearing).tilt(65.5f).zoom(18f).build()
            mMap?.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace))
        }
    }

    override fun onStop() {
        Locus.stopLocationUpdates()
        tiltProvider?.stop()
        super.onStop()
    }

    fun showDialogWithEmail(current: String, l: (String) -> Unit) {
        val editText = EditText(this)
        editText.setText(current)
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Введите EMAIL")
            .setMessage("На который будут отправляться данные")
            .setView(editText)
            .setPositiveButton("Применить") { d, _ ->
                if (editText.text.toString().isValidEmail()) {
                    l.invoke(editText.text.toString())
                    hideKbd()
                } else {
                    toast("Нужен валидный email")
                }
            }
            .setNeutralButton("Выйти") { d, _ ->
                d.dismiss()
                hideKbd()
            }.show()
        showKbd()
    }

    fun showDialogWithQuantity(l: (Int) -> Unit) {
        val editText = HorizontalQuantitizer(this)
        editText.value = 1
        editText.minValue = 1
        editText.maxValue = 100
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Введите количество")
            .setMessage("")
            .setView(editText)
            .setPositiveButton("Применить") { d, _ ->
                l.invoke(editText.value)
            }
            .setNeutralButton("Выйти") { d, _ ->
                d.dismiss()
            }.show()
    }


    private fun toast(message: String) {
        Cue.init()
            .with(this)
            .setMessage(message)
            .setType(com.fxn.cue.enums.Type.INFO)
            .setGravity(Gravity.BOTTOM)
            .show()
    }
}