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
import android.util.Log
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import edu.umich.argo.arnote.MainActivity




class PlacesArFragment : ArFragment() {
    private var anchorSelected: Boolean = false
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
        session.configure(config)
        arSceneView.setupSession(session)
        if ((activity as MainActivity).setupAugmentedImagesDB(config, session))
            Log.d("arcoreimg_db", "success")
        else
            Log.e("arcoreimg_db","faliure setting up db")
        return config
    }
}
