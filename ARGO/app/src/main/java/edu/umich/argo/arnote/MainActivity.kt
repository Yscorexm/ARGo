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
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.ImageFormat.NV21
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.getSystemService
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import edu.umich.argo.arnote.ar.PlaceNode
import edu.umich.argo.arnote.ar.PlacesArFragment
import edu.umich.argo.arnote.model.NoteStore.addNoteToStore
import edu.umich.argo.arnote.model.NoteStore.dumpNote
import edu.umich.argo.arnote.model.NoteStore.editNote
import edu.umich.argo.arnote.model.NoteStore.getNote
import edu.umich.argo.arnote.model.NoteStore.getNotebyId
import edu.umich.argo.arnote.model.NoteStore.loadNote
import edu.umich.argo.arnote.model.NoteStore.storeSize
import edu.umich.argo.arnote.model.Place
import edu.umich.argo.arnote.model.getDistance
import edu.umich.argo.arnote.model.getPositionVector
import java.io.*
import java.nio.ByteBuffer


@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "MainActivity"

    private lateinit var arFragment: PlacesArFragment
    private lateinit var toolbar: Toolbar
    private lateinit var toolbartitle: TextView
    private lateinit var addButton: View
    private lateinit var itemButton: View
    private lateinit var gpsButton: View
    private lateinit var createLauncher: ActivityResultLauncher<Intent>
    private lateinit var createItemLauncher: ActivityResultLauncher<Intent>
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

    // augmented images
    private var imageDatabase: AugmentedImageDatabase? = null
    private val arAugImageDBPath = "argo_item_notes_database.imgdb"
    var imageUri: Uri? = null
    var tmpImageUri: Uri? = null
    private lateinit var forCropResult: ActivityResultLauncher<Intent>
    private var trackingItemNotes: Set<String> = setOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isSupportedDevice()) {
            return
        }
        setContentView(R.layout.activity_main)
        getPermission()              // get permission for GPS
        getCurrentLocation()
        createLaunchers()
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as PlacesArFragment
        sensorManager = getSystemService()!!
        toolbar = findViewById(R.id.toolbar)
        initToolbar()
        addButton = findViewById(R.id.floatingActionButton)
        itemButton = findViewById(R.id.itembutton)
        gpsButton = findViewById(R.id.gpsbutton)
        itemButton.visibility = INVISIBLE
        gpsButton.visibility = INVISIBLE
        setButtons()

        loadNote(applicationContext)
        val places = getNote()
        if (places?.size ?: 0 == 0) {
            addNoteToStore(
                Place(
                    storeSize().toString(),
                    "gps",
                    "note1, balabala",
                    lat=(42.3009473).toString(),
                    lng=(-83.73001909999999).toString(),
                    x=(1.00).toString(),
                    y=(1.00).toString(),
                    z=(1.00).toString(),
                    orientation = (0.00).toString(),
                    ""
                ),
            )
            addNoteToStore(
                Place(storeSize().toString(),
                    "gps",
                    "note2, wt",
                    lat=(42.299268).toString(),
                    lng=(-83.717808).toString(),
                    x=(1.00).toString(),
                    y=(1.00).toString(),
                    z=(1.00).toString(),
                    orientation = (0.00).toString(),
                    ""
                )
            )
        }
        setUpAr()

