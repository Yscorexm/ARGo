package edu.umich.argo.arnote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import edu.umich.argo.arnote.model.NoteStore.getNote
import edu.umich.argo.arnote.model.NoteStore.postNote
import edu.umich.argo.arnote.model.Place


class EditActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var toolbartitle: TextView
    private lateinit var text: EditText
    private lateinit var shareButton: View
    private var place: Place? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        // retrieve the note
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        toolbar = findViewById(R.id.toolbar)
        text = findViewById(R.id.message)
        initToolbar()
        shareButton = findViewById(R.id.action_share)
        val placeId = intent.extras?.getString("placeId")
        placeId?.let {
            shareButton.isEnabled = true
            val places = getNote()
            place = places.filter {
                it.id == placeId
            }.first()
            text.hint = place?.message
            text.setText(place?.message, TextView.BufferType.EDITABLE)
        }?: let {
            shareButton.isEnabled = false
        }
    }

    private fun initToolbar() {
        toolbar.title = ""
        toolbartitle = toolbar.findViewById(R.id.toolbar_title)
        toolbartitle.text="ARGo"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_new_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.inflateMenu(R.menu.editmenu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_share -> place?.let { p -> postNote(applicationContext, p) }
            }
            true
        }
    }

    fun saveNote(view: View?) {
        val editText = findViewById<EditText>(R.id.message)
        val intent = Intent()
        intent.putExtra("message", editText.text.toString())
        setResult(RESULT_OK, intent)
        finish()
    }
}