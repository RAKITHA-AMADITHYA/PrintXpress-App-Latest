package com.example.printxpress

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CH_ORDERS = "orders"
    const val CH_PROMO = "promotions"

    /** Fixed id for the seasonal promo alert, so it can be replaced or cancelled. */
    const val PROMO_ID = 100000

    fun cancel(c: Context, id: Int) = NotificationManagerCompat.from(c).cancel(id)

    fun createChannels(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_ORDERS, "Order updates", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel(CH_PROMO, "Offers & promotions", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    @SuppressLint("MissingPermission")
    fun show(c: Context, channel: String, id: Int, title: String, text: String, orderId: Long = -1) {
        createChannels(c)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = if (orderId > 0)
            Intent(c, OrderDetailActivity::class.java).putExtra("orderId", orderId)
        else Intent(c, MainActivity::class.java)
        val pi = PendingIntent.getActivity(c, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(c).notify(id, n)
    }
}
