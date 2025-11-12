package com.example.skaner_kodow.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.FragmentAddProductBinding
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private lateinit var viewModel: ProductsViewModel
    private var productId: String? = null
    private var currentImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddProductBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        val args = arguments
        productId = args?.getString("id")
        val name = args?.getString("name") ?: ""
        val barcode = args?.getString("barcode") ?: ""
        val description = args?.getString("description") ?: ""
        val imageUrl = args?.getString("imageUrl") ?: ""
        val price = args?.getDouble("price") ?: 0.0

        currentImageUrl = imageUrl

        // wypełniamy pola
        binding.editTextProductName.setText(name)
        binding.inputBarcode.setText(barcode)
        binding.inputDescription.setText(description)
        binding.editTextPrice.setText(if (price > 0) price.toString() else "")

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).into(binding.imageView)
        }

        // zmieniamy napis przycisku
        binding.buttonAddProduct.text = "Zapisz zmiany"
        binding.buttonAddProduct.setOnClickListener {
            saveChanges()
        }


        return binding.root
    }

    private fun saveChanges() {
        val id = productId
        if (id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Brak ID produktu", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.editTextProductName.text.toString().trim()
        val barcode = binding.inputBarcode.text.toString().trim()
        val description = binding.inputDescription.text.toString().trim()
        val priceText = binding.editTextPrice.text.toString().trim()
        val price = priceText.toDoubleOrNull() ?: 0.0
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Podaj nazwę", Toast.LENGTH_SHORT).show()
            return
        }

        val product = Product(
            id = id,
            name = name,
            barcode = barcode,
            imageUrl = currentImageUrl ?: "",
            description = description,
            price = price,
            priceUpdatedAt = today
        )

        FirebaseDatabase.getInstance()
            .getReference("products")
            .child(id)
            .setValue(product)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show()
            }
    }
}
