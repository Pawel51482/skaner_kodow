package com.example.skaner_kodow.ui.promotions

data class Promotion(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val barcode: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val addedBy: String = "",
    val imageUrl: String = ""
)
