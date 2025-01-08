package com.example.skaner_kodow.ui.products

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skaner_kodow.BarcodeScannerActivity
import com.example.skaner_kodow.databinding.FragmentAddProductBinding
import com.example.skaner_kodow.utils.ImgurUploader
import com.google.firebase.database.FirebaseDatabase
import java.io.File

class AddProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUrl: String? = null
    private val SCAN_BARCODE_REQUEST = 2


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddProductBinding.inflate(inflater, container, false)

        binding.buttonScanBarcode.setOnClickListener {
            val intent = Intent(context, BarcodeScannerActivity::class.java)
            startActivityForResult(intent, SCAN_BARCODE_REQUEST)
        }


        binding.buttonAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.buttonAddProduct.setOnClickListener {
            val productName = binding.editTextProductName.text.toString()
            val productBarcode = binding.inputBarcode.text.toString()
            val productDescription = binding.inputDescription.text.toString()
            if (productName.isNotEmpty() && selectedImageUrl != null) {
                saveProductToDatabase(
                    Product(
                        name = productName,
                        barcode = productBarcode,
                        imageUrl = selectedImageUrl!!,
                        description = productDescription
                    )
                )
            } else {
                Toast.makeText(context, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_BARCODE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val scannedCode = data.getStringExtra("scanned_code")
            if (scannedCode != null) {
                binding.inputBarcode.setText(scannedCode) // Wstaw zeskanowany kod
            } else {
                Toast.makeText(context, "Nie zeskanowano kodu", Toast.LENGTH_SHORT).show()
            }
        }


        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                val imageFile = getFileFromUri(imageUri)
                if (imageFile != null) {
                    ImgurUploader.uploadImage(imageFile,
                        onSuccess = { imageUrl ->
                            activity?.runOnUiThread {
                                Glide.with(this)
                                    .load(imageUrl)
                                    .into(binding.imageView)
                                selectedImageUrl = imageUrl
                            }
                        },
                        onError = { exception ->
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Błąd przesyłania obrazu: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    Toast.makeText(context, "Nie udało się odczytać obrazu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun getFileFromUri(uri: android.net.Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun saveProductToDatabase(product: Product) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("products")
        val productId = productsRef.push().key

        if (productId != null) {
            productsRef.child(productId).setValue(product)
                .addOnSuccessListener {
                    Toast.makeText(context, "Produkt dodany!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Błąd podczas dodawania produktu", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Błąd: nie udało się dodać produktu", Toast.LENGTH_SHORT).show()
        }
    }
}
