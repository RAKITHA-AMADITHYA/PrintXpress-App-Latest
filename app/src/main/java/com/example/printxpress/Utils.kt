package com.example.printxpress

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale

const val MAX_FILE_BYTES = 25L * 1024 * 1024
const val PICKUP_LOCATION = "PrintXpress Store, Colombo 03"
const val DELIVERY_FEE = 350.0

/** Base screen: gives every activity the database, the logged-in user id and a back arrow. */
open class BaseActivity : AppCompatActivity() {
    val db by lazy { DbHelper(this) }
    val uid: Long get() = Session.userId(this)

    /** Screens that must work before sign-in (Register) override this. */
    protected open val requiresLogin: Boolean get() = true

    /**
     * Nobody should reach a personal screen without a session – a notification tapped
     * after logging out would otherwise open the previous user's data.
     */
    override fun onStart() {
        super.onStart()
        if (requiresLogin && uid == -1L && !isFinishing) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }

    fun setupScreen(layout: Int, screenTitle: String) {
        setContentView(layout)
        title = screenTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

/**
 * ACTION_OPEN_DOCUMENT that also asks for a *persistable* read grant.
 * Without this flag takePersistableUriPermission() throws, so saved artwork
 * stops opening once the app is restarted.
 */
class OpenPersistableDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
}

object Session {
    private const val PREFS = "printxpress_session"
    var promoShownThisRun = false

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun login(c: Context, id: Long) = prefs(c).edit().putLong("uid", id).apply()
    fun userId(c: Context): Long = prefs(c).getLong("uid", -1L)
    fun logout(c: Context) { prefs(c).edit().remove("uid").apply(); promoShownThisRun = false }
    fun promoEnabled(c: Context) = prefs(c).getBoolean("promo", true)
    fun setPromoEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean("promo", on).apply()
}

object Validator {
    private val PHONE = Regex("^(?:\\+94|0)7\\d{8}$")
    private val NAME = Regex("^[A-Za-z .'-]{3,50}$")
    private const val SALT = "PrintXpress#2026"

    fun isEmail(s: String) = Patterns.EMAIL_ADDRESS.matcher(s).matches()
    fun isPhone(s: String) = PHONE.matches(s.replace(" ", ""))
    fun isName(s: String) = NAME.matches(s)
    fun isStrongPassword(s: String) = s.length >= 8 && s.any { it.isDigit() } && s.any { it.isLetter() }
    fun normalizePhone(s: String): String {
        val p = s.replace(" ", "")
        return if (p.startsWith("+94")) "0" + p.substring(3) else p
    }
    fun normalizeLogin(s: String) = if (isEmail(s)) s.lowercase() else normalizePhone(s)
    fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256").digest((SALT + password).toByteArray())
            .joinToString("") { "%02x".format(it) }
}

object OrderStatus {
    const val PENDING = "Pending"
    const val PROCESSING = "Processing"
    const val PRINTING = "Printing"
    const val COMPLETED = "Completed"
    const val CANCELLED = "Cancelled"

    fun flow(method: String) = listOf(
        PENDING, PROCESSING, PRINTING,
        if (method == "Delivery") "Out for Delivery" else "Ready for Pickup",
        COMPLETED
    )

    /** Orders can be changed only before printing begins. */
    fun canModify(status: String) = status == PENDING || status == PROCESSING

    fun next(status: String, method: String): String? {
        val f = flow(method)
        val i = f.indexOf(status)
        return if (i in 0 until f.size - 1) f[i + 1] else null
    }

    fun color(status: String): Int = Color.parseColor(
        when (status) {
            PENDING -> "#F59E0B"
            PROCESSING -> "#3B82F6"
            PRINTING -> "#8B5CF6"
            COMPLETED -> "#10B981"
            CANCELLED -> "#EF4444"
            else -> "#0EA5E9"
        }
    )
}

fun lkr(v: Double) = "LKR " + String.format(Locale.US, "%,.2f", v)

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

/** Date picker limited to tomorrow .. 60 days ahead. Returns yyyy-MM-dd. */
fun pickDate(c: Context, onPicked: (String) -> Unit) {
    // Both bounds must sit at midnight: DatePicker compares raw milliseconds, so a
    // bound carrying the current time of day makes that whole day unselectable.
    fun dayStart(daysAhead: Int) = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, daysAhead)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val min = dayStart(1)
    val max = dayStart(60)
    val dlg = DatePickerDialog(c, { _, y, m, d ->
        onPicked(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
    }, min.get(Calendar.YEAR), min.get(Calendar.MONTH), min.get(Calendar.DAY_OF_MONTH))
    dlg.datePicker.minDate = min.timeInMillis
    dlg.datePicker.maxDate = max.timeInMillis
    dlg.show()
}

/** Returns (display name, size in bytes or -1). */
fun queryFileInfo(c: Context, uri: Uri): Pair<String, Long> {
    var name = "artwork"
    var size = -1L
    c.contentResolver.query(uri, null, null, null, null)?.use { cur ->
        if (cur.moveToFirst()) {
            val ni = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (ni >= 0 && !cur.isNull(ni)) name = cur.getString(ni)
            val si = cur.getColumnIndex(OpenableColumns.SIZE)
            if (si >= 0 && !cur.isNull(si)) size = cur.getLong(si)
        }
    }
    if (size < 0) {
        // Some providers leave SIZE out; ask the descriptor so the 25 MB limit still applies.
        size = try {
            c.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (e: Exception) { -1L }
    }
    return name to size
}

/** Dialog with validation for adding a delivery address. */
fun showAddAddressDialog(act: Activity, db: DbHelper, userId: Long, onAdded: () -> Unit) {
    val etLabel = EditText(act).apply { hint = "Label (e.g. Home, Office)"; setSingleLine() }
    val etAddr = EditText(act).apply { hint = "Full address incl. city"; minLines = 2 }
    val box = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(act, 20), dp(act, 8), dp(act, 20), 0)
        addView(etLabel); addView(etAddr)
    }
    val dlg = AlertDialog.Builder(act).setTitle("Add delivery address").setView(box)
        .setPositiveButton("Save", null).setNegativeButton("Cancel", null).create()
    dlg.setOnShowListener {
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val label = etLabel.text.toString().trim()
            val addr = etAddr.text.toString().trim()
            when {
                label.length !in 2..20 -> etLabel.error = "Enter a label (2–20 characters)"
                addr.length !in 10..200 -> etAddr.error = "Enter a complete address (10–200 characters)"
                else -> { db.addAddress(userId, label, addr); dlg.dismiss(); onAdded() }
            }
        }
    }
    dlg.show()
}

/** Single-field text dialog with 3–40 character validation. */
fun promptText(act: Activity, dialogTitle: String, hintText: String, prefill: String, onOk: (String) -> Unit) {
    val et = EditText(act).apply { hint = hintText; setText(prefill); setSingleLine() }
    val box = LinearLayout(act).apply {
        setPadding(dp(act, 20), dp(act, 8), dp(act, 20), 0)
        addView(et, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
    val dlg = AlertDialog.Builder(act).setTitle(dialogTitle).setView(box)
        .setPositiveButton("Save", null).setNegativeButton("Cancel", null).create()
    dlg.setOnShowListener {
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val v = et.text.toString().trim()
            if (v.length !in 3..40) et.error = "Enter 3–40 characters" else { dlg.dismiss(); onOk(v) }
        }
    }
    dlg.show()
}
