package edu.umich.argo.arnote.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import edu.umich.argo.arnote.R
import android.graphics.Color
import android.widget.*
import edu.umich.argo.arnote.model.NoteStore.postNote
import android.net.Uri
import com.squareup.picasso.Picasso



class NoteListAdapter(context: Context, users: MutableList<Note>) :
    ArrayAdapter<Note?>(context, 0, users as List<Note?>) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = LayoutInflater.from(context).inflate(R.layout.listitem_note, parent, false)

        getItem(position)?.run {
            val tView = rowView.findViewById<TextView>(R.id.tView)
            val iView = rowView.findViewById<ImageView>(R.id.iView)
            val share = rowView.findViewById<ImageButton>(R.id.imageButton)
            tView.text = message
            iView.setImageURI(null)

            if (type=="gps"){
                iView.setImageResource(R.mipmap.pin_full_color)
                iView.rotation= 0F
            }else{
                if (imageUri.contains("https")){
                    Picasso.get().load(imageUri).into(iView)
                }else{
                    iView.setImageURI(Uri.parse(imageUri))
                }

                iView.rotation= 90F
            }

            rowView.setBackgroundColor(Color.parseColor(if (position % 2 == 0) "#E0E0E0" else "#EEEEEE"))
            share.setOnClickListener {
                // once a note is shared, upload the note to backend
                postNote(context, this)
            }
        }
        return rowView
    }
}