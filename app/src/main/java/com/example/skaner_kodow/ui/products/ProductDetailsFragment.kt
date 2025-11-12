package com.example.skaner_kodow.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentProductDetailsBinding
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class ProductDetailsFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailsBinding

    private var foundPromotion: com.example.skaner_kodow.ui.promotions.Promotion? = null
    private var productBarcode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false)

        val productName = arguments?.getString("name") ?: ""
        productBarcode = arguments?.getString("barcode") ?: ""
        val productDescription = arguments?.getString("description") ?: ""
        val productImageUrl = arguments?.getString("imageUrl") ?: ""
        val productPrice = arguments?.getDouble("price") ?: 0.0
        val productPriceDate = arguments?.getString("priceUpdatedAt") ?: ""

        binding.textViewProductName.text = productName
        binding.textViewProductBarcode.text = productBarcode
        binding.textViewProductDescription.text = productDescription

        if (productPrice > 0.0) {
            val priceFormatted =
                String.format(Locale.getDefault(), "%.2f zł", productPrice)
            val dateInfo = if (productPriceDate.isNotEmpty()) {
                " (z dnia $productPriceDate)"
            } else ""
            binding.textViewProductPrice.visibility = View.VISIBLE
            binding.textViewProductPrice.text = priceFormatted + dateInfo
        } else {
            binding.textViewProductPrice.visibility = View.GONE
        }

        if (productImageUrl.isNotEmpty()) {
            Glide.with(this).load(productImageUrl).into(binding.imageViewProduct)
        }

        binding.promoInfoGroup.visibility = View.GONE

        checkPromotionForBarcode(productBarcode)

        binding.btnGoToPromotion.setOnClickListener {
            val promo = foundPromotion
            if (promo != null) {
                val bundle = Bundle().apply {
                    putString("title", promo.title)
                    putString("description", promo.description)
                    putString("barcode", promo.barcode)
                    putString("startDate", promo.startDate)
                    putString("endDate", promo.endDate)
                    putString("imageUrl", promo.imageUrl)
                }
                findNavController().navigate(
                    R.id.promotionDetailsFragment,
                    bundle
                )
            } else {
                Toast.makeText(requireContext(), "Brak szczegółów promocji", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }


    private fun checkPromotionForBarcode(barcode: String) {
        if (barcode.isEmpty()) return

        val promosRef = FirebaseDatabase.getInstance().getReference("promotions")
        promosRef.get()
            .addOnSuccessListener { snap ->
                var matched: com.example.skaner_kodow.ui.promotions.Promotion? = null
                for (child in snap.children) {
                    val promo = child.getValue(com.example.skaner_kodow.ui.promotions.Promotion::class.java)
                    if (promo != null && promo.barcode == barcode) {
                        matched = promo.copy(id = child.key ?: "")
                        break
                    }
                }

                if (matched != null) {
                    foundPromotion = matched
                    binding.promoInfoGroup.visibility = View.VISIBLE
                    binding.tvPromoInfo.text = "Dla tego produktu jest aktywna promocja."
                } else {
                    binding.promoInfoGroup.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                binding.promoInfoGroup.visibility = View.GONE
            }
    }
}
