package com.example.skaner_kodow

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.*

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToMainActivity()
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        // Logowanie użytkownika
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // lokalna walidacja, zanim pójdzie request do Firebase
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Proszę wprowadzić wszystkie dane")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Nieprawidłowy adres e-mail")
                return@setOnClickListener
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showToast("Zalogowano pomyślnie")
                        navigateToMainActivity()
                    } else {
                        showToast(polishAuthMessage(task.exception))
                    }
                }
        }

        // Przejście do rejestracji
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Resetowanie hasła
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                showToast("Podaj adres e-mail, aby zresetować hasło")
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Nieprawidłowy adres e-mail")
            } else {
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        showToast("Wysłano link resetujący hasło na $email")
                    }
                    .addOnFailureListener { e ->
                        showToast("Błąd przy wysyłaniu linku: ${e.message}")
                    }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        val t = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        t.setGravity(Gravity.CENTER, 0, 0)
        t.show()
    }

    // Tłumaczenie wyjątków
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
