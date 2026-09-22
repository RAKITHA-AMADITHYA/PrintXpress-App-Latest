package com.example.printxpress

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView

class OrdersActivity : BaseActivity() {

    private lateinit var adapter: RowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_list, "My Orders")
        findViewById<TextView>(R.id.tvHint).apply {
            visibility = View.VISIBLE
            text = "Tap an order to track, reschedule or cancel it."
        }
        adapter = RowAdapter(this)
        val lv = findViewById<ListView>(R.id.lv)
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, OrderDetailActivity::class.java).putExtra("orderId", adapter.getItem(pos).id))
        }
    }

    override fun onResume() {
        super.onResume()
        val orders = db.getOrders(uid)
        adapter.update(orders.map { o ->
            Row(o.id, "#${o.id} • ${o.productName}",
                "${o.quantity} pcs • ${o.deliveryMethod} on ${o.scheduleDate}\n${lkr(o.total)}",
                o.status, OrderStatus.color(o.status), "📦")
        })
        findViewById<TextView>(R.id.tvEmpty).apply {
            text = "No orders yet.\nBrowse products to place your first order."
            visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
