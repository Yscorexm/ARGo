package edu.umich.argo.arnote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

class NoteActivity : AppCompatActivity() {
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)
        toolbar = findViewById(R.id.toolbar)
        initToolbar()
    }

    private fun initToolbar() {
        toolbar.title = "ARGo"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_new_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.inflateMenu(R.menu.plainmenu)

    }
}