package com.example.skaner_kodow.ui.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

data class AccountUser(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "",
    val photoUrl: String = ""
)

class AccountViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().getReference("users")

    private val _user = MutableLiveData<AccountUser>()
    val user: LiveData<AccountUser> get() = _user

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    // Pobiera dane zalogowanego użytkownika z bazy
    fun loadUserData() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        db.child(uid).get().addOnSuccessListener { snap ->
            val uname = snap.child("username").getValue(String::class.java).orEmpty()
            val role = snap.child("role").getValue(String::class.java) ?: "user"
            val photoUrl = snap.child("photoUrl").getValue(String::class.java).orEmpty()

            _user.value = AccountUser(
                uid = uid,
                username = if (uname.isNotBlank()) uname else "Anonim",
                email = user.email.orEmpty(),
                role = role,
                photoUrl = photoUrl
            )
        }
    }


    fun updateProfile(newUsername: String, newPhotoUrl: String?, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        _loading.value = true

        val finalName = if (newUsername.isBlank()) "Anonim" else newUsername

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(finalName)
            .apply {
                if (newPhotoUrl != null) setPhotoUri(android.net.Uri.parse(newPhotoUrl))
            }
            .build()

        user.updateProfile(profileUpdate)
            .addOnSuccessListener {
                val updates = mutableMapOf<String, Any>(
                    "username" to finalName,
                    "updatedAt" to SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                )
                if (newPhotoUrl != null) updates["photoUrl"] = newPhotoUrl

                db.child(user.uid).updateChildren(updates)
                    .addOnCompleteListener { task ->
                        _loading.value = false
                        onComplete(task.isSuccessful)
                    }
            }
            .addOnFailureListener {
                _loading.value = false
                onComplete(false)
            }
    }

    // wysyła mail do resetu hasła
    fun resetPassword(onComplete: (Boolean) -> Unit) {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            onComplete(false)
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun logout() {
        auth.signOut()
    }
}
