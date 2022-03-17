// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.umich.argo.arnote

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import edu.umich.argo.arnote.ar.NoteStore.addNoteToStore
import edu.umich.argo.arnote.ar.NoteStore.dumpNote
import edu.umich.argo.arnote.ar.NoteStore.editNote
import edu.umich.argo.arnote.ar.NoteStore.getNote
import edu.umich.argo.arnote.ar.NoteStore.loadNote
import edu.umich.argo.arnote.ar.NoteStore.storeSize
import edu.umich.argo.arnote.ar.PlaceNode
import edu.umich.argo.arnote.ar.PlacesArFragment
import edu.umich.argo.arnote.model.Place
import edu.umich.argo.arnote.model.getPositionVector

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "MainActivity"

    private lateinit var arFragment: PlacesArFragment
    private lateinit var toolbar: Toolbar
    private lateinit var addButton: View
    private lateinit var createLauncher: ActivityResultLauncher<Intent>
    private lateinit var editLauncher: ActivityResultLauncher<Intent>
    private lateinit var listLauncher: ActivityResultLauncher<Intent>

    // Sensor
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var anchorNode: AnchorNode? = null
    private var otherAnchorNodes = mutableListOf<AnchorNode>()
    private var currentLocation: Place? = null

    private var anchorSelected: Boolean = false
    private var newAnchorNode: AnchorNode? = null
    private var currentPlaceNode: PlaceNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isSupportedDevice()) {
            return
        }
        setContentView(R.layout.activity_main)
        getPermission()
        getCurrentLocation()
        createLaunchers()
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as PlacesArFragment
        sensorManager = getSystemService()!!
        toolbar = findViewById(R.id.toolbar)
        initToolbar()
        addButton = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        addButton.setOnClickListener {
            arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
                // Create anchor
                val anchor = hitResult.createAnchor()
                val newAnchorNode = AnchorNode(anchor)
                newAnchorNode?.setParent(arFragment.arSceneView.scene)
                otherAnchorNodes.add(newAnchorNode)
                addNote(newAnchorNode!!)
            }
            it.visibility = INVISIBLE
        }

        loadNote(applicationContext)
        val places = getNote()
        if (places?.size ?: 0 == 0) {
            addNoteToStore(
                Place("0", "note1, balabala", lat=(42.3009473).toString(), lng=(-83.73001909999999).toString()),
            )
            addNoteToStore(
                Place("1", "note2, wt", lat=(42.299268).toString(), lng=(-83.717808).toString())
            )

        }
        setUpAr()
    }

    private fun getPermission() {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach {
                if (!it.value) {
                    Toast.makeText(this, "Location access denied", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun createLaunchers() {
        createLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val message = it.data?.getStringExtra("message")?:""

                currentLocation?.let {

                    val place = Place(storeSize().toString(), message, it.lat, it.lng)
                    addNoteToStore(place)
                    newAnchorNode?.let {
                        val placeNode = PlaceNode(this, place)
                        placeNode.setParent(it)
                        placeNode.localPosition = Vector3(0f, 0f, 0f)
                        placeNode.setOnTapListener { _, _ ->
                            showInfoWindow(place, it)
                        }
                    }
                }
            }
        }
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val message = it.data?.getStringExtra("message")
                currentPlaceNode?.setText(message)
                if (message != null) {
                    editNote(currentPlaceNode?.place, message)
                }
            }
        }
        listLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

        }
    }

    private fun initToolbar() {
        toolbar.title = "ARGo"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_event_note_24)
        toolbar.setNavigationOnClickListener {
            // change to listActivity
            listLauncher.launch(Intent(this, EditActivity::class.java))
        }
        toolbar.inflateMenu(R.menu.mainmenu)
    }


    override fun onResume() {
        super.onResume()
//        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
//            sensorManager.registerListener(
//                this,
//                it,
//                SensorManager.SENSOR_DELAY_NORMAL
//            )
//        }
//        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
//            sensorManager.registerListener(
//                this,
//                it,
//                SensorManager.SENSOR_DELAY_NORMAL
//            )
//        }
        arFragment.setOnTapArPlaneListener(null)
        addButton.visibility = VISIBLE
        if (anchorNode != null) {
            if (!anchorNode?.isTracking!!) {
                setUpAr()
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        sensorManager.unregisterListener(this)
        dumpNote(applicationContext)
    }

    private fun setUpAr() {
        arFragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            val frame = arFragment.arSceneView.arFrame
            if (frame != null) {
                //get the trackables to ensure planes are detected
                if (!anchorSelected) {
                    val planes = frame.getUpdatedTrackables(Plane::class.java).iterator()
                    while(planes.hasNext()) {
                        val plane = planes.next() as Plane

                        //If a plane has been detected & is being tracked by ARCore
                        if (plane.trackingState == TrackingState.TRACKING && !anchorSelected) {
                            // Hide the plane discovery helper animation
                            // arFragment.planeDiscoveryController.hide()
                            val anchor = plane.createAnchor(plane.centerPose)
                            anchorSelected = true
                            anchorNode = AnchorNode(anchor)
                            anchorNode?.setParent(arFragment.arSceneView.scene)
                            addPlaces(anchorNode!!)
                            arFragment.setAnchored()
                            arFragment.arSceneView.scene.addOnUpdateListener{}
                        }
                    }
                }

            }
        }
    }

    private fun addNote(anchorNode: AnchorNode) {
        newAnchorNode = anchorNode
        createLauncher.launch(Intent(this, EditActivity::class.java))
    }


    private fun addPlaces(anchorNode: AnchorNode) {
        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w(TAG, "Location has not been determined yet")
            return
        }

        val places = getNote()
        if (places == null) {
            Log.w(TAG, "No places to put")
            return
        }

        for (place in places) {
            // Add the place in AR
            val placeNode = PlaceNode(this, place)
            placeNode.setParent(anchorNode)
            placeNode.localPosition = place.getPositionVector(orientationAngles[0], currentLocation.latLng)
            placeNode.setOnTapListener { _, _ ->
                showInfoWindow(place, anchorNode)
            }
        }
    }

    private fun showInfoWindow(place: Place, anchorNode: AnchorNode) {
        // Show in AR
        currentPlaceNode = anchorNode?.children?.filter {
            it is PlaceNode
        }?.first {
            val otherPlace = (it as PlaceNode).place ?: return@first false
            return@first otherPlace == place
        } as? PlaceNode
        editLauncher.launch(Intent(this, EditActivity::class.java))
    }

    private fun getCurrentLocation() {
        LocationServices.getFusedLocationProviderClient(applicationContext)
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    currentLocation = Place("current", "ok", it.result.latitude.toString(), it.result.longitude.toString())
                } else {
                    Log.e("PostActivity getFusedLocation", it.exception.toString())
                }
            }
    }

    private fun isSupportedDevice(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val openGlVersionString = activityManager.deviceConfigurationInfo.glEsVersion
        if (openGlVersionString.toDouble() < 3.0) {
            Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            finish()
            return false
        }
        return true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }

}


