package com.example.skaner_kodow.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoritesViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private val _favProducts = MutableLiveData<Set<String>>()
    val favProducts: LiveData<Set<String>> get() = _favProducts

    private val _favPromotions = MutableLiveData<Set<String>>()
    val favPromotions: LiveData<Set<String>> get() = _favPromotions

    init {
        if (uid != null) observeFavorites()
    }

    private fun observeFavorites() {
        db.getReference("users/$uid/favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prods = snapshot.child("products").children.mapNotNull { it.key }.toSet()
                    val promos = snapshot.child("promotions").children.mapNotNull { it.key }.toSet()
                    _favProducts.postValue(prods)
                    _favPromotions.postValue(promos)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun toggleFavoriteProduct(id: String) {
        val ref = db.getReference("users/$uid/favorites/products/$id")
        ref.get().addOnSuccessListener { if (it.exists()) ref.removeValue() else ref.setValue(true) }
    }

    fun toggleFavoritePromotion(id: String) {
        val ref = db.getReference("users/$uid/favorites/promotions/$id")
        ref.get().addOnSuccessListener { if (it.exists()) ref.removeValue() else ref.setValue(true) }
    }
}
