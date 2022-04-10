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

package edu.umich.argo.arnote.ar

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ar.core.*
import com.google.ar.sceneform.ux.ArFragment
import edu.umich.argo.arnote.MainActivity
import edu.umich.argo.arnote.model.NoteStore
import java.util.*


class PlacesArFragment : ArFragment() {
    private var anchorSelected: Boolean = false
    var imageDatabase: AugmentedImageDatabase? = null
    var numImageInDB = 0
    private val arAugImageDBPath = "argo_item_notes_database.imgdb"

    override fun getAdditionalPermissions(): Array<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray()

    override fun onResume() {
        super.onResume()
        if (anchorSelected) {
            planeDiscoveryController.hide()
        }
    }

    fun setAnchored() {
        //get the trackables to ensure planes are detected
        anchorSelected=true
    }

    override fun getSessionConfiguration(session: Session): Config {
//        planeDiscoveryController.setInstructionView(null)
        val config = Config(session)
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
        config.setFocusMode(Config.FocusMode.AUTO)

        val filter = CameraConfigFilter(session)

        filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
        val cameraConfigList = session?.getSupportedCameraConfigs(filter)

        session.cameraConfig = cameraConfigList[0]

        session.configure(config)
        arSceneView.setupSession(session)
        session.apply {
            resume()
            pause()
            resume()
        }

        if ((activity as MainActivity).setupAugmentedImagesDB(config, session))
            Log.d("arcoreimg_db", "success")
        else
            Log.e("arcoreimg_db","faliure setting up db")
        return config
    }

    fun dumpDB(context: Context) {
        context.openFileOutput(arAugImageDBPath, Context.MODE_PRIVATE).use {
            imageDatabase?.serialize(it)
        }
    }

    fun loadDB(session: Session, context: Context): AugmentedImageDatabase? {
        imageDatabase = AugmentedImageDatabase(session)
        imageDatabase = AugmentedImageDatabase.deserialize(session, context.openFileInput(
            arAugImageDBPath
        ))
        return imageDatabase
    }

    fun addImage(imgName: String, bitmap: Bitmap) {
        imageDatabase?.addImage(imgName, bitmap)
        this.arSceneView.session?.apply {
            val changedConfig = config
            changedConfig.augmentedImageDatabase = imageDatabase
            configure(changedConfig)
        }
    }

    fun addImportImage() {
        for (i in 0 until NoteStore.toAdd.size) {
            addImage(NoteStore.toAdd_name[i], NoteStore.toAdd[i])
        }
        NoteStore.toAdd.clear()
        NoteStore.toAdd_name.clear()
    }
}
