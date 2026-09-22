package com.example.printxpress

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class AddressesActivity : BaseActivity() {

    private lateinit var adapter: RowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_list, "Delivery Addresses")
        findViewById<TextView>(R.id.tvHint).apply { visibility = View.VISIBLE; text = "Long-press an address to delete it." }

        adapter = RowAdapter(this)
        val lv = findViewById<ListView>(R.id.lv)
        lv.adapter = adapter
        lv.setOnItemLongClickListener { _, _, pos, _ ->
            val row = adapter.getItem(pos)
            AlertDialog.Builder(this).setTitle("Delete \"${row.title}\"?")
                .setPositiveButton("Delete") { _, _ -> db.deleteAddress(row.id, uid); refresh() }
                .setNegativeButton("Cancel", null).show()
            true
        }

        findViewById<Button>(R.id.btnAction).apply {
            visibility = View.VISIBLE
            text = "+ Add address"
            setOnClickListener { showAddAddressDialog(this@AddressesActivity, db, uid) { refresh() } }
        }
        refresh()
    }

    private fun refresh() {
        val list = db.getAddresses(uid)
        adapter.update(list.map { Row(it.id, it.label, it.address, icon = "🏠") })
        findViewById<TextView>(R.id.tvEmpty).apply {
            text = "No saved addresses yet."
            visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
