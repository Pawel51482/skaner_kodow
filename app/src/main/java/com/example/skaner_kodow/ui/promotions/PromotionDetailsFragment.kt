package com.example.skaner_kodow.ui.promotions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.FragmentPromotionDetailsBinding

class PromotionDetailsFragment : Fragment() {

    private lateinit var binding: FragmentPromotionDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPromotionDetailsBinding.inflate(inflater, container, false)

        val args = arguments
        val title = args?.getString("title") ?: ""
        val description = args?.getString("description") ?: ""
        val barcode = args?.getString("barcode") ?: ""
        val startDate = args?.getString("startDate") ?: ""
        val endDate = args?.getString("endDate") ?: ""
        val imageUrl = args?.getString("imageUrl") ?: ""

        binding.tvTitle.text = title
        binding.tvDescription.text = description
        binding.tvBarcode.text = "Kod produktu: $barcode"
        binding.tvDates.text = "Obowiązuje: $startDate – $endDate"

        if (imageUrl.isNotEmpty()) {
            binding.ivImage.visibility = View.VISIBLE
            Glide.with(this).load(imageUrl).into(binding.ivImage)
        } else {
            binding.ivImage.visibility = View.GONE
        }

        return binding.root
    }
}
