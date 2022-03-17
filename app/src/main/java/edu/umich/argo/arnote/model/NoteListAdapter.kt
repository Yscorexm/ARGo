package edu.umich.argo.arnote.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import edu.umich.argo.arnote.R
import edu.umich.argo.arnote.ar.PlaceNode
import android.graphics.Color
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

class NoteListAdapter(context: Context, users: MutableList<Place>) :
    ArrayAdapter<Place?>(context, 0, users as List<Place?>) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = LayoutInflater.from(context).inflate(R.layout.listitem_note, parent, false)

        getItem(position)?.run {
            val tView = rowView.findViewById<TextView>(R.id.tView)
            val iView = rowView.findViewById<ImageView>(R.id.iView)
            val share = rowView.findViewById<ImageButton>(R.id.imageButton)
            tView.text = name
            iView.setImageResource(R.mipmap.pin_full_color)
            rowView.setBackgroundColor(Color.parseColor(if (position % 2 == 0) "#E0E0E0" else "#EEEEEE"))
            share.setOnClickListener {

            }
        }
        return rowView
    }
}