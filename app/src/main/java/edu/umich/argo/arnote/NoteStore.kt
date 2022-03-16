package com.google.codelabs.findnearbyplacesar

import android.content.Context
import com.google.gson.Gson
import edu.umich.argo.arnote.model.Place
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.io.IOException

object NoteStore {
    var notes = mutableListOf<Place>()

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

    fun postNote(context: Context, place: Place) {




    }

    fun loadNote(context: Context) {
        val jsonStr = file2JsonStr(context)
        val data = JSONArray(jsonStr)
        val gson = Gson()
        for (i in 0 until data.length()) {
            val noteEntry = data[i] as JSONArray
            if (noteEntry.length() != 0) {
                notes.add(Place(
                    id = noteEntry[0].toString(),
                    name = noteEntry[1].toString(),
                    lat = noteEntry[2].toString(),
                    lng = noteEntry[3].toString()
                ))
            }
        }

    }

}