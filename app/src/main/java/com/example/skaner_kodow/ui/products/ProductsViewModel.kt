package com.example.skaner_kodow.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

data class Product(
    val name: String = "",
    val barcode: String = "",
    val imageUrl: String = "",
    val description: String = ""
)

class ProductsViewModel : ViewModel() {

    private val _products = MutableLiveData<List<Product>>()

    private val _filteredProducts = MutableLiveData<List<Product>>()
    val filteredProducts: LiveData<List<Product>> get() = _filteredProducts

    fun fetchProducts() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("products")
        databaseRef.get().addOnSuccessListener { snapshot ->
            val productsList = snapshot.children.mapNotNull { it.getValue(Product::class.java) }
            _products.value = productsList
            _filteredProducts.value = productsList
        }.addOnFailureListener { exception ->
        }
    }

    fun filterProducts(query: String) {
        val filteredList = _products.value?.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
        }
        _filteredProducts.value = filteredList!!
    }
}
