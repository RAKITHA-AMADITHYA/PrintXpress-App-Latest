package com.example.printxpress

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

class ProductsActivity : BaseActivity() {

    private lateinit var adapter: RowAdapter
    private lateinit var spCategory: Spinner
    private lateinit var tilSearch: TextInputLayout
    private val allLabel = "All categories"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_products, "Print Products")

        spCategory = findViewById(R.id.spCategory)
        tilSearch = findViewById(R.id.tilSearch)
        val lv = findViewById<ListView>(R.id.lv)

        val cats = listOf(allLabel) + db.getCategories()
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cats)
        intent.getStringExtra("category")?.let { c -> cats.indexOf(c).takeIf { it >= 0 }?.let { spCategory.setSelection(it) } }

        adapter = RowAdapter(this)
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, PlaceOrderActivity::class.java).putExtra("productId", adapter.getItem(pos).id))
        }

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        tilSearch.editText!!.doAfterTextChanged { refresh() }
        refresh()
    }

    private fun refresh() {
        val cat = spCategory.selectedItem?.toString()?.takeIf { it != allLabel }
        val products = db.getProducts(cat, tilSearch.editText!!.text.toString())
        adapter.update(products.map { p ->
            val materials = db.getOptions(p.id, "MATERIAL").joinToString(", ")
            val sizes = db.getOptions(p.id, "SIZE").joinToString(", ")
            Row(p.id, p.name, "${lkr(p.price)} / unit\n$materials\nSizes: $sizes", p.category, Color.parseColor(p.color))
        })
        findViewById<TextView>(R.id.tvEmpty).visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
    }
}
