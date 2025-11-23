package com.example.skaner_kodow.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentProductsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProductsFragment : Fragment() {

    private lateinit var binding: FragmentProductsBinding
    private lateinit var viewModel: ProductsViewModel
    private var isAdmin: Boolean = false
    private lateinit var adapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        // najpierw sprawdzamy czy admin, potem ustawiamy listę
        checkIfAdminAndSetup()

        binding.addProductButton.setOnClickListener {
            findNavController().navigate(R.id.action_nav_products_to_addProductFragment)
        }

        // nasluchuje ulubione
        viewModel.observeFavoriteProducts()

        viewModel.filteredProducts.observe(viewLifecycleOwner) { list ->
            // przekazanie aktualny zestaw ulubionych
            adapter.submitFavorites(viewModel.favProducts.value.orEmpty())
            adapter.submitList(list)
        }

        // gdy zmieniają się ulubione odmalowuje ikonki serduszek
        viewModel.favProducts.observe(viewLifecycleOwner) { favs ->
            if (::adapter.isInitialized) {
                adapter.submitFavorites(favs)
            }
        }

        viewModel.fetchProducts()

        setupSearch()

        return binding.root
    }

    private fun checkIfAdminAndSetup() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            setupRecyclerView(false)
            return
        }

        val uid = currentUser.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snap ->
                val role = snap.child("role").getValue(String::class.java)
                isAdmin = role == "admin"
                setupRecyclerView(isAdmin)
            }
            .addOnFailureListener {
                setupRecyclerView(false)
            }
    }

    private fun setupRecyclerView(isAdmin: Boolean) {
        adapter = ProductsAdapter(
            isAdmin = isAdmin,
            onProductClick = { product ->
                navigateToProductDetails(product)
            },
            onProductLongClick = { product ->
                // tylko admin tu wchodzi
                showAdminMenu(product)
            }
        )

        // --- ULUBIONE: kliknięcie w serduszko → przełącz w /users/{uid}/favorites/products
        adapter.setOnFavoriteClickListener { productId ->
            viewModel.toggleFavoriteProduct(productId)
        }

        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewProducts.adapter = adapter

        // jeśli nie admin – schowaj przycisk dodawania
        if (!isAdmin) {
            binding.addProductButton.visibility = View.GONE
        }
    }

    private fun showAdminMenu(product: Product) {
        val options = arrayOf("Edytuj produkt", "Usuń produkt")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Opcje produktu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // nawigacja do edycji
                        val bundle = Bundle().apply {
                            putString("id", product.id)
                            putString("name", product.name)
                            putString("barcode", product.barcode)
                            putString("description", product.description)
                            putString("imageUrl", product.imageUrl)
                            putDouble("price", product.price)
                            putString("priceUpdatedAt", product.priceUpdatedAt)
                        }
                        findNavController().navigate(
                            R.id.editProductFragment,
                            bundle
                        )
                    }
                    1 -> {
                        // potwierdzenie usunięcia
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Usuń produkt")
                            .setMessage("Na pewno chcesz usunąć ten produkt?")
                            .setPositiveButton("Usuń") { _, _ ->
                                viewModel.deleteProduct(product.id)
                                Toast.makeText(requireContext(), "Usunięto produkt", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Anuluj", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
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
            putString("id", product.id)
            putString("name", product.name)
            putString("barcode", product.barcode)
            putString("description", product.description)
            putString("imageUrl", product.imageUrl)
            putDouble("price", product.price)
            putString("priceUpdatedAt", product.priceUpdatedAt)
        }
        findNavController().navigate(
            R.id.action_nav_products_to_productDetailsFragment,
            bundle
        )
    }
}
