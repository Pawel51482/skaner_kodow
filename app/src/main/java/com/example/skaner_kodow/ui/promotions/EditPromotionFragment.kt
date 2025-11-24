package com.example.skaner_kodow.ui.promotions

import android.app.DatePickerDialog
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.FragmentAddPromotionBinding
import com.example.skaner_kodow.utils.ImagePreview
import com.example.skaner_kodow.utils.ImgurUploader
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditPromotionFragment : Fragment() {

    private lateinit var binding: FragmentAddPromotionBinding
    private lateinit var viewModel: PromotionsViewModel

    // dane promo przekazane z listy
    private var promoId: String? = null
    private var addedBy: String? = null
    private var currentImageUrl: String? = null

    // wybrane zdjęcie jeśli użytkownik zmieni
    private var selectedImageUrl: String? = null

    // picker z galerii
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

        // limit znaków tytułu
        binding.etTitle.filters = arrayOf(InputFilter.LengthFilter(50))

        // odbieramy argumenty
        val args = arguments
        promoId = args?.getString("id")
        val title = args?.getString("title") ?: ""
        val description = args?.getString("description") ?: ""
        val barcode = args?.getString("barcode") ?: ""
        val startDate = args?.getString("startDate") ?: ""
        val endDate = args?.getString("endDate") ?: ""
        addedBy = args?.getString("addedBy")
        currentImageUrl = args?.getString("imageUrl")

        // na start zakładamy, że wybrane = obecne
        selectedImageUrl = currentImageUrl

        // ustawiamy dane w polach
        binding.etTitle.setText(title)
        binding.etDescription.setText(description)
        binding.etProductBarcode.setText(barcode)
        binding.etStartDate.setText(startDate)
        binding.etEndDate.setText(endDate)

        // jeśli było zdjęcie – pokaż
        if (!currentImageUrl.isNullOrEmpty()) {
            showImagePreview(currentImageUrl!!)
        } else {
            binding.ivImagePreview.visibility = View.GONE
        }

        // daty – jak wcześniej
        binding.etStartDate.setOnClickListener {
            showDatePicker { d -> binding.etStartDate.setText(d) }
        }
        binding.etEndDate.setOnClickListener {
            showDatePicker { d -> binding.etEndDate.setText(d) }
        }

        // przycisk – teraz to "zapisz zmiany", ale korzystamy z tego samego id
        binding.btnPickImage?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.progressImageUpload?.visibility = View.GONE

        binding.btnAddPromotion.text = "Zapisz zmiany"
        binding.btnAddPromotion.setOnClickListener {
            saveEditedPromotion()
        }

        return binding.root
    }

    private fun saveEditedPromotion() {
        val id = promoId
        if (id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Brak ID promocji", Toast.LENGTH_SHORT).show()
            return
        }

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

        // limit znaków tytułu
        if (title.length > 50) {
            Toast.makeText(
                requireContext(),
                "Tytuł może mieć maksymalnie 50 znaków",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = sdf.parse(sdf.format(Date()))
        val startDate = try { sdf.parse(start) } catch (e: Exception) { null }
        val endDate = try { sdf.parse(end) } catch (e: Exception) { null }

        if (startDate == null || endDate == null || today == null) {
            Toast.makeText(requireContext(), "Nieprawidłowy format daty", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDate.before(today)) {
            Toast.makeText(
                requireContext(),
                "Data zakończenia nie może być wcześniejsza niż dzisiaj",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (endDate.before(startDate)) {
            Toast.makeText(
                requireContext(),
                "Data zakończenia nie może być wcześniejsza niż data rozpoczęcia",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val finalImageUrl = selectedImageUrl ?: currentImageUrl.orEmpty()

        val edited = Promotion(
            id = id,
            title = title,
            description = description,
            barcode = barcode,
            startDate = start,
            endDate = end,
            addedBy = addedBy ?: "",
            imageUrl = finalImageUrl
        )

        viewModel.updatePromotion(edited)
        Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

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
                onDatePicked("$dd-$mm-$year")
            },
            y, m, d
        ).show()
    }

    // ========== PODGLĄD ZDJĘCIA ==========

    private fun showImagePreview(url: String) {
        val iv = binding.ivImagePreview
        iv.visibility = View.VISIBLE
        Glide.with(this)
            .load(url)
            .into(iv)
        selectedImageUrl = url
        iv.setOnClickListener {
            val finalUrl = selectedImageUrl ?: currentImageUrl
            if (!finalUrl.isNullOrEmpty()) {
                ImagePreview(finalUrl).show(parentFragmentManager, "edit_promotion_preview")
            }
        }
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
}
