package com.example.skaner_kodow.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.FragmentProductDetailsBinding

class ProductDetailsFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false)

        val productName = arguments?.getString("name")
        val productBarcode = arguments?.getString("barcode")
        val productDescription = arguments?.getString("description")
        val productImageUrl = arguments?.getString("imageUrl")


        binding.textViewProductName.text = productName
        binding.textViewProductBarcode.text = productBarcode
        binding.textViewProductDescription.text = productDescription
        Glide.with(this).load(productImageUrl).into(binding.imageViewProduct)

        return binding.root
    }
}
