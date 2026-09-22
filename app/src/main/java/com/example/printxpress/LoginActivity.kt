package com.example.printxpress

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Session.userId(this) != -1L) { openMain(); return }   // already logged in
        setContentView(R.layout.activity_login)

        val db = DbHelper(this)
        val tilId = findViewById<TextInputLayout>(R.id.tilIdentifier)
        val tilPw = findViewById<TextInputLayout>(R.id.tilPassword)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val id = tilId.editText!!.text.toString().trim()
            val pw = tilPw.editText!!.text.toString()
            tilId.error = null; tilPw.error = null
            var ok = true

            if (id.isEmpty()) { tilId.error = "Email or phone is required"; ok = false }
            else if (!Validator.isEmail(id) && !Validator.isPhone(id)) {
                tilId.error = "Enter a valid email or phone (07XXXXXXXX)"; ok = false
            }
            if (pw.isEmpty()) { tilPw.error = "Password is required"; ok = false }
            if (!ok) return@setOnClickListener

            val userId = db.login(Validator.normalizeLogin(id), Validator.hash(pw))
            if (userId == -1L) {
                tilPw.error = "Incorrect email/phone or password"
            } else {
                Session.login(this, userId)
                openMain()
            }
        }

        findViewById<TextView>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
