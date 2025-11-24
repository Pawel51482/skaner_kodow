package com.example.skaner_kodow.ui.products

import android.app.Activity
import android.content.Intent
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
import com.example.skaner_kodow.utils.ImagePreview
import com.example.skaner_kodow.utils.ImgurUploader
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private lateinit var viewModel: ProductsViewModel
    private var productId: String? = null

    // stare zdjęcie z bazy
    private var currentImageUrl: String? = null

    // nowe zdjęcie wybrane w edycji
    private var selectedImageUrl: String? = null

    private val PICK_IMAGE_REQUEST = 1

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

        // zapamiętujemy stare zdjęcie
        currentImageUrl = imageUrl
        selectedImageUrl = imageUrl

        // wypełniamy pola
        binding.editTextProductName.setText(name)
        binding.inputBarcode.setText(barcode)
        binding.inputDescription.setText(description)
        binding.editTextPrice.setText(if (price > 0) price.toString() else "")

        // podgląd starego zdjęcia, jeśli jest
        if (imageUrl.isNotEmpty()) {
            binding.imageView.visibility = View.VISIBLE

            Glide.with(this)
                .load(imageUrl)
                .into(binding.imageView)

            binding.imageView.setOnClickListener {
                val urlToShow = selectedImageUrl ?: currentImageUrl
                if (!urlToShow.isNullOrEmpty()) {
                    ImagePreview(urlToShow)
                        .show(parentFragmentManager, "edit_product_preview")
                }
            }
        } else {
            binding.imageView.visibility = View.GONE
        }

        // przycisk do wybrania nowego zdjęcia
        binding.buttonAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // zmieniamy napis przycisku
        binding.buttonAddProduct.text = "Zapisz zmiany"
        binding.buttonAddProduct.setOnClickListener {
            saveChanges()
        }

        return binding.root
    }

    // zapis zmian w produkcie
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

        val finalImageUrl = selectedImageUrl ?: currentImageUrl ?: ""

        val product = Product(
            id = id,
            name = name,
            barcode = barcode,
            imageUrl = finalImageUrl,
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

    // obsługa wyboru zdjęcia z galerii
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                val imageFile = getFileFromUri(imageUri)
                if (imageFile != null) {
                    ImgurUploader.uploadImage(
                        imageFile,
                        onSuccess = { imageUrl ->
                            activity?.runOnUiThread {
                                selectedImageUrl = imageUrl

                                binding.imageView.visibility = View.VISIBLE

                                Glide.with(this)
                                    .load(imageUrl)
                                    .into(binding.imageView)

                                binding.imageView.setOnClickListener {
                                    val urlToShow = selectedImageUrl ?: currentImageUrl
                                    if (!urlToShow.isNullOrEmpty()) {
                                        ImagePreview(urlToShow)
                                            .show(parentFragmentManager, "edit_product_preview")
                                    }
                                }

                                Toast.makeText(
                                    context,
                                    "Zdjęcie przesłane",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onError = { exception ->
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "Błąd przesyłania obrazu: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Nie udało się odczytać obrazu",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: android.net.Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile =
                File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
