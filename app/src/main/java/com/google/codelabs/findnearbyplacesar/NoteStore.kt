package com.google.codelabs.findnearbyplacesar

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import com.google.codelabs.findnearbyplacesar.model.Geometry
import com.google.codelabs.findnearbyplacesar.model.GeometryLocation
import com.google.codelabs.findnearbyplacesar.model.Place
import com.google.gson.Gson
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object NoteStore {
    val notes = mutableListOf<Place>()

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

    private fun jsonStr2List(jsonStr: String?) {
        val data = JSONArray(jsonStr)
        val gson = Gson()
        for (i in 0 until data.length()) {
            val placeEntry = data[i] as JSONArray
            if (placeEntry.length() != 0) {
                if (notes != null) {
                    notes.add(Place(id = placeEntry[0].toString(),
                        name = placeEntry[1].toString(),
                        geometry = Geometry(
                            location = GeometryLocation(
                                lat = placeEntry[2].toString().toDouble(),
                                lng = placeEntry[3].toString().toDouble()
                            )
                        )
                    ))
                }
            }
        }
    }

    fun postNote(context: Context, place: Place) {




    }

    fun loadNote(context: Context) {
        val jsonStr = file2JsonStr(context)

    }

}