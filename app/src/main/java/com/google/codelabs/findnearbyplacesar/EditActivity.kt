package com.google.codelabs.findnearbyplacesar

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText

class EditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

    }

    fun saveNote(view: View?) {
        val editText = findViewById<EditText>(R.id.message)
        val intent = Intent()
        intent.putExtra("message", editText.text.toString())
        setResult(RESULT_OK, intent)
        finish()
    }
}