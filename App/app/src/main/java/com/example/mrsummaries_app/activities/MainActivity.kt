package com.example.mrsummaries_app.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.adapters.FolderAdapter
import com.example.mrsummaries_app.fragments.NoteEditorFragment
import com.example.mrsummaries_app.models.Folder
import com.example.mrsummaries_app.models.Note
import com.example.mrsummaries_app.storage.FileSystemManager

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var folderActionsLayout: LinearLayout
    private lateinit var createSubfolderButton: Button
    private lateinit var createNoteButton: Button

    private var currentFolder: Folder? = null
    private val displayItems = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the file system manager
        fileSystemManager = FileSystemManager(this)

        setupUI()
        setupDrawer()
        setupFoldersList()

        // Initially load the root folder
        loadFolder(fileSystemManager.getRootFolder())
    }

    private fun setupUI() {
        drawerLayout = findViewById(R.id.drawerLayout)
        folderActionsLayout = findViewById(R.id.folderActionsLayout)
        createSubfolderButton = findViewById(R.id.createSubfolderButton)
        createNoteButton = findViewById(R.id.createNoteButton)

        createSubfolderButton.setOnClickListener {
            showCreateFolderDialog()
        }

        createNoteButton.setOnClickListener {
            showCreateNoteDialog()
        }

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    private fun setupDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupFoldersList() {
        val foldersRecyclerView = findViewById<RecyclerView>(R.id.foldersRecyclerView)

        folderAdapter = FolderAdapter(
            onFolderClicked = { folder ->
                loadFolder(folder)
                folderActionsLayout.visibility = View.VISIBLE
            },
            onNoteClicked = { note ->
                openNote(note)
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        )

        foldersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = folderAdapter
        }
    }

    private fun loadFolder(folder: Folder) {
        currentFolder = folder

        displayItems.clear()
        displayItems.addAll(folder.subfolders)
        displayItems.addAll(folder.notes)

        folderAdapter.updateItems(displayItems)
        folderActionsLayout.visibility = View.VISIBLE
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this)
        input.hint = "Folder Name"

        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val folderName = input.text.toString()
                if (folderName.isNotEmpty() && currentFolder != null) {
                    val newFolder = fileSystemManager.createFolder(folderName, currentFolder!!)
                    loadFolder(currentFolder!!)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showCreateNoteDialog() {
        val input = EditText(this)
        input.hint = "Note Title"

        AlertDialog.Builder(this)
            .setTitle("Create New Note")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val noteTitle = input.text.toString()
                if (noteTitle.isNotEmpty() && currentFolder != null) {
                    val newNote = fileSystemManager.createNote(noteTitle, currentFolder!!)
                    loadFolder(currentFolder!!)
                    openNote(newNote)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun openNote(note: Note) {
        val noteEditorFragment = NoteEditorFragment.newInstance(note.id)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContentFrame, noteEditorFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}