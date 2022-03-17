package edu.umich.argo.arnote.model

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object NoteStore {
    var notes = mutableListOf<Place>()
    var _notes = mutableListOf<JsonPlace>()

    private const val gpsFilePath = "gps_notes.json"
    private lateinit var queue: RequestQueue
    private const val serverUrl = "https://18.216.173.236/getnote/"


    private fun file2JsonStr(context: Context): String? {
        val stringBuilder = StringBuilder()
        try {
            val bf = context.openFileInput(gpsFilePath).bufferedReader()
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            bf.close()
            return stringBuilder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun dumpNote(context: Context) {
        val jsonList = Json.encodeToString(_notes)
        context.openFileOutput(gpsFilePath, Context.MODE_PRIVATE).use {
            it.write(jsonList.toByteArray())
        }
    }

    fun addNoteToStore(place: Place) {
        _notes.add(
            JsonPlace(
            id = place.id,
            message = place.message,
            lat = place.lat,
            lng = place.lng,
            x = place.x,
            y = place.y,
            z = place.z,
            orientation = place.orientation
        )
        )
        notes.add(place)
    }

    fun editNote(place: Place?, message: String) {
        val targetId = place?.id
        for (i in 0 until notes.size) {
            if (targetId != null) {
                if (i == targetId.toInt()) {
                    notes[i].message = message
                    _notes[i].message = message
                }
            }
        }
    }

    fun storeSize(): Int {
        return notes.size
    }

    fun loadNote(context: Context) {
        val jsonStr = file2JsonStr(context) ?: return
        val data = JSONArray(jsonStr)
        val gson = Gson()
        for (i in 0 until data.length()) {
            val noteEntry = data[i] as JSONObject?
            if (noteEntry != null) {
                _notes.add(JsonPlace(
                    id = noteEntry.get("id").toString(),
                    message = noteEntry.get("message").toString(),
                    lat = noteEntry.get("lat").toString(),
                    lng = noteEntry.get("lng").toString(),
                    x = noteEntry.get("x").toString(),
                    y = noteEntry.get("y").toString(),
                    z = noteEntry.get("z").toString(),
                    orientation = noteEntry.get("orientation").toString(),
                ))
            }
            if (noteEntry != null) {
                notes.add(Place(
                    id = noteEntry.get("id").toString(),
                    message = noteEntry.get("message").toString(),
                    lat = noteEntry.get("lat").toString(),
                    lng = noteEntry.get("lng").toString(),
                    x = noteEntry.get("x").toString(),
                    y = noteEntry.get("y").toString(),
                    z = noteEntry.get("z").toString(),
                    orientation = noteEntry.get("orientation").toString(),
                ))
            }
        }
    }

    fun getNote(): MutableList<Place> {
        return notes
    }

    fun postNote(context: Context, place: Place) {
        val jsonObj = mapOf(
            "message" to place.name,
            "lat" to place.lat,
            "lng" to place.lng,
            "x" to place.x,
            "y" to place.y,
            "z" to place.z
        )
        val postRequest = JsonObjectRequest(
            Request.Method.POST,
            serverUrl+"postnoteplace/", JSONObject(jsonObj),
            { Log.d("postNote", "note posted!") },
            { error -> Log.e("postNote", error.localizedMessage ?: "JsonObjectRequest error") }
        )

        if (!this::queue.isInitialized) {
            queue = newRequestQueue(context)
        }
        queue.add(postRequest)
    }
}
