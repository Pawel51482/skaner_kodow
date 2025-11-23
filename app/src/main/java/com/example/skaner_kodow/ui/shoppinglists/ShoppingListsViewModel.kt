package com.example.skaner_kodow.ui.shoppinglists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShoppingList(
    val id: String = "",
    val name: String = "",
    val createdAt: String = "",
    val itemsCount: Int = 0
)

class ShoppingListsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    private val _lists = MutableLiveData<List<ShoppingList>>()
    val lists: LiveData<List<ShoppingList>> get() = _lists

    // Nasłuchuje /users/{uid}/shoppingLists i aktualizuje liste
    fun observeShoppingLists() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users/$uid/shoppingLists")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val result = snapshot.children.map { listSnap ->
                        val id = listSnap.key ?: ""
                        val name = listSnap.child("name").getValue(String::class.java) ?: ""
                        val createdAt =
                            listSnap.child("createdAt").getValue(String::class.java) ?: ""
                        val itemsCount = listSnap.child("items").childrenCount.toInt()

                        ShoppingList(
                            id = id,
                            name = name,
                            createdAt = createdAt,
                            itemsCount = itemsCount
                        )
                    }.sortedBy { it.createdAt } // po dacie

                    _lists.postValue(result)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                }
            })
    }

    // Dodaje nową listę zakupową
    fun addShoppingList(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.getReference("users/$uid/shoppingLists").push()

        val now = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("pl", "PL"))
            .format(Date())

        val data = mapOf(
            "name" to name,
            "createdAt" to now
        )

        ref.setValue(data)
    }

    fun deleteList(listId: String) {
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users/$uid/shoppingLists/$listId")
            .removeValue()
    }

}
