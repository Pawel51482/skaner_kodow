package com.example.skaner_kodow

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tvAuthMessage: TextView
    private lateinit var progress: ProgressBar

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.setLanguageCode("pl")

        setContentView(R.layout.activity_register)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        tvAuthMessage = findViewById(R.id.tvAuthMessage)
        progress = findViewById(R.id.progressRegister)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        etEmail.doAfterTextChanged { clearErrors() }
        etPassword.doAfterTextChanged { clearErrors() }
        etConfirmPassword.doAfterTextChanged { clearErrors() }

        btnRegister.setOnClickListener { attemptRegister() }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun attemptRegister() {
        clearErrors()

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        var ok = true

        if (email.isEmpty()) {
            tilEmail.error = "Podaj adres e-mail"
            ok = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Nieprawidłowy adres e-mail"
            ok = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "Podaj hasło"
            ok = false
        } else if (password.length < 6) {
            tilPassword.error = "Hasło musi mieć min. 6 znaków"
            ok = false
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Powtórz hasło"
            ok = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Hasła nie są takie same"
            ok = false
        }

        if (!ok) return

        setLoading(true)

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    createUserProfile()
                    Snackbar.make(findViewById(R.id.rootRegister), "Konto utworzone. Możesz się zalogować.", Snackbar.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    showFormMessage(polishAuthMessage(task.exception))
                }
            }
    }

    // Tworzy wpis w /users/{uid} z email i role:"user"
    private fun createUserProfile() {
        val user = firebaseAuth.currentUser ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)

        usersRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    val map = hashMapOf(
                        "email" to (user.email ?: ""),
                        "role" to "user"
                    )
                    usersRef.setValue(map)
                }
            }
            .addOnFailureListener {
                // celowo bez blokowania rejestracji
            }
    }

    private fun setLoading(loading: Boolean) {
        btnRegister.isEnabled = !loading
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
        tvAuthMessage.visibility = View.GONE
        tvAuthMessage.text = ""
    }

    private fun showFormMessage(msg: String) {
        tvAuthMessage.text = msg
        tvAuthMessage.visibility = View.VISIBLE
    }

    // Tłumaczenie wyjątków na polskie
    private fun polishAuthMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "Hasło jest zbyt krótkie (min. 6 znaków)"
            is FirebaseAuthInvalidCredentialsException -> "Nieprawidłowy adres e-mail"
            is FirebaseAuthUserCollisionException -> "Konto z tym adresem już istnieje"
            is FirebaseAuthException -> "Rejestracja nie powiodła się. Spróbuj ponownie."
            else -> "Rejestracja nie powiodła się. Spróbuj ponownie."
        }
    }
}
