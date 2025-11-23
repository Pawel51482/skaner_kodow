package com.example.skaner_kodow.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.databinding.FragmentFavoritesListBinding
import com.example.skaner_kodow.ui.products.ProductsAdapter
import com.example.skaner_kodow.ui.products.ProductsViewModel
import androidx.navigation.fragment.findNavController
import com.example.skaner_kodow.R
import androidx.core.os.bundleOf


class FavoritesProductsFragment : Fragment() {

    private lateinit var binding: FragmentFavoritesListBinding
    private lateinit var viewModel: ProductsViewModel
    private lateinit var adapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(ProductsViewModel::class.java)

        adapter = ProductsAdapter(
            isAdmin = false,
            onProductClick = { product ->
                val args = bundleOf(
                    "id" to product.id,
                    "name" to product.name,
                    "barcode" to product.barcode,
                    "description" to product.description,
                    "imageUrl" to product.imageUrl,
                    "price" to product.price,
                    "priceUpdatedAt" to product.priceUpdatedAt
                )

                findNavController().navigate(
                    R.id.productDetailsFragment,
                    args
                )
            },
            onProductLongClick = { }
        )
        adapter.setOnFavoriteClickListener { productId ->
            viewModel.toggleFavoriteProduct(productId)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.observeFavoriteProducts()
        viewModel.fetchProducts()

        viewModel.filteredProducts.observe(viewLifecycleOwner) { all ->
            val favs = viewModel.favProducts.value.orEmpty()
            adapter.submitFavorites(favs)
            adapter.submitList(all.filter { favs.contains(it.id) })
        }
        viewModel.favProducts.observe(viewLifecycleOwner) { favs ->
            val all = viewModel.filteredProducts.value.orEmpty()
            adapter.submitFavorites(favs)
            adapter.submitList(all.filter { favs.contains(it.id) })
        }

        return binding.root
    }
}
