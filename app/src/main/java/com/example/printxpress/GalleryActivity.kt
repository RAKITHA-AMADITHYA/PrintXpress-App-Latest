package com.example.printxpress

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class GalleryActivity : BaseActivity() {

    private data class Sample(val title: String, val category: String, val desc: String, val color: String, val icon: String)

    private val samples = listOf(
        Sample("Minimal Corporate Card", "Business Cards", "Clean navy and white layout with the logo on the left.", "#1E3A8A", "💼"),
        Sample("Gold Foil Luxury Card", "Business Cards", "Black card with gold foil name – ideal for premium brands.", "#B8860B", "✨"),
        Sample("Avurudu Sale Flyer", "Flyers", "Bright festive flyer with traditional patterns.", "#DC2626", "🌞"),
        Sample("Grand Opening Poster", "Posters", "Bold headline poster with large date and venue.", "#7C3AED", "🎉"),
        Sample("Vesak Lantern Stickers", "Stickers", "Round die-cut stickers with lantern artwork.", "#F59E0B", "🏮"),
        Sample("Shop Front Banner", "Banners", "Wide banner with logo, phone number and services.", "#059669", "🏪"),
        Sample("Team Event T-Shirt", "T-Shirts", "Front logo and back name print for sports teams.", "#0EA5E9", "👕"),
        Sample("Photo Memory Mug", "Mugs", "Wrap-around family photo with a custom message.", "#DB2777", "☕")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_list, "Design Gallery")
        findViewById<TextView>(R.id.tvHint).apply {
            visibility = View.VISIBLE
            text = "Sample designs for inspiration. Tap one to order in that style."
        }
        val lv = findViewById<ListView>(R.id.lv)
        lv.adapter = RowAdapter(this, samples.mapIndexed { i, s ->
            Row(i.toLong(), s.title, s.desc, s.category, Color.parseColor(s.color), s.icon)
        })
        lv.setOnItemClickListener { _, _, pos, _ ->
            val s = samples[pos]
            AlertDialog.Builder(this)
                .setTitle("${s.icon}  ${s.title}")
                .setMessage("${s.desc}\n\nCategory: ${s.category}")
                .setPositiveButton("Order this style") { _, _ ->
                    startActivity(Intent(this, ProductsActivity::class.java).putExtra("category", s.category))
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}
