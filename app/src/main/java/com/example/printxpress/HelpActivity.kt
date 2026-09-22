package com.example.printxpress

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout

class HelpActivity : BaseActivity() {

    private val faqs = listOf(
        "What file format should I upload?" to "PDF is best. We also accept AI, PSD, PNG and JPG. Images should be at least 300 DPI.",
        "What is bleed and why do I need it?" to "Bleed is 3 mm of extra artwork beyond the trim edge. It prevents white edges after cutting.",
        "Should I use RGB or CMYK?" to "Use CMYK. Printers use CMYK inks, so RGB colours (especially bright blues and greens) may look duller.",
        "Can I change or cancel my order?" to "Yes – from My Orders you can reschedule or cancel until your order reaches the Printing stage.",
        "How long does printing take?" to "Most orders are ready in 1–3 working days. Large banners and bulk orders may take longer.",
        "Do you deliver outside Colombo?" to "Yes, we deliver island-wide. A flat delivery fee of LKR 350 applies."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_help, "Guidelines & Support")

        // FAQs (tap to expand)
        val container = findViewById<LinearLayout>(R.id.faqContainer)
        faqs.forEach { (q, a) ->
            val v = layoutInflater.inflate(R.layout.item_faq, container, false)
            val tvQ = v.findViewById<TextView>(R.id.tvQ)
            val tvA = v.findViewById<TextView>(R.id.tvA)
            tvQ.text = "+  $q"
            tvA.text = a
            v.setOnClickListener {
                val open = tvA.visibility == View.VISIBLE
                tvA.visibility = if (open) View.GONE else View.VISIBLE
                tvQ.text = (if (open) "+  " else "–  ") + q
            }
            container.addView(v)
        }

        findViewById<Button>(R.id.btnCall).setOnClickListener {
            launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+94112345678")))
        }
        findViewById<Button>(R.id.btnEmail).setOnClickListener {
            launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@printxpress.lk"))
                .putExtra(Intent.EXTRA_SUBJECT, "PrintXpress design support"))
        }

        val tilSubject = findViewById<TextInputLayout>(R.id.tilSubject)
        val tilMessage = findViewById<TextInputLayout>(R.id.tilMessage)
        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            tilSubject.error = null; tilMessage.error = null
            val subject = tilSubject.editText!!.text.toString().trim()
            val message = tilMessage.editText!!.text.toString().trim()
            var ok = true
            if (subject.length !in 5..60) { tilSubject.error = "Subject must be 5–60 characters"; ok = false }
            if (message.length !in 15..500) { tilMessage.error = "Question must be 15–500 characters"; ok = false }
            if (!ok) return@setOnClickListener

            db.addQuery(uid, subject, message)
            tilSubject.editText!!.setText(""); tilMessage.editText!!.setText("")
            toast("Question sent. Our design team will reply within 24 hours.")
            showQueries()
        }
        showQueries()
    }

    private fun showQueries() {
        val list = db.getQueries(uid)
        findViewById<TextView>(R.id.tvMyQueries).text =
            if (list.isEmpty()) "" else "Your questions:\n" +
                list.joinToString("\n") { "• [${it.status}] ${it.subject} – ${it.createdAt}" }
    }

    private fun launch(i: Intent) {
        try { startActivity(i) } catch (e: ActivityNotFoundException) { toast("No app available for this action") }
    }
}
