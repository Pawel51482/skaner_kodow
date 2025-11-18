package com.example.skaner_kodow.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Product(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val priceUpdatedAt: String = ""
)

class ProductsViewModel : ViewModel() {

    private val _products = MutableLiveData<List<Product>>()
    private val _filteredProducts = MutableLiveData<List<Product>>()
    val filteredProducts: LiveData<List<Product>> get() = _filteredProducts

    private val ref = FirebaseDatabase.getInstance().getReference("products")

    fun fetchProducts() {
        ref.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.children.map { snap ->
                    val p = snap.getValue(Product::class.java)
                    // wstrzykujemy klucz jako id
                    p?.copy(id = snap.key ?: "")
                }.filterNotNull()

                _products.value = list
                _filteredProducts.value = list
            }
    }

    fun filterProducts(query: String) {
        val base = _products.value ?: emptyList()
        _filteredProducts.value = base.filter {
            it.name.contains(query, true) ||
                    it.description.contains(query, true)
        }
    }

    fun deleteProduct(id: String) {
        if (id.isEmpty()) return
        ref.child(id).removeValue()
    }


    // Ulubione
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    private val _favProducts = MutableLiveData<Set<String>>()
    val favProducts: LiveData<Set<String>> get() = _favProducts

    // Nasłuchuje produkty i aktualizuje ulubione
    fun observeFavoriteProducts() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users/$uid/favorites/products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    _favProducts.postValue(s.children.mapNotNull { it.key }.toSet())
                }
                override fun onCancelled(error: DatabaseError) { /* no-op */ }
            })
    }

    // przełącza stan ulubionego dla danego productID
    fun toggleFavoriteProduct(productId: String) {
        val uid = auth.currentUser?.uid ?: return
        val favRef = db.getReference("users/$uid/favorites/products/$productId")
        favRef.get().addOnSuccessListener { snap ->
            if (snap.exists()) favRef.removeValue() else favRef.setValue(true)
        }
    }
}
