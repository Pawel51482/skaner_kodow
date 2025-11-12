package com.example.skaner_kodow

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var tvLogin: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.setLanguageCode("pl")

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // kolejność walidacji
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Proszę wypełnić wszystkie pola")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Nieprawidłowy adres e-mail")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showToast("Hasło jest zbyt krótkie (min. 6 znaków).")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showToast("Hasła się nie zgadzają")
                return@setOnClickListener
            }

            // Jeśli lokalna walidacja przeszła, dopiero wtedy wołamy Firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // po utworzeniu konta dopisz profil w /users/{uid} jeśli nie istnieje
                        createUserProfile()

                        showToast("Rejestracja udana")
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        showToast(polishAuthMessage(task.exception))
                    }
                }
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Tworzy wpis w /users/{uid} z email i role:"user"
    private fun createUserProfile() {
        val user = firebaseAuth.currentUser ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)

        usersRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    val data = mapOf(
                        "email" to (user.email ?: ""),
                        "role" to "user"
                    )
                    usersRef.setValue(data)
                }
            }
            .addOnFailureListener {
                // cicho ignorujemy błąd — nie blokujemy rejestracji
            }
    }

    private fun showToast(msg: String) {
        val t = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        t.setGravity(Gravity.CENTER, 0, 0)
        t.show()
    }

    // Tłumaczenie wyjątków na Polskie
    private fun polishAuthMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "Hasło jest zbyt krótkie (min. 6 znaków)"
            is FirebaseAuthInvalidCredentialsException -> "Nieprawidłowy adres e-mail"
            is FirebaseAuthUserCollisionException -> "Konto z tym adresem już istnieje"
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Nieprawidłowy adres e-mail"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Ten e-mail jest już zajęty"
                "ERROR_OPERATION_NOT_ALLOWED" -> "Operacja niedostępna. Skontaktuj się z administratorem"
                "ERROR_WEAK_PASSWORD" -> "Hasło jest zbyt krótkie (min. 6 znaków)"
                else -> "Wystąpił błąd: ${e.errorCode.replace('_',' ').lowercase()}"
            }
            else -> "Wystąpił nieoczekiwany błąd. Spróbuj ponownie."
        }
    }
}
