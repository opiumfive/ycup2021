package com.opiumfive.ycupwars

import android.content.Context
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.birjuvachhani.locus.*
import com.fxn.cue.Cue
import com.fxn.cue.enums.Type
import com.github.javafaker.Faker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion
import java.util.*
import kotlin.math.max
import kotlin.math.min


// Odin object for everything
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var isDialogKilledShowing = false
    var userId: String? = null
    var name: String? = null
    var db: FirebaseDatabase? = null
    var myData: UserData? = null
    val gameData = mutableListOf<UserData>()
    val enemyMarkers = mutableListOf<Marker>()
    var text: TextView? = null
    var overlayView: OverlayView? = null
    var tiltProvider: TiltProvider? = null
    var currentAngle: Quaternion? = null
    var cameraHelper: CameraHelper? = null
    var mMap: GoogleMap? = null
    var myMarker: Marker? = null
    var declination: Double = 0.0

    val eventListener = object: ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            gameData.clear()
            var foundMe = false
            for (matchSnapShot in snapshot.children) {
                val match: UserData? = matchSnapShot.getValue(UserData::class.java)
                if (match != null) {
                    if (matchSnapShot.key == userId) {
                        foundMe = true
                        if (myData == null) {
                            myData = match
                            myData?.alive = true
                            myData?.online = true
                            toast("Вы в игре!")
                            syncMyData()
                            position()
                        } else {
                            myData = match

                            if (myData?.alive == false) {
                                if (isDialogKilledShowing == false) {
                                    isDialogKilledShowing = true
                                    val res = gameData.find { it.uid == myData?.killedBy }
                                    val whoKilled = if (res != null) "В вас попал ${res.name}" else "В вас попали"
                                    showDialogKilled(whoKilled)
                                }
                            } else {
                                position()
                            }
                        }

                    } else {
                        gameData.add(match)
                    }
                }
            }

            if (!foundMe && !name.isNullOrEmpty()) {
                myData = UserData(userId, name, true, 0.0, 0.0, true)
                toast("Вы в игре!")
                syncMyData()
            }

            if (myData != null) {
                var ind = 1
                val tlist = mutableListOf<UserData>()
                tlist.addAll(gameData)
                tlist.add(myData!!)
                //TODO выделить победителя
                text?.text = "Рейтинг:\n" + tlist.sortedByDescending { it.pts }.map { "${ind++}. ${it.name} ${if (it.uid == userId) "(ВЫ)" else ""} (${if (it.online == true) "онлайн" else "оффлайн"} ${if (it.online == true) {if (it.alive == true) "жив" else "убит"} else ""}) - ${it.pts} frag ${ if (ind == 2 && (it.pts ?: 0) > 0) "WINNER!" else ""}" }.joinToString("\n")
            }

            onStateChange()
        }

        override fun onCancelled(error: DatabaseError) {
            // reconnect or what?
        }
    }

    fun showDialogKilled(message: String) {
        val time = System.currentTimeMillis()
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Оппа!")
            .setMessage(message)
            .setPositiveButton("Выйти") { d, _ ->
                d.dismiss()
                isDialogKilledShowing = false
                finish()
            }
            .setNegativeButton("Еще!") { d, _ ->
                val delay = System.currentTimeMillis() - time
                val realDelay = min(5000, delay)
                if (realDelay >= 1000) toast("Вы продолжите через ${realDelay / 1000} сек")
                Handler().postDelayed({
                    myData?.alive = true
                    myData?.online = true
                    syncMyData()
                    isDialogKilledShowing = false
                    toast("Вы в игре!")
                }, realDelay)
            }.show()
    }

    fun showDialogWithName(l: (String) -> Unit) {
        val editText = EditText(this)
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Welcome to Войнушка")
            .setMessage("Придумайте себе псевдоним")
            .setView(editText)
            .setPositiveButton("Применить") { d, _ ->
                if (editText.text.toString().isNotEmpty()) {
                    l.invoke(editText.text.toString())
                    hideKbd()
                } else {
                    toast("Имя не может быть пустым")
                }
            }
            .setNeutralButton("Выйти") { d, _ ->
                d.dismiss()
                finish()
            }
            .setNegativeButton("Случайное") { d, _ ->
                val firstName = Faker().name().firstName()
                l.invoke(firstName)
                d.dismiss()
                hideKbd()
            }.show()
        showKbd()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        text = findViewById(R.id.text)
        overlayView = findViewById(R.id.overlay)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)

        if (sharedPref.contains("userId")) {
            userId = sharedPref.getString("userId", "")
            name = sharedPref.getString("name", "")
        } else {
            userId = UUID.randomUUID().toString()

            showDialogWithName {
                name = it
                sharedPref.edit().putString("userId", userId).putString("name", name).apply()

                myData = UserData(userId, name, true, 0.0, 0.0, true)
                syncMyData()
            }
        }

        db = FirebaseDatabase.getInstance("https://ycupwars-default-rtdb.europe-west1.firebasedatabase.app")
        db?.setPersistenceEnabled(true)
        db?.reference?.child("users")?.addValueEventListener(eventListener)

        Locus.startLocationUpdates(this) { result ->
            result.location?.let {
                myData?.lat = it.latitude
                myData?.lng = it.longitude

                val geomagneticField = GeomagneticField(
                    it.latitude.toFloat(),
                    it.longitude.toFloat(),
                    it.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                declination = Math.toRadians(geomagneticField.declination.toDouble())

                syncMyData()
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
                    if (myData != null) {
                        overlayView?.setData(it, myData!!, gameData)
                        position()
                    }
                }
            }

        }

        findViewById<View>(R.id.rootView).setOnClickListener {
            val hittedName = overlayView?.piu()

            if (myData?.alive == true) {

                if (hittedName != null) {
                    toast("Вы попали в ${hittedName.name}")
                    killEnemy(hittedName.uid)
                } else {
                    toast("Промах")
                }
            } else {
                toast("Вы не в игре")
            }

        }

        cameraHelper = CameraHelper(
            owner = this,
            context = this.applicationContext,
            viewFinder = findViewById(R.id.cameraView)
        )

        try {
            cameraHelper?.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        position()
    }

    fun position() {
        if (myData == null) return
        if (myData?.lat != 0.0) {
            val sydney = LatLng(myData?.lat!!, myData?.lng!!)
            if (myMarker == null) {
                myMarker = mMap?.addMarker(MarkerOptions().position(sydney).title("ME"))
                myMarker?.showInfoWindow()
            } else {
                myMarker?.position = sydney
            }

            var bearing = Math.toDegrees(currentAngle?.toEulerAngles()!![0]).toFloat()
            bearing += Math.toDegrees(declination).toFloat()
            if (bearing < 0) bearing += 360;

            if (gameData.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                if (enemyMarkers.isEmpty()) {
                    gameData.forEach {
                        if (it.alive == true) {
                            val marker = mMap?.addMarker(MarkerOptions().position(LatLng(it.lat!!, it.lng!!)).title(it.name))
                            builder.include(LatLng(it.lat!!, it.lng!!))
                            marker?.showInfoWindow()
                            marker?.let { it1 -> enemyMarkers.add(it1) }
                        }
                    }
                } else {
                    enemyMarkers.forEach {
                        val user = gameData.find { u -> u.name == it.title}
                        user?.let { u ->
                            builder.include(LatLng(u.lat!!, u.lng!!))
                            it.position = LatLng(u.lat!!, u.lng!!)
                        }
                    }

                    val iterator = enemyMarkers.iterator()

                    while (iterator.hasNext()) {
                        val item = iterator.next()
                        val user = gameData.find { u -> u.name == item.title}
                        if (user == null || user.alive == false) {
                            item.remove()
                            iterator.remove()
                        }
                    }

                    gameData.forEach {
                        val item = enemyMarkers.find { u -> u.title == it.name }
                        if (item == null && it.alive == true) {
                            val marker = mMap?.addMarker(MarkerOptions().position(LatLng(it.lat!!, it.lng!!)).title(it.name))
                            marker?.showInfoWindow()
                            marker?.let { it1 -> enemyMarkers.add(it1) }
                        }
                    }
                }

                val bounds = try { builder.build() } catch (e: Exception) { null }

                if (bounds != null) {
                    val scale = Location("1").apply {
                        latitude = bounds.southwest.latitude
                        longitude = bounds.southwest.longitude
                    }.distanceTo(Location("2").apply {
                        latitude = bounds.northeast.latitude
                        longitude = bounds.northeast.longitude
                    }) / 1000.0
                    val zoom = (16 - Math.log(scale) / Math.log(2.0)).toFloat()
                }

                val currentPlace = CameraPosition.Builder()
                    .target(sydney)
                    .bearing(bearing).tilt(65.5f).zoom(17f).build()
                mMap?.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace))
            } else {
                val currentPlace = CameraPosition.Builder()
                    .target(sydney)
                    .bearing(bearing).tilt(65.5f).zoom(18f).build()
                mMap?.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace))
            }
            //mMap?.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        db?.reference?.child("users")?.removeEventListener(eventListener)
        Locus.stopLocationUpdates()
        tiltProvider?.stop()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (myData != null) {
            myData?.online = true
            syncMyData()
        }
    }

    override fun onPause() {
        myData?.online = false
        syncMyData()
        super.onPause()
    }

    private fun syncMyData() {
        db?.reference?.child("users")?.child(userId ?: "")?.setValue(myData)
    }

    private fun killEnemy(uid: String) {
        val res = gameData.find { it.uid == uid }
        if (res != null) {
            res.alive = false
            res.killedBy = userId
            db?.reference?.child("users")?.child(uid)?.setValue(res)
        }
        myData?.pts = (myData?.pts ?: 0) + 1
        syncMyData()
    }

    private fun onStateChange() {
        val onlineUsers = gameData.filter { it.online == true }
        val aliveUsers = gameData.filter { it.alive == true }
        val meAlive = myData?.alive == true
    }

    private fun toast(message: String) {
        Cue.init()
            .with(this)
            .setMessage(message)
            .setType(Type.INFO)
            .setGravity(Gravity.BOTTOM or Gravity.LEFT)
            .show()
    }
}