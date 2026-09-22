package com.example.printxpress

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat

class OffersActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_list, "Offers & Promotions")
        findViewById<TextView>(R.id.tvHint).apply { visibility = View.VISIBLE; text = "Tap an offer to copy its code, then paste it at checkout." }

        findViewById<SwitchCompat>(R.id.swToggle).apply {
            visibility = View.VISIBLE
            text = "Send me promotion notifications"
            isChecked = Session.promoEnabled(this@OffersActivity)
            setOnCheckedChangeListener { _, on ->
                Session.setPromoEnabled(this@OffersActivity, on)
                if (!on) Notifier.cancel(this@OffersActivity, Notifier.PROMO_ID)
                toast(if (on) "Promotion alerts on" else "Promotion alerts off")
            }
        }

        val promos = db.getPromos()
        val colors = listOf("#F97316", "#DC2626", "#059669", "#7C3AED")
        val adapter = RowAdapter(this, promos.mapIndexed { i, p ->
            Row(p.id, p.title,
                "${p.description}\nCode: ${p.code}" + if (p.minQty > 1) " • Min ${p.minQty} items" else "",
                "${p.discount}% OFF", Color.parseColor(colors[i % colors.size]), "🏷️")
        })
        val lv = findViewById<ListView>(R.id.lv)
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, pos, _ ->
            val code = promos[pos].code
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Promo code", code))
            toast("Code $code copied")
        }
    }
}
