package com.example.skaner_kodow.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

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
}
