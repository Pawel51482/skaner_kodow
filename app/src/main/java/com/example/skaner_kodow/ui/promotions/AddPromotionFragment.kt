package com.example.skaner_kodow.ui.promotions

import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skaner_kodow.BarcodeScannerActivity
import com.example.skaner_kodow.databinding.FragmentAddPromotionBinding
import com.example.skaner_kodow.utils.ImgurUploader
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class AddPromotionFragment : Fragment() {

    private lateinit var binding: FragmentAddPromotionBinding
    private lateinit var viewModel: PromotionsViewModel

    private var selectedImageUrl: String? = null
    private val productsRef = FirebaseDatabase.getInstance().getReference("products")

    // SKANER
    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val code = result.data?.getStringExtra("scanned_code")
                if (code != null) {
                    binding.etProductBarcode.setText(code)
                    // po skanowaniu spróbuj znaleźć obrazek w produktach
                    findImageInProductsLocally(code)
                }
            }
        }

    // GALERIA
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                uploadImageToImgur(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddPromotionBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(PromotionsViewModel::class.java)

        // daty
        binding.etStartDate.setOnClickListener {
            showDatePicker { d -> binding.etStartDate.setText(d) }
        }
        binding.etEndDate.setOnClickListener {
            showDatePicker { d -> binding.etEndDate.setText(d) }
        }

        // dodaj
        binding.btnAddPromotion.setOnClickListener { addPromotion() }

        // aparat przy kodzie
        binding.btnScanBarcode.setOnClickListener {
            val intent = Intent(requireContext(), BarcodeScannerActivity::class.java)
            scanLauncher.launch(intent)
        }

        // jak ktoś wpisze ręcznie
        binding.etProductBarcode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val code = binding.etProductBarcode.text.toString().trim()
                findImageInProductsLocally(code)
            }
        }

        // przycisk "wybierz zdjęcie"
        binding.btnPickImage?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // schowaj podgląd i progress jeśli są
        binding.ivImagePreview?.visibility = View.GONE
        binding.progressImageUpload?.visibility = View.GONE

        return binding.root
    }

    private fun addPromotion() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val barcode = binding.etProductBarcode.text.toString().trim()
        val start = binding.etStartDate.text.toString().trim()
        val end = binding.etEndDate.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || barcode.isEmpty() ||
            start.isEmpty() || end.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        // zapisujemy to co mamy w selectedImageUrl (albo pusty string)
        viewModel.addPromotion(
            title,
            description,
            barcode,
            start,
            end,
            selectedImageUrl ?: ""
        )
        Toast.makeText(requireContext(), "Promocja dodana!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    // SZUKANIE OBRAZKA W /products
    private fun findImageInProductsLocally(barcode: String) {
        if (barcode.isEmpty()) return

        productsRef.get()
            .addOnSuccessListener { snap ->
                var urlFound: String? = null
                for (child in snap.children) {
                    val childBarcode = child.child("barcode").getValue(String::class.java)
                    if (childBarcode == barcode) {
                        urlFound =
                            child.child("imageUrl").getValue(String::class.java)
                                ?: child.child("imageURL").getValue(String::class.java)
                                        ?: child.child("photoUrl").getValue(String::class.java)
                        break
                    }
                }

                if (urlFound != null) {
                    selectedImageUrl = urlFound
                    showImagePreview(urlFound)
                } else {
                    // nic nie znaleziono – nic nie robimy
                }
            }
    }

    // ========== PODGLĄD ==========
    private fun showImagePreview(url: String) {
        val iv = binding.ivImagePreview
        if (iv != null) {
            iv.visibility = View.VISIBLE
            Glide.with(this)
                .load(url)
                .into(iv)
        }
        selectedImageUrl = url
    }

    // ========== GALERIA + IMGUR ==========
    private fun uploadImageToImgur(uri: Uri) {
        val ctx = requireContext()
        val pb = binding.progressImageUpload
        pb?.visibility = View.VISIBLE

        val file = createTempFileFromUri(uri)
        if (file == null) {
            pb?.visibility = View.GONE
            Toast.makeText(ctx, "Nie udało się odczytać pliku", Toast.LENGTH_SHORT).show()
            return
        }

        ImgurUploader.uploadImage(
            file,
            onSuccess = { url ->
                // jesteśmy w wątku OkHttp → wracamy na główny
                requireActivity().runOnUiThread {
                    pb?.visibility = View.GONE
                    showImagePreview(url)
                    Toast.makeText(ctx, "Zdjęcie przesłane", Toast.LENGTH_SHORT).show()
                }
            },
            onError = {
                requireActivity().runOnUiThread {
                    pb?.visibility = View.GONE
                    Toast.makeText(ctx, "Błąd uploadu zdjęcia", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val resolver: ContentResolver = requireContext().contentResolver
            val name = queryName(resolver, uri) ?: "image.jpg"
            val file = File(requireContext().cacheDir, name)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? {
        val returnCursor = resolver.query(uri, null, null, null, null) ?: return null
        returnCursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
    }

    // ========== DATE PICKER ==========
    private fun showDatePicker(onDatePicked: (String) -> Unit) {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH)
        val d = c.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val mm = (month + 1).toString().padStart(2, '0')
                val dd = day.toString().padStart(2, '0')
                onDatePicked("$year-$mm-$dd")
            },
            y, m, d
        ).show()
    }
}
