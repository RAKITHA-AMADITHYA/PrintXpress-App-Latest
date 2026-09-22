package com.example.printxpress

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class DesignsActivity : BaseActivity() {

    private lateinit var adapter: RowAdapter
    private var designs = listOf<Design>()

    private val pickFile = registerForActivityResult(OpenPersistableDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val (name, size) = queryFileInfo(this, uri)
        if (size > MAX_FILE_BYTES) { toast("File is too large. Maximum size is 25 MB."); return@registerForActivityResult }
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: SecurityException) { }
        promptText(this, "Name this design", "e.g. Company logo card", name.substringBeforeLast('.').take(40)) { title ->
            db.addDesign(uid, title, name, uri.toString(), "")
            toast("Design saved")
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_list, "Saved Designs")
        findViewById<TextView>(R.id.tvHint).apply {
            visibility = View.VISIBLE
            text = "Tap to open a design. Long-press to delete. Saved designs can be reused when ordering."
        }

        adapter = RowAdapter(this)
        val lv = findViewById<ListView>(R.id.lv)
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, pos, _ -> open(designs[pos]) }
        lv.setOnItemLongClickListener { _, _, pos, _ ->
            val d = designs[pos]
            AlertDialog.Builder(this).setTitle("Delete \"${d.title}\"?")
                .setPositiveButton("Delete") { _, _ -> db.deleteDesign(d.id, uid); refresh() }
                .setNegativeButton("Cancel", null).show()
            true
        }

        findViewById<Button>(R.id.btnAction).apply {
            visibility = View.VISIBLE
            text = "+ Upload new design"
            setOnClickListener { pickFile.launch(arrayOf("image/*", "application/pdf")) }
        }
        refresh()
    }

    private fun refresh() {
        designs = db.getDesigns(uid)
        adapter.update(designs.map {
            Row(it.id, it.title, (it.fileName.ifEmpty { "Text: ${it.customText}" }) + "\nSaved ${it.createdAt}",
                color = Color.parseColor("#7C3AED"), icon = "⭐")
        })
        findViewById<TextView>(R.id.tvEmpty).apply {
            text = "No saved designs yet."
            visibility = if (designs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun open(d: Design) {
        if (d.fileUri.isEmpty()) {
            AlertDialog.Builder(this).setTitle(d.title).setMessage(d.customText).setPositiveButton("OK", null).show()
            return
        }
        try {
            val uri = Uri.parse(d.fileUri)
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, contentResolver.getType(uri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: ActivityNotFoundException) {
            toast("No app found to open this file")
        } catch (e: SecurityException) {
            toast("File is no longer accessible")
        }
    }
}
