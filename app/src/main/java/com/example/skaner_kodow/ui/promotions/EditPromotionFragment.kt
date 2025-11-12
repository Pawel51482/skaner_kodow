package com.example.skaner_kodow.ui.promotions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.FragmentAddPromotionBinding
import java.util.Calendar

class EditPromotionFragment : Fragment() {

    private lateinit var binding: FragmentAddPromotionBinding
    private lateinit var viewModel: PromotionsViewModel

    // dane promo przekazane z listy
    private var promoId: String? = null
    private var addedBy: String? = null
    private var currentImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddPromotionBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(PromotionsViewModel::class.java)

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

        // ustawiamy dane w polach
        binding.etTitle.setText(title)
        binding.etDescription.setText(description)
        binding.etProductBarcode.setText(barcode)
        binding.etStartDate.setText(startDate)
        binding.etEndDate.setText(endDate)

        // jeśli było zdjęcie – pokaż
        if (!currentImageUrl.isNullOrEmpty()) {
            binding.ivImagePreview.visibility = View.VISIBLE
            Glide.with(this)
                .load(currentImageUrl)
                .into(binding.ivImagePreview)
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
        binding.btnAddPromotion.text = "Zapisz zmiany"
        binding.btnAddPromotion.setOnClickListener {
            saveEditedPromotion()
        }

        // (możesz dodać też skaner / wybór zdjęcia tak jak w add – ale na razie zostawiamy prosto)

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

        // składamy obiekt taki jak w Promotion.kt
        val edited = Promotion(
            id = id,
            title = title,
            description = description,
            barcode = barcode,
            startDate = start,
            endDate = end,
            addedBy = addedBy ?: "",
            imageUrl = currentImageUrl ?: ""
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
                onDatePicked("$year-$mm-$dd")
            },
            y, m, d
        ).show()
    }
}
