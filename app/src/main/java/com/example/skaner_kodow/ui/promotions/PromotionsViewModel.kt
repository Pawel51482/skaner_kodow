package com.example.skaner_kodow.ui.promotions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PromotionsViewModel : ViewModel() {

    private val databaseRef: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("promotions")

    private val _promotions = MutableLiveData<List<Promotion>>()
    val promotions: LiveData<List<Promotion>> get() = _promotions

    init {
        fetchPromotions()
    }

    // Pobiera wszystkie promocje w czasie rzeczywistym
    private fun fetchPromotions() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Promotion>()
                for (child in snapshot.children) {
                    val promo = child.getValue(Promotion::class.java)
                    if (promo != null) {
                        list.add(promo.copy(id = child.key ?: ""))
                    }
                }
                _promotions.value = list.reversed() // najnowsze na górze
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // Dodaje nową promocję do bazy
    fun addPromotion(
        title: String,
        description: String,
        barcode: String,
        startDate: String,
        endDate: String,
        imageUrl: String = ""
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val id = databaseRef.push().key ?: return
        val promotion = Promotion(
            id = id,
            title = title,
            description = description,
            barcode = barcode,
            startDate = startDate,
            endDate = endDate,
            addedBy = uid,
            imageUrl = imageUrl
        )

        databaseRef.child(id).setValue(promotion)
    }

    //usuwanie
    fun deletePromotion(id: String) {
        FirebaseDatabase.getInstance()
            .getReference("promotions")
            .child(id)
            .removeValue()
    }

    //edycja
    fun updatePromotion(promotion: Promotion) {
        val id = promotion.id
        if (id.isEmpty()) return

        databaseRef.child(id).setValue(promotion)
    }


    // Ulubione
    private val favAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val favDb = com.google.firebase.database.FirebaseDatabase.getInstance()

    private val _favPromotions = androidx.lifecycle.MutableLiveData<Set<String>>()
    val favPromotions: androidx.lifecycle.LiveData<Set<String>> get() = _favPromotions

    fun observeFavoritePromotions() {
        val uid = favAuth.currentUser?.uid ?: return
        favDb.getReference("users/$uid/favorites/promotions")
            .addValueEventListener(object: com.google.firebase.database.ValueEventListener {
                override fun onDataChange(s: com.google.firebase.database.DataSnapshot) {
                    _favPromotions.postValue(s.children.mapNotNull { it.key }.toSet())
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    fun toggleFavoritePromotion(promoId: String) {
        val uid = favAuth.currentUser?.uid ?: return
        val ref = favDb.getReference("users/$uid/favorites/promotions/$promoId")
        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) ref.removeValue() else ref.setValue(true)
        }
    }


}
