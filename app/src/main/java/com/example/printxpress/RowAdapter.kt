package com.example.printxpress

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

data class Row(
    val id: Long,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
    val color: Int = Color.parseColor("#1E3A8A"),
    val icon: String? = null
)

/** One simple adapter reused by every list in the app. */
class RowAdapter(private val context: Context, rows: List<Row> = emptyList()) : BaseAdapter() {
    var rows: List<Row> = rows
        private set

    fun update(newRows: List<Row>) { rows = newRows; notifyDataSetChanged() }

    override fun getCount() = rows.size
    override fun getItem(position: Int): Row = rows[position]
    override fun getItemId(position: Int): Long = rows[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_row, parent, false)
        val r = rows[position]
        v.findViewById<TextView>(R.id.rowTitle).text = r.title
        v.findViewById<TextView>(R.id.rowSubtitle).text = r.subtitle

        val swatch = v.findViewById<TextView>(R.id.rowSwatch)
        swatch.text = r.icon ?: r.title.trim().take(1).uppercase()
        (swatch.background.mutate() as GradientDrawable).setColor(r.color)

        val badge = v.findViewById<TextView>(R.id.rowBadge)
        if (r.badge.isNullOrEmpty()) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            badge.text = r.badge
            (badge.background.mutate() as GradientDrawable).setColor(r.color)
        }
        return v
    }
}
