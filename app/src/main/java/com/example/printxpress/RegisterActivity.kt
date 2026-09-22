package com.example.printxpress

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : BaseActivity() {

    /** Reached before there is a session. */
    override val requiresLogin: Boolean get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen(R.layout.activity_register, "Register")

        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPhone = findViewById<TextInputLayout>(R.id.tilPhone)
        val tilPw = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirm = findViewById<TextInputLayout>(R.id.tilConfirm)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            listOf(tilName, tilEmail, tilPhone, tilPw, tilConfirm).forEach { it.error = null }
            cbTerms.error = null

            val name = tilName.editText!!.text.toString().trim()
            val email = tilEmail.editText!!.text.toString().trim().lowercase()
            val phone = Validator.normalizePhone(tilPhone.editText!!.text.toString().trim())
            val pw = tilPw.editText!!.text.toString()
            val confirm = tilConfirm.editText!!.text.toString()
            var ok = true

            if (!Validator.isName(name)) { tilName.error = "Enter 3–50 letters"; ok = false }

            if (!Validator.isEmail(email)) { tilEmail.error = "Enter a valid email"; ok = false }
            else if (db.isTaken("email", email)) { tilEmail.error = "This email is already registered"; ok = false }

            if (!Validator.isPhone(phone)) { tilPhone.error = "Enter a valid Sri Lankan mobile (07XXXXXXXX)"; ok = false }
            else if (db.isTaken("phone", phone)) { tilPhone.error = "This number is already registered"; ok = false }

            if (!Validator.isStrongPassword(pw)) { tilPw.error = "Min 8 characters with letters and numbers"; ok = false }
            if (confirm != pw || confirm.isEmpty()) { tilConfirm.error = "Passwords do not match"; ok = false }

            if (!cbTerms.isChecked) { cbTerms.error = "Required"; toast("Please accept the privacy policy"); ok = false }
            if (!ok) return@setOnClickListener

            val newId = db.registerUser(name, email, phone, Validator.hash(pw))
            if (newId == -1L) {
                toast("Registration failed. Please try again.")
            } else {
                Session.login(this, newId)
                toast("Welcome to PrintXpress, $name!")
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }
    }
}
