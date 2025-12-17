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
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tvAuthMessage: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tvAuthMessage = findViewById(R.id.tvAuthMessage)
        progress = findViewById(R.id.progressLogin)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        etEmail.doAfterTextChanged { clearErrors() }
        etPassword.doAfterTextChanged { clearErrors() }

        btnLogin.setOnClickListener { attemptLogin() }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener { attemptResetPassword() }
    }

    private fun attemptLogin() {
        clearErrors()

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

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
        }

        if (!ok) return

        setLoading(true)

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    // bez Toastów
                    Snackbar.make(findViewById(R.id.rootLogin), "Zalogowano pomyślnie", Snackbar.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    showFormMessage(polishAuthMessage(task.exception))
                }
            }
    }

    private fun attemptResetPassword() {
        clearErrors()

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            tilEmail.error = "Podaj adres e-mail, aby zresetować hasło"
            etEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Nieprawidłowy adres e-mail"
            etEmail.requestFocus()
            return
        }

        setLoading(true)

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                setLoading(false)
                Snackbar.make(findViewById(R.id.rootLogin), "Wysłano link resetujący na $email", Snackbar.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                setLoading(false)
                showFormMessage("Nie udało się wysłać linku resetującego. Spróbuj ponownie.")
            }
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilPassword.error = null
        tvAuthMessage.visibility = View.GONE
        tvAuthMessage.text = ""
    }

    private fun showFormMessage(msg: String) {
        tvAuthMessage.text = msg
        tvAuthMessage.visibility = View.VISIBLE
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // tłumaczenie wyjątków Firebase na PL
    private fun polishAuthMessage(e: Exception?): String {
        val code = when (e) {
            is FirebaseAuthInvalidCredentialsException -> "ERROR_INVALID_CREDENTIALS"
            is FirebaseAuthInvalidUserException -> e.errorCode
            is FirebaseAuthException -> e.errorCode
            else -> null
        }

        return when (code) {
            "ERROR_INVALID_EMAIL" -> "Nieprawidłowy adres e-mail"
            "ERROR_INVALID_CREDENTIALS" -> "Nieprawidłowe dane logowania"
            "ERROR_USER_NOT_FOUND" -> "Nie znaleziono użytkownika z tym adresem e-mail"
            "ERROR_WRONG_PASSWORD" -> "Nieprawidłowe hasło"
            "ERROR_USER_DISABLED" -> "Konto zostało dezaktywowane"
            else -> "Logowanie nie powiodło się. Spróbuj ponownie."
        }
    }
}
