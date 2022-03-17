package edu.umich.argo.arnote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

class EditActivity : AppCompatActivity() {
    lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        toolbar = findViewById(R.id.toolbar)
        initToolbar()
    }

    private fun initToolbar() {
        toolbar.title = "ARGo"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_new_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.inflateMenu(R.menu.editmenu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_share -> Toast.makeText(this, "Share", Toast.LENGTH_LONG).show()
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