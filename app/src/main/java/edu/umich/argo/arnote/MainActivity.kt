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
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import edu.umich.argo.arnote.R
import edu.umich.argo.arnote.ar.PlaceNode
import edu.umich.argo.arnote.ar.PlacesArFragment
import edu.umich.argo.arnote.model.Geometry
import edu.umich.argo.arnote.model.GeometryLocation
import edu.umich.argo.arnote.model.Place
import edu.umich.argo.arnote.model.getPositionVector

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "MainActivity"

    private lateinit var arFragment: PlacesArFragment

    // Sensor
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var anchorNode: AnchorNode? = null
    private var otherAnchorNodes = mutableListOf<AnchorNode>()
    private var markers: MutableList<Marker> = emptyList<Marker>().toMutableList()
    private var places: MutableList<Place>? = null
    private var currentLocation: Place? = null

    private var anchorSelected: Boolean = false
    private var currentPlaceNode: PlaceNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isSupportedDevice()) {
            return
        }
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as PlacesArFragment

        sensorManager = getSystemService()!!

        this.places = mutableListOf(
            Place("id0", "note1, balabala", Geometry(GeometryLocation(lat=42.3009473, lng=-83.73001909999999))),
            Place("id1", "note2, wt", Geometry(GeometryLocation(lat=42.299268, lng=-83.717808)))
        )
        getCurrentLocation()
        setUpAr()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
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
                            //Hide the plane discovery helper animation
                            // arFragment.planeDiscoveryController.hide()
                            val anchor = plane.createAnchor(plane.centerPose)
                            anchorSelected = true
                            anchorNode = AnchorNode(anchor)
                            anchorNode?.setParent(arFragment.arSceneView.scene)
                            addPlaces(anchorNode!!)
                            break
                        }
                    }
                }

            }
        }
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            // Create anchor
            val anchor = hitResult.createAnchor()
            val newAnchorNode = AnchorNode(anchor)
            newAnchorNode?.setParent(arFragment.arSceneView.scene)
            otherAnchorNodes.add(newAnchorNode)
            addNote(newAnchorNode!!)
        }
    }

    private fun addNote(anchorNode: AnchorNode) {
        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w(TAG, "Location has not been determined yet")
            return
        }

        val place = currentLocation
        this.places?.add(place)
        val placeNode = PlaceNode(this, place)
        placeNode.setParent(anchorNode)
        placeNode.localPosition = Vector3(0f, 1f, 0f)
        placeNode.setOnTapListener { _, _ ->
            showInfoWindow(place, anchorNode)
        }
//        val placeNode = PlaceNode(this, null)

    }


    private fun addPlaces(anchorNode: AnchorNode) {
        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w(TAG, "Location has not been determined yet")
            return
        }

        val places = places
        if (places == null) {
            Log.w(TAG, "No places to put")
            return
        }

        for (place in places) {
            // Add the place in AR
            val placeNode = PlaceNode(this, place)
            placeNode.setParent(anchorNode)
            placeNode.localPosition = place.getPositionVector(orientationAngles[0], currentLocation.geometry.location.latLng)
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
        startActivityForResult(Intent(this, EditActivity::class.java), 1)

    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        LocationServices.getFusedLocationProviderClient(applicationContext)
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    currentLocation = Place("current", "ok", Geometry(GeometryLocation(it.result.latitude, it.result.longitude)))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val message = data?.getStringExtra("message")
                currentPlaceNode
                currentPlaceNode?.setText(message)
            }
        }
    }
}


