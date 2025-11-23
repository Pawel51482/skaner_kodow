package com.example.skaner_kodow.ui.shoppinglists

data class ShoppingListItem(
    val id: String = "",
    val productId: String = "",
    val quantity: Int = 1,
    val checked: Boolean = false,
    val customName: String = "",
    val customImageUrl: String = "",
    val itemImageUrl: String = ""
)
