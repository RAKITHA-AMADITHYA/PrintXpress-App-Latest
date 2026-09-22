package com.example.printxpress

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : BaseActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showPromoNotification()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (uid == -1L) { startActivity(Intent(this, LoginActivity::class.java)); finish(); return }
        setContentView(R.layout.activity_main)
        title = "PrintXpress"
        Notifier.createChannels(this)

        bind(R.id.btnProducts, ProductsActivity::class.java)
        bind(R.id.btnOrders, OrdersActivity::class.java)
        bind(R.id.btnOffers, OffersActivity::class.java)
        bind(R.id.btnGallery, GalleryActivity::class.java)
        bind(R.id.btnHelp, HelpActivity::class.java)
        bind(R.id.btnProfile, ProfileActivity::class.java)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else showPromoNotification()
    }

    override fun onResume() {
        super.onResume()
        if (uid == -1L) return
        val user = db.getUser(uid)
        findViewById<TextView>(R.id.tvGreeting).text = "Hi, ${user?.name?.substringBefore(" ") ?: "there"} 👋"

        val active = db.getOrders(uid).count { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
        findViewById<TextView>(R.id.tvSummary).text =
            if (active == 0) "No active orders. Ready to print something?"
            else "You have $active active order${if (active > 1) "s" else ""}."

    }

    /** One seasonal promotion alert per app launch (if the user allows promo notifications). */
    private fun showPromoNotification() {
        if (!Session.promoEnabled(this) || Session.promoShownThisRun) return
        Session.promoShownThisRun = true
        db.getPromos().randomOrNull()?.let {
            Notifier.show(this, Notifier.CH_PROMO, Notifier.PROMO_ID, "🎉 ${it.title}", "${it.description}. Use code ${it.code} at checkout.")
        }
    }

    private fun bind(id: Int, cls: Class<*>) =
        findViewById<View>(id).setOnClickListener { startActivity(Intent(this, cls)) }

    override fun onSupportNavigateUp(): Boolean = false
}
