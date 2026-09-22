package com.example.printxpress

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class OrderDetailActivity : BaseActivity() {

    private var orderId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reachable from a notification, possibly after logout – onStart() sends us to login.
        if (uid == -1L) return
        setupScreen(R.layout.activity_order_detail, "Order")
        orderId = intent.getLongExtra("orderId", -1)

        findViewById<Button>(R.id.btnReschedule).setOnClickListener { reschedule() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { cancel() }
        findViewById<Button>(R.id.btnAdvance).setOnClickListener { advance() }
        bind()
    }

    private fun bind() {
        val o = db.getOrder(orderId, uid)
        if (o == null) { toast("Order not found"); finish(); return }
        title = "Order #${o.id}"

        findViewById<TextView>(R.id.tvOrderTitle).text = "${o.productName} × ${o.quantity}"
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        tvStatus.text = o.status
        (tvStatus.background.mutate() as GradientDrawable).setColor(OrderStatus.color(o.status))

        // Status timeline
        val timeline = if (o.status == OrderStatus.CANCELLED) "❌  Order cancelled" else {
            val flow = OrderStatus.flow(o.deliveryMethod)
            val current = flow.indexOf(o.status)
            flow.mapIndexed { i, s -> (if (i <= current) "✅  " else "⚪  ") + s }.joinToString("\n")
        }
        findViewById<TextView>(R.id.tvTimeline).text = timeline

        findViewById<TextView>(R.id.tvDetails).text = buildString {
            append("Material: ${o.material}\n")
            append("Size: ${o.size}\n")
            append("Quantity: ${o.quantity}\n")
            append("Artwork: ${o.fileName.ifEmpty { "—" }}\n")
            append("Custom text: ${o.customText.ifEmpty { "—" }}\n")
            append("${o.deliveryMethod} date: ${o.scheduleDate}\n")
            append("${if (o.deliveryMethod == "Delivery") "Address" else "Pickup at"}: ${o.address}\n")
            append("Placed on: ${o.createdAt}\n")
            append("Total: ${lkr(o.total)}")
        }

        val modifiable = OrderStatus.canModify(o.status)
        findViewById<Button>(R.id.btnReschedule).isEnabled = modifiable
        findViewById<Button>(R.id.btnCancel).isEnabled = modifiable
        findViewById<TextView>(R.id.tvNote).text = when {
            o.status == OrderStatus.CANCELLED -> "This order was cancelled."
            modifiable -> "You can reschedule or cancel this order until printing begins."
            else -> "Printing has started, so this order can no longer be changed."
        }
        findViewById<Button>(R.id.btnAdvance).visibility =
            if (o.status != OrderStatus.CANCELLED && OrderStatus.next(o.status, o.deliveryMethod) != null) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun reschedule() {
        val o = db.getOrder(orderId, uid) ?: return
        if (!OrderStatus.canModify(o.status)) { toast("This order can no longer be rescheduled"); bind(); return }
        pickDate(this) { date ->
            db.rescheduleOrder(orderId, uid, date)
            Notifier.show(this, Notifier.CH_ORDERS, orderId.toInt(), "📅 Order #$orderId rescheduled",
                "New ${o.deliveryMethod.lowercase()} date: $date", orderId)
            toast("Rescheduled to $date")
            bind()
        }
    }

    private fun cancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel order?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Cancel order") { _, _ ->
                val o = db.getOrder(orderId, uid) ?: return@setPositiveButton
                if (!OrderStatus.canModify(o.status)) { toast("Printing has started – cannot cancel"); bind(); return@setPositiveButton }
                db.updateOrderStatus(orderId, uid, OrderStatus.CANCELLED)
                Notifier.show(this, Notifier.CH_ORDERS, orderId.toInt(), "Order #$orderId cancelled",
                    "Your order for ${o.productName} has been cancelled.", orderId)
                bind()
            }
            .setNegativeButton("Keep order", null)
            .show()
    }

    /** Simulates the print shop moving the order forward, so tracking and notifications can be demonstrated. */
    private fun advance() {
        val o = db.getOrder(orderId, uid) ?: return
        val next = OrderStatus.next(o.status, o.deliveryMethod) ?: return
        db.updateOrderStatus(orderId, uid, next)
        val msg = when (next) {
            OrderStatus.COMPLETED -> "🎉 Order #$orderId completed. Thank you for choosing PrintXpress!"
            "Ready for Pickup" -> "Your ${o.productName} order is ready for pickup at $PICKUP_LOCATION."
            "Out for Delivery" -> "Your ${o.productName} order is on its way."
            else -> "Your order is now: $next"
        }
        Notifier.show(this, Notifier.CH_ORDERS, orderId.toInt(), "Order #$orderId: $next", msg, orderId)
        bind()
    }

}
