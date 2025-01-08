package com.example.skaner_kodow.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentProductsBinding
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class ProductsFragment : Fragment() {

    private lateinit var binding: FragmentProductsBinding
    private lateinit var viewModel: ProductsViewModel
    private lateinit var adapter: ProductsAdapter
    private val adminEmails = listOf("test@test.pl")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupSearchView()


        binding.addProductButton.setOnClickListener {
            navigateToAddProduct()
        }

        viewModel.fetchProducts()

        if (!isAdmin()) {
            binding.addProductButton.visibility = View.GONE
        }

        return binding.root
    }

    private fun isAdmin(): Boolean {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        return currentUserEmail != null && adminEmails.contains(currentUserEmail)
    }

    private fun navigateToAddProduct() {
        findNavController().navigate(R.id.action_nav_products_to_addProductFragment)
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter { product ->
            navigateToProductDetails(product)
        }
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewProducts.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.filteredProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.filterProducts(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.filterProducts(it) }
                return true
            }
        })
    }

    private fun navigateToProductDetails(product: Product) {
        val bundle = Bundle().apply {
            putString("name", product.name)
            putString("barcode", product.barcode)
            putString("description", product.description)
            putString("imageUrl", product.imageUrl)
        }
        findNavController().navigate(R.id.action_nav_products_to_productDetailsFragment, bundle)
    }
}
