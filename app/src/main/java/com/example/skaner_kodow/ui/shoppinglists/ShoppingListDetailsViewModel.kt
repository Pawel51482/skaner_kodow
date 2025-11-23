package com.example.skaner_kodow.ui.shoppinglists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.skaner_kodow.ui.products.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShoppingListDetailsViewModel(
    private val listId: String
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    private val _items = MutableLiveData<List<Pair<ShoppingListItem, Product>>>()
    val items: LiveData<List<Pair<ShoppingListItem, Product>>> get() = _items

    // Pobieramy itemy i dociągamy produkty z /products
    fun observeItems() {
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users/$uid/shoppingLists/$listId/items")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val itemSnaps = snapshot.children.toList()
                    if (itemSnaps.isEmpty()) {
                        _items.postValue(emptyList())
                        return
                    }

                    val result = mutableListOf<Pair<ShoppingListItem, Product>>()
                    var processed = 0

                    fun maybePost() {
                        if (processed == itemSnaps.size) {
                            _items.postValue(result)
                        }
                    }

                    itemSnaps.forEach { itemSnap ->
                        val baseItem = itemSnap.getValue(ShoppingListItem::class.java)
                            ?.copy(id = itemSnap.key ?: "")
                            ?: run {
                                processed++
                                maybePost()
                                return@forEach
                            }

                        // Własny produkt – brak powiązania z /products
                        if (baseItem.productId.isEmpty()) {
                            val pseudoProduct = Product(
                                id = "",
                                name = baseItem.customName.ifEmpty { "Produkt" },
                                barcode = "",
                                imageUrl = "",
                                description = "",
                                price = 0.0,
                                priceUpdatedAt = ""
                            )
                            result.add(baseItem to pseudoProduct)
                            processed++
                            maybePost()
                            return@forEach
                        }

                        // Produkt powiązany z /products/{productId}
                        db.getReference("products/${baseItem.productId}")
                            .get()
                            .addOnSuccessListener { prodSnap ->
                                val product = prodSnap.getValue(Product::class.java)
                                    ?.copy(id = prodSnap.key ?: "")
                                    ?: Product(
                                        id = "",
                                        name = baseItem.customName.ifEmpty { "Produkt" },
                                        barcode = "",
                                        imageUrl = "",
                                        description = "",
                                        price = 0.0,
                                        priceUpdatedAt = ""
                                    )

                                result.add(baseItem to product)
                                processed++
                                maybePost()
                            }
                            .addOnFailureListener {
                                val pseudoProduct = Product(
                                    id = "",
                                    name = baseItem.customName.ifEmpty { "Produkt" },
                                    barcode = "",
                                    imageUrl = "",
                                    description = "",
                                    price = 0.0,
                                    priceUpdatedAt = ""
                                )
                                result.add(baseItem to pseudoProduct)
                                processed++
                                maybePost()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Oznaczenie kupione/niekupione
    fun toggleChecked(itemId: String, value: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users/$uid/shoppingLists/$listId/items/$itemId/checked")
            .setValue(value)
    }

    // Dodanie własnego produktu (bez powiązania z /products)
    fun addCustomItem(name: String, quantity: Int) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.getReference("users/$uid/shoppingLists/$listId/items").push()

        val data = ShoppingListItem(
            id = ref.key ?: "",
            productId = "",     // brak powiązania z /products
            quantity = quantity,
            checked = false,
            customName = name,
            itemImageUrl = ""   // na początku brak własnego zdjęcia
        )

        ref.setValue(data)
    }

    // Update zmiany ilości
    fun updateQuantity(itemId: String, newQuantity: Int) {
        val uid = auth.currentUser?.uid ?: return
        if (newQuantity <= 0) return  // nie zapisujemy 0 ani ujemnych

        db.getReference("users/$uid/shoppingLists/$listId/items/$itemId/quantity")
            .setValue(newQuantity)
    }

    // Aktualizacja zdjęcia przypisanego do pozycji listy
    fun updateItemImage(itemId: String, url: String) {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users/$uid/shoppingLists/$listId/items/$itemId/itemImageUrl")
            .setValue(url)
    }
}
