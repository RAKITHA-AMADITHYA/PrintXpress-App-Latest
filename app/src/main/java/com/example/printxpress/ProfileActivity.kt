package com.example.printxpress

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_profile, "My Profile")

        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPhone = findViewById<TextInputLayout>(R.id.tilPhone)

        db.getUser(uid)?.let {
            tilName.editText!!.setText(it.name)
            tilEmail.editText!!.setText(it.email)
            tilPhone.editText!!.setText(it.phone)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            listOf(tilName, tilEmail, tilPhone).forEach { it.error = null }
            val name = tilName.editText!!.text.toString().trim()
            val email = tilEmail.editText!!.text.toString().trim().lowercase()
            val phone = Validator.normalizePhone(tilPhone.editText!!.text.toString().trim())
            var ok = true
            if (!Validator.isName(name)) { tilName.error = "Enter 3–50 letters"; ok = false }
            if (!Validator.isEmail(email)) { tilEmail.error = "Enter a valid email"; ok = false }
            else if (db.isTaken("email", email, uid)) { tilEmail.error = "Email used by another account"; ok = false }
            if (!Validator.isPhone(phone)) { tilPhone.error = "Enter a valid mobile (07XXXXXXXX)"; ok = false }
            else if (db.isTaken("phone", phone, uid)) { tilPhone.error = "Number used by another account"; ok = false }
            if (!ok) return@setOnClickListener

            if (db.updateUser(uid, name, email, phone)) toast("Profile updated") else toast("Update failed")
        }

        findViewById<Button>(R.id.btnAddresses).setOnClickListener { startActivity(Intent(this, AddressesActivity::class.java)) }
        findViewById<Button>(R.id.btnDesigns).setOnClickListener { startActivity(Intent(this, DesignsActivity::class.java)) }
        findViewById<Button>(R.id.btnHistory).setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Log out?")
                .setPositiveButton("Log out") { _, _ ->
                    Session.logout(this)
                    startActivity(Intent(this, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                }
                .setNegativeButton("Stay", null).show()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<Button>(R.id.btnAddresses).text = "🏠 Delivery addresses (${db.getAddresses(uid).size})"
        findViewById<Button>(R.id.btnDesigns).text = "⭐ Saved designs (${db.getDesigns(uid).size})"
        findViewById<Button>(R.id.btnHistory).text = "📦 Order history (${db.getOrders(uid).size})"
    }
}
