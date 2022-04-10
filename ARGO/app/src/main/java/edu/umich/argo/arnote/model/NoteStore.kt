package edu.umich.argo.arnote.model

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.widget.Toast
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import android.graphics.Bitmap

import android.graphics.BitmapFactory

import android.os.AsyncTask
import android.widget.ImageView
import edu.umich.argo.arnote.R
import edu.umich.argo.arnote.saveImage
import java.io.InputStream
import java.lang.Exception
import java.net.URL


/*
Supports add note, edit note, load note
*/

fun Uri.toFile(context: Context): File? {

    if (!(authority == "media" || authority == "com.google.android.apps.photos.contentprovider")) {
        // for on-device media files only
        Toast.makeText(context, "Media file not on device", Toast.LENGTH_LONG).show()
        Log.d("Uri.toFile", authority.toString())
        return null
    }

    if (scheme.equals("content")) {
        var cursor: Cursor? = null
        try {
            cursor = context.getContentResolver().query(
                this, arrayOf("_data"),
                null, null, null
            )

            cursor?.run {
                moveToFirst()
                return File(getString(getColumnIndexOrThrow("_data")))
            }
        } finally {
            cursor?.close()
        }
    }
    return null
}

object NoteStore {
    private val TAG="NoteStore"
    var notes = mutableListOf<Place>()
    private val nFields = 11
    private const val serverUrl = "https://441.scarletissimo.cf/"
    private const val gpsFilePath = "gps_notes.json"
    private val client = OkHttpClient()

    // transform all GPS info in "gps_notes.json" into jsonStr
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

    // store GPS information of all notes to "gps_notes.json"
    fun dumpNote(context: Context) {
        val jsonList = Json.encodeToString(notes)
        context.openFileOutput(gpsFilePath, Context.MODE_PRIVATE).use {
            it.write(jsonList.toByteArray())
        }
    }

    fun addNoteToStore(place: Place) {
        notes.add(place)
    }

    fun editNote(place: Place?, message: String) {
        val targetId = place?.id
        for (i in 0 until notes.size) {
            if (targetId != null) {
                if (i == targetId.toInt()) {
                    notes[i].message = message
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
        notes.clear()
        for (i in 0 until data.length()) {
            val noteEntry = data[i] as JSONObject?
            if (noteEntry != null) {
                notes.add(
                    Place(
                        id = noteEntry.get("id").toString(),
                        type = noteEntry.get("type").toString(),
                        message = noteEntry.get("message").toString(),
                        lat = noteEntry.get("lat").toString(),
                        lng = noteEntry.get("lng").toString(),
                        x = noteEntry.get("x").toString(),
                        y = noteEntry.get("y").toString(),
                        z = noteEntry.get("z").toString(),
                        orientation = noteEntry.get("orientation").toString(),
                        imageUri = noteEntry.get("imageUri").toString(),
                    )
                )
            }
        }
    }

    fun getNote(): MutableList<Place> {
        return notes
    }

    fun getNotebyId(id: String) : Place {
        val noteResult = notes.filter {
            it.id == id
        }
        if (noteResult.isEmpty()) {
            error("Can't find note")
        }
        return noteResult[0]
    }

    // add a local place to backend
    fun postNote(context: Context, place: Place) {
        var mpFD = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("message", place.message)
        var finalUrl=serverUrl;
        if (place.type=="gps"){
            mpFD.addFormDataPart("lat", place.lat)
                .addFormDataPart("lng", place.lng)
                .addFormDataPart("x", place.x)
                .addFormDataPart("y", place.y)
                .addFormDataPart("z", place.z)
                .addFormDataPart("orientation",place.orientation)

            finalUrl+="postnoteplace/"
        }else{
            val imageUri=Uri.parse(place.imageUri)
            imageUri?.run {
                toFile(context)?.let {
                    mpFD.addFormDataPart("image", "itemImage",
                        it.asRequestBody("image/png".toMediaType()))
                } ?: Toast.makeText(context, "Unsupported image format", Toast.LENGTH_LONG).show()
            }
            finalUrl+="postnoteimage/"
        }
        val request = Request.Builder()
            .url(finalUrl)
            .post(mpFD.build())
            .build()
        Toast.makeText(context, "Uploading...", Toast.LENGTH_LONG).show()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "Share note fails!", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val id = JSONObject(response.body?.string() ?: "").getString("ID")
                    (context as Activity).runOnUiThread {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText("id", id)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Uuid copied to clipboard!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // retrieve a note from backend and add to local
    fun addNoteByID(ID:String, completion: (Place)->Unit) {
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
                        completion(Place(
                            id = storeSize().toString(),
                            message = chattEntry[1].toString(),
                            type = chattEntry[10].toString(),
                            lat = chattEntry[2].toString(),
                            lng = chattEntry[3].toString(),
                            x = chattEntry[4].toString(),
                            y = chattEntry[5].toString(),
                            z = chattEntry[6].toString(),
                            orientation = chattEntry[7].toString(),
                            imageUri = chattEntry[9].toString(),
                        ))
                    } else {
                        Log.e(TAG,
                            "Received unexpected number of fields: " + chattEntry.length()
                                .toString() + " instead of " + nFields.toString()
                        )
                    }
                }

            }
        })
    }
}


