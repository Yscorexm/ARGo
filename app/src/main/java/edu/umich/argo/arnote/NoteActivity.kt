package edu.umich.argo.arnote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import edu.umich.argo.arnote.ar.NoteStore
import edu.umich.argo.arnote.model.NoteListAdapter
import edu.umich.argo.arnote.ar.NoteStore.getNote

class NoteActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var noteListView: ListView
    private lateinit var importButton: View
    private lateinit var cardView: CardView
    private lateinit var childImport: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)
        toolbar = findViewById(R.id.toolbar)
        noteListView= findViewById(R.id.noteListView)
        importButton = findViewById(R.id.importButton)
        cardView = findViewById(R.id.import_box)
        childImport = findViewById(R.id.child_import)
        initToolbar()
        initNoteListView()
        initImports()
    }
    private fun initNoteListView(){
        val notes=getNote()
        val noteListAdapter=NoteListAdapter(this,notes)
        noteListView.adapter=noteListAdapter
    }

    private fun initToolbar() {
        toolbar.title = "ARGo"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_new_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.inflateMenu(R.menu.plainmenu)
    }

    private fun initImports() {
        cardView.visibility = INVISIBLE
        childImport.setOnClickListener {
            importButton.visibility = VISIBLE
            cardView.visibility = INVISIBLE
        }
    }

    fun importNote(view: View?) {
        view?.visibility = INVISIBLE
        cardView.visibility = VISIBLE
    }
}