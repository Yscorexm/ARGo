package edu.umich.argo.arnote.ar

import android.content.Context
import com.google.gson.Gson
import edu.umich.argo.arnote.model.JsonPlace
import edu.umich.argo.arnote.model.Place
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.io.IOException

object NoteStore {
    var notes = mutableListOf<Place>()
    var _notes = mutableListOf<JsonPlace>()

    private const val gpsFilePath = "gps_notes.json"

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
        val jsonList = Json.encodeToString(this._notes)
        context.openFileOutput(gpsFilePath, Context.MODE_PRIVATE).use {
            it.write(jsonList.toByteArray())
        }
    }

    fun addNoteToStore(place: Place) {
        this._notes.add(
            JsonPlace(
            id = place.id,
            name = place.name,
            lat = place.lat,
            lng = place.lng
        )
        )
        this.notes.add(place)
    }

    fun loadNote(context: Context) {
        val jsonStr = file2JsonStr(context) ?: return
        val data = JSONArray(jsonStr)
        val gson = Gson()
        for (i in 0 until data.length()) {
            val noteEntry = data[i] as JSONArray
            if (noteEntry.length() != 0) {
                this._notes.add(JsonPlace(
                    id = noteEntry[0].toString(),
                    name = noteEntry[1].toString(),
                    lat = noteEntry[2].toString(),
                    lng = noteEntry[3].toString()
                ))
                this.notes.add(Place(
                    id = noteEntry[0].toString(),
                    name = noteEntry[1].toString(),
                    lat = noteEntry[2].toString(),
                    lng = noteEntry[3].toString()
                ))
            }
        }
    }

    fun getNote(): MutableList<Place> {
        return this.notes
    }

}