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

package edu.umich.argo.arnote.model

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.ar.sceneform.math.Vector3
import com.google.maps.android.ktx.utils.sphericalHeading
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

/**
 * A model describing details about a Note (location, name, type, etc.).
 */

@Serializable
data class Note(
    val id: String,
    val type: String,  // (item | gps)
    var message: String,
    val lat: String,
    val lng: String,
    val x: String,
    val y: String,
    val z: String,
    val orientation: String,
    var imageUri: String
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Note) {
            return false
        }
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    fun getLatLng(): LatLng {
        return LatLng(lat.toDouble(), lng.toDouble())
    }
}

fun Note.getPositionVector(azimuth: Float, latLng: LatLng): Vector3 {
    val placeLatLng = this.getLatLng()
    val heading = latLng.sphericalHeading(placeLatLng)
    val distance = getDistance(latLng)
    val x1 = distance * sin(azimuth + heading).toFloat()
    val z1 = distance * cos(azimuth + heading).toFloat()

    val x2 = this.x.toFloat() * sin(this.orientation.toDouble()).toFloat()
    val z2 = this.z.toFloat() * cos(this.orientation.toDouble()).toFloat()
    val x = x1 + x2
    val y = 1f
    val z = z1 + z2
    Log.d("Note", this.orientation)
    Log.d("Note", "$x1 $x2 $distance")
    Log.d("Note", Vector3(x, y, z).toString())
    return Vector3(x, y, z)
}

fun Note.getDistance(latLng: LatLng): Float {
    val startLocation = Location("Start")
    startLocation.latitude = this.getLatLng().latitude
    startLocation.longitude = this.getLatLng().longitude
    val endLocation = Location("end")
    endLocation.latitude = latLng.latitude
    endLocation.longitude = latLng.longitude
    val distance = startLocation.distanceTo(endLocation)
    Log.d("Note", distance.toString())
    return distance
}
