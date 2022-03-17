package edu.umich.argo.arnote.model

import android.content.Context
import android.text.Editable
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object NoteStore {
    private val TAG="NoteStore"
    var notes = mutableListOf<Place>()
    var _notes = mutableListOf<JsonPlace>()
    private val nFields = 10

    private lateinit var queue: RequestQueue
    private const val serverUrl = "https://18.216.173.236/"

    private const val gpsFilePath = "gps_notes.json"

    private val client = OkHttpClient()

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

    fun addNoteByID(ID:String, completion: ()->Unit) {
        val request = Request.Builder()
            .url(serverUrl + "getnote/?ID="+ID)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed GET request")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val chattsReceived = try {
                        JSONObject(response.body?.string() ?: "").getJSONArray("notes")
                    } catch (e: JSONException) {
                        JSONArray()
                    }
                    val chattEntry = chattsReceived as JSONArray
                    if (chattEntry.length() == nFields) {
                        addNoteToStore(Place(id = storeSize().toString(),
                            message = chattEntry[1].toString(),
                            lat=chattEntry[2].toString(),
                            lng=chattEntry[3].toString(),
                            x=chattEntry[4].toString(),
                            y=chattEntry[5].toString(),
                            z=chattEntry[6].toString(),
                            orientation = chattEntry[7].toString(),
                        ))
                    } else {
                        Log.e(TAG,
                            "Received unexpected number of fields: " + chattEntry.length()
                                .toString() + " instead of " + nFields.toString()
                        )
                    }
                }
                completion()
            }
        })

    }
}