package edu.umich.argo.arnote.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import edu.umich.argo.arnote.R
import android.graphics.Color
import android.widget.*
import edu.umich.argo.arnote.model.NoteStore.postNote
import android.graphics.BitmapFactory

import android.graphics.Bitmap
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL


class NoteListAdapter(context: Context, users: MutableList<Place>) :
    ArrayAdapter<Place?>(context, 0, users as List<Place?>) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = LayoutInflater.from(context).inflate(R.layout.listitem_note, parent, false)

        getItem(position)?.run {
            val tView = rowView.findViewById<TextView>(R.id.tView)
            val iView = rowView.findViewById<ImageView>(R.id.iView)
            val share = rowView.findViewById<ImageButton>(R.id.imageButton)
            tView.text = message
//            imageUri?.let{
//                try {
//                    val bitmap =
//                        BitmapFactory.decodeStream(URL(it).getContent() as InputStream)
//                    iView.setImageBitmap(bitmap)
//                } catch (e: MalformedURLException) {
//                    e.printStackTrace()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }?:run{
            iView.setImageResource(R.mipmap.pin_full_color)
//            }

            rowView.setBackgroundColor(Color.parseColor(if (position % 2 == 0) "#E0E0E0" else "#EEEEEE"))
            share.setOnClickListener {
                // once a note is shared, upload the note to backend
                postNote(context, this)
            }
        }
        return rowView
    }
}