//        val cropIntent = initCropIntent()

        forCropResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data.let {
                        imageUri?.run {
                            if (!toString().contains("ORIGINAL")) {
                                // delete uncropped photo taken for posting
//                                contentResolver.delete(this, null, null)
                            }
                        }
                        imageUri = it
                        createItemLauncher.launch(Intent(this, EditActivity::class.java))
                    }
                } else {
                    Log.d("Crop", result.resultCode.toString())
                }
            }
    }

    private fun getPermission() {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach {
                if (!it.value) {
                    Toast.makeText(this, "Location access denied", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    private fun createLaunchers() {
        createLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val message = it.data?.getStringExtra("message")?:""

                currentLocation?.let {
                    val place = Place(
                        storeSize().toString(),
                        "gps",
                        message,
                        it.lat,
                        it.lng,
                        newAnchorNode?.anchor?.pose?.tx().toString(),
                        newAnchorNode?.anchor?.pose?.ty().toString(),
                        newAnchorNode?.anchor?.pose?.tz().toString(),
                        orientationAngles[0].toString(),
                        ""
                    )
                    Log.d("Place", orientationAngles[0].toString())
                    addNoteToStore(place)
                    newAnchorNode?.let {
                        val placeNode = PlaceNode(this, place)
                        placeNode.setParent(it)
                        placeNode.localPosition = Vector3(0f, 1f, 0f)
                        placeNode.setOnTapListener { _, _ ->
                            showInfoWindow(place, it)
                        }
                    }
                }
            }
        }
        createItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val message = it.data?.getStringExtra("message")?:""
                val place = Place(
                    storeSize().toString(),
                    "item",
                    message,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    imageUri.toString()
                )
                addNoteToStore(place)
                imageUri = null
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
        listLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}
    }

    private fun initToolbar() {
        toolbar.title = ""
        toolbartitle = toolbar.findViewById(R.id.toolbar_title)
//        setSupportActionBar(toolbar)
        toolbartitle.text="ARGo"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_event_note_24)
        toolbar.setNavigationOnClickListener {
            // change to listActivity
            listLauncher.launch(Intent(this, NoteActivity::class.java))
        }
        toolbar.inflateMenu(R.menu.plainmenu)
    }

    private fun setButtons() {
        addButton.setOnClickListener {
            it.visibility = INVISIBLE
            itemButton.visibility = VISIBLE
            gpsButton.visibility = VISIBLE
        }
        itemButton.setOnClickListener {
            val imageObj = arFragment.arSceneView.arFrame?.acquireCameraImage()

            val bytes =
                imageObj?.let {
                        it1 -> YUV_420_888toNV21(it1)?.let { it1 -> imageObj?.let {
                            it2 -> NV21toJPEG(it1, it2.width, imageObj.height)
                        } }
                }
            val image = bytes?.let { it1 -> BitmapFactory.decodeByteArray(bytes, 0, it1.size, null) }
            if (image != null) {
                imageUri = saveImage(image, applicationContext, "ARcore")
            }
//            imageUri = image?.let { it1 -> getUriFromBitmap(it1) }
            val cropIntent = initCropIntent()
            cropIntent?.putExtra(Intent.EXTRA_STREAM, imageUri)
            doCrop(cropIntent)
            imageDatabase?.addImage(storeSize().toString(), image)
            arFragment.arSceneView.session?.apply {
                val changedConfig = config
                changedConfig.augmentedImageDatabase = imageDatabase
                configure(changedConfig)
            }
        }
        gpsButton.setOnClickListener {
            arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
                // Create anchor
                val anchor = hitResult.createAnchor()
                val newAnchorNode = AnchorNode(anchor)
                newAnchorNode?.setParent(arFragment.arSceneView.scene)
                otherAnchorNodes.add(newAnchorNode)
                addNote(newAnchorNode!!)
            }
            itemButton.visibility = INVISIBLE
            gpsButton.visibility = INVISIBLE
        }
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
        arFragment.arSceneView.session?.resume()
        arFragment.setOnTapArPlaneListener(null)
        addButton.visibility = VISIBLE
        itemButton.visibility = INVISIBLE
        gpsButton.visibility = INVISIBLE
