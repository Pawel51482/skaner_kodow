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
        // zakładamy, że promotion.id nie jest pusty
        val id = promotion.id
        if (id.isEmpty()) return

        databaseRef.child(id).setValue(promotion)
    }
}
