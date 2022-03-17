package edu.umich.argo.arnote.model

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.*

object NoteStore {
    var notes = mutableListOf<Place>()
    var _notes = mutableListOf<JsonPlace>()

    private const val gpsFilePath = "gps_notes.json"
    private val client = OkHttpClient()
    private const val serverUrl = "https://18.216.173.236/"


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
        val mpFD = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("message", place.message)
            .addFormDataPart("lat", place.lat)
            .addFormDataPart("lng", place.lng)
            .addFormDataPart("x", place.x)
            .addFormDataPart("y", place.y)
            .addFormDataPart("z", place.z)

        val request = Request.Builder()
            .url(serverUrl+"postnoteplace/")
            .post(mpFD.build())
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "Share note fails!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val id = JSONObject(response.body?.string() ?: "").getString("ID")
                    (context as Activity).runOnUiThread {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText("id", id)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Url copied to clipboard!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
