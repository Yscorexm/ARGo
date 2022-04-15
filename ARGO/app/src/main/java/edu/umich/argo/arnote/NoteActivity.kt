package edu.umich.argo.arnote

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import edu.umich.argo.arnote.model.NoteListAdapter
import edu.umich.argo.arnote.model.NoteStore
import edu.umich.argo.arnote.model.NoteStore.addNoteByID
import edu.umich.argo.arnote.model.NoteStore.getNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class NoteActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var toolbartitle: TextView
    private lateinit var noteListView: ListView
    private lateinit var importButton: View
    private lateinit var cardView: CardView
    private lateinit var childImport: View
    private lateinit var childCancel: View
    private lateinit var editView: EditText
    private lateinit var noteListAdapter: NoteListAdapter
    private var m_currentToast: Toast? = null

    private fun showToast(text: String?) {
        if (m_currentToast != null) {
            m_currentToast!!.cancel()
        }
        m_currentToast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        m_currentToast?.show()
    }

    private val refreshTime:Long =500
    private val mRunnable: Runnable = object : Runnable {
        override fun run() {
            noteListAdapter.notifyDataSetChanged()
            noteListView.postDelayed(this, refreshTime)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)
        toolbar = findViewById(R.id.toolbar)
        noteListView= findViewById(R.id.noteListView)
        importButton = findViewById(R.id.importButton)
        cardView = findViewById(R.id.import_box)
        childImport = findViewById(R.id.child_import)
        editView=findViewById(R.id.input_import)
        childCancel=findViewById(R.id.child_cancel)
        initToolbar()
        initNoteListView()
        initImports()
        noteListView.postDelayed(mRunnable, refreshTime)
    }

    private fun initNoteListView(){
        val notes=getNote()
        noteListAdapter=NoteListAdapter(this,notes)
        noteListView.adapter=noteListAdapter
    }

    @SuppressLint("SetTextI18n")
    private fun initToolbar() {
        toolbar.title = ""
        toolbartitle = toolbar.findViewById(R.id.toolbar_title)
        toolbartitle.text="ARGo"
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
            val id=editView.text
            showToast("Importing the note...")
            addNoteByID(id.toString()) {
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        Log.d("Downloader", "Processing the image...")
                        showToast("Processing the image...")
                    }
                    val bitmap=getBitmap(it.imageUri)
                    if (bitmap!=null){
                        it.imageUri = saveImage(bitmap,applicationContext,"ARcore").toString()
                        NoteStore.toAdd.add(bitmap)
                        NoteStore.toAdd_name.add(NoteStore.storeSize().toString())
                    }

                    withContext(Dispatchers.Main) {
                        Log.d("Downloader", "Done!")
                        showToast("Done!")
                        NoteStore.addNoteToStore(it)
                    }
                }
            }
            editView.setText("")
        }
        childCancel.setOnClickListener {
            importButton.visibility = VISIBLE
            cardView.visibility = INVISIBLE
            editView.setText("")
        }
    }

    fun importNote(view: View?) {
        view?.visibility = INVISIBLE
        cardView.visibility = VISIBLE
    }
}

private fun getBitmap(imageUrl: String): Bitmap? {
    val url = URL(imageUrl)
    return BitmapFactory.decodeStream(url.openConnection().getInputStream())
}
