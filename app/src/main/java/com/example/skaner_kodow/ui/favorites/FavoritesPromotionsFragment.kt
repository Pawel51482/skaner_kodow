package com.example.skaner_kodow.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.databinding.FragmentFavoritesListBinding
import com.example.skaner_kodow.ui.promotions.PromotionsAdapter
import com.example.skaner_kodow.ui.promotions.PromotionsViewModel
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.skaner_kodow.R

class FavoritesPromotionsFragment : Fragment() {

    private lateinit var binding: FragmentFavoritesListBinding
    private lateinit var viewModel: PromotionsViewModel
    private lateinit var adapter: PromotionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(PromotionsViewModel::class.java)

        adapter = PromotionsAdapter(
            onClick = { promo ->
                val args = bundleOf(
                    "id" to promo.id,
                    "title" to promo.title,
                    "description" to promo.description,
                    "barcode" to promo.barcode,
                    "imageUrl" to promo.imageUrl,
                    "startDate" to promo.startDate,
                    "endDate" to promo.endDate
                )
                findNavController().navigate(
                    R.id.promotionDetailsFragment,
                    args
                )
            },
            onLongClick = { }
        )
        adapter.setOnFavoriteClickListener { promoId ->
            viewModel.toggleFavoritePromotion(promoId)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.observeFavoritePromotions()

        viewModel.promotions.observe(viewLifecycleOwner) { all ->
            val favs = viewModel.favPromotions.value.orEmpty()
            adapter.submitFavorites(favs)
            adapter.submitList(all.filter { favs.contains(it.id) })
        }
        viewModel.favPromotions.observe(viewLifecycleOwner) { favs ->
            val all = viewModel.promotions.value.orEmpty()
            adapter.submitFavorites(favs)
            adapter.submitList(all.filter { favs.contains(it.id) })
        }

        return binding.root
    }
}