//        if (anchorNode != null) {
//            if (!anchorNode?.isTracking!!) {
//                anchorSelected = false
//                setUpAr()
//            }
//        }
    }

    override fun onPause() {
        super.onPause()
//        sensorManager.unregisterListener(this)
        applicationContext.openFileOutput(arAugImageDBPath, Context.MODE_PRIVATE).use {
            imageDatabase?.serialize(it)
        }
        dumpNote(applicationContext)
    }

    private fun setUpAr() {
        arFragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            val frame = arFragment.arSceneView.arFrame
            // get the trackables to ensure planes are detected
            if (frame != null) {
                if (!anchorSelected) {
                    val planes = frame.getUpdatedTrackables(Plane::class.java).iterator()
                    while(planes.hasNext()) {
                        val plane = planes.next() as Plane

                        // If a plane has been detected & is being tracked by ARCore
                        if (plane.trackingState == TrackingState.TRACKING && !anchorSelected) {
                            // Hide the plane discovery helper animation
                            // arFragment.planeDiscoveryController.hide()
                            val anchor = plane.createAnchor(plane.centerPose)

                            val x = anchor.pose.tx()
                            val y = anchor.pose.ty()
                            val z = anchor.pose.tz()
                            val distance = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
                            Log.d(TAG, distance.toString())
                            if (distance > 50) {
                                continue
                            }
                            anchorSelected = true
                            anchorNode = AnchorNode(anchor)
                            anchorNode?.setParent(arFragment.arSceneView.scene)
                            addPlaces(anchorNode!!)
                            arFragment.setAnchored()
                            break
                        }
                    }
                }

                // Augmented images
                val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

                for (img in updatedAugmentedImages) {
                    if (img.trackingState == TrackingState.TRACKING) {

                        Log.d("AugImage", img.name)

                        // You can also check which image this is based on AugmentedImage.getName().
                        val notePlace = getNotebyId(img.name)
                        if (!trackingItemNotes.contains(notePlace.id)) {
                            val centerPoseAnchor: Anchor = img.createAnchor(img.centerPose)
                            trackingItemNotes.plus(notePlace.id)
                            placeObject(arFragment, centerPoseAnchor, notePlace)
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
            // Skip item based place
            if (place.lat == "") {
                continue
            }
            // Add the place in AR
             if (place.type == "item") { continue }
            val placeNode = PlaceNode(this, place)
            if (placeNode.place != null) {
                if (placeNode.place.getDistance(currentLocation.getLatLng()) > 10.0) {
                    continue
                }
            }

            placeNode.setParent(anchorNode)
            placeNode.localPosition = place.getPositionVector(orientationAngles[0], currentLocation.getLatLng())
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
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("placeId", place.id)
        editLauncher.launch(intent)
    }

    private fun getCurrentLocation() {
        //Get current location of the user
        LocationServices.getFusedLocationProviderClient(applicationContext)
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    currentLocation = Place(
                        "current",
                        "gps",
                        "ok",
                        it.result.latitude.toString(),
                        it.result.longitude.toString(),
                        (0.00).toString(),
                        (0.00).toString(),
                        (0.00).toString(),
                        (0.00).toString(),
                        ""
                    )
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

    fun setupAugmentedImagesDB(config: Config, session: Session): Boolean {
        imageDatabase = AugmentedImageDatabase(session)
//        val filename = "default.jpg"
//        val bitmap = loadAugmentedImage(filename) ?: return false
//        imageDatabase.addImage("default", bitmap)
        imageDatabase = AugmentedImageDatabase.deserialize(session, application.openFileInput(arAugImageDBPath))
        config.augmentedImageDatabase = imageDatabase
        session.configure(config)
        return true
    }

    private fun loadAugmentedImage(imagename: String): Bitmap? {
        try {
            assets.open(imagename).use { return BitmapFactory.decodeStream(it) }
        } catch (e: IOException) {
            Toast.makeText(this, "Error load image", Toast.LENGTH_LONG).show()
        }
        return null
    }

    private fun placeObject(arFragment: ArFragment, anchor: Anchor, place: Place) {
        val anchorNode = AnchorNode(anchor)
        val placeNode = PlaceNode(this, place)
        placeNode.setParent(anchorNode)
        placeNode.localScale = Vector3(0.20f, 0.20f, 0.20f)
        placeNode.localPosition = Vector3(0f, 0f, 0f)
        placeNode.localRotation = Quaternion(Vector3(90f, 270f, 0f))
        placeNode.setOnTapListener { _, _ ->
            showInfoWindow(place, anchorNode)
        }
        anchorNode.setParent(arFragment.arSceneView.scene)
    }

    private fun initCropIntent(): Intent? {
        // Is there any published Activity on device to do image cropping?
        val intent = Intent("com.android.camera.action.CROP")
        intent.type = "image/*"
        val listofCroppers = packageManager.queryIntentActivities(intent, 0)
        // No image cropping Activity published
        if (listofCroppers.size == 0) {
            Log.d("Crop", "Device does not support image cropping")
            return null
        }

        intent.component = ComponentName(
            listofCroppers[0].activityInfo.packageName,
            listofCroppers[0].activityInfo.name)

        // create a random crop box:
        intent.putExtra("scale", true)
            .putExtra("crop", true)
            .putExtra("return-data", false)
            .putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            .putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
            .putExtra("noFaceDetection", true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        return intent
    }

    private fun doCrop(intent: Intent?) {
        intent ?: run {
            imageUri?.let { }
            return
        }

        imageUri?.let {
            intent.data = it
            forCropResult.launch(intent)
        }
    }

    /// @param folderName can be your app's name
    private fun saveImage(bitmap: Bitmap, context: Context, folderName: String): Uri? {
        val values = contentValues()
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName)
        values.put(MediaStore.Images.Media.IS_PENDING, true)
        // RELATIVE_PATH and IS_PENDING are introduced in API 29.

        val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
            values.put(MediaStore.Images.Media.IS_PENDING, false)
            context.contentResolver.update(uri, values, null, null)
        }
        return uri
    }

    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun YUV_420_888toNV21(image: Image): ByteArray? {
        val nv21: ByteArray
        val yBuffer: ByteBuffer = image.getPlanes().get(0).getBuffer()
        val uBuffer: ByteBuffer = image.getPlanes().get(1).getBuffer()
        val vBuffer: ByteBuffer = image.getPlanes().get(2).getBuffer()
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }

    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return out.toByteArray()
    }
}



