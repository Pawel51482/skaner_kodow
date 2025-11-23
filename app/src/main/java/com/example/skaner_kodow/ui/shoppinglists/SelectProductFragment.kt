package com.example.skaner_kodow.ui.shoppinglists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.databinding.FragmentSelectProductBinding
import com.example.skaner_kodow.ui.products.Product
import com.example.skaner_kodow.ui.products.ProductsAdapter
import com.example.skaner_kodow.ui.products.ProductsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SelectProductFragment : Fragment() {

    private lateinit var binding: FragmentSelectProductBinding
    private lateinit var productsViewModel: ProductsViewModel
    private lateinit var adapter: ProductsAdapter

    private var listId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectProductBinding.inflate(inflater, container, false)

        // odbieramy listId przekazane z ShoppingListDetailsFragment
        listId = arguments?.getString("listId") ?: ""

        productsViewModel =
            ViewModelProvider(requireActivity()).get(ProductsViewModel::class.java)

        setupRecycler()
        setupSearch()

        // ładuje produkty
        productsViewModel.fetchProducts()

        productsViewModel.filteredProducts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        return binding.root
    }

    private fun setupRecycler() {
        adapter = ProductsAdapter(
            isAdmin = false,
            onProductClick = { product ->
                askQuantityAndAdd(product)
            },
            onProductLongClick = { _ ->
            }
        )

        binding.recyclerViewSelectProducts.layoutManager =
            LinearLayoutManager(requireContext())
        binding.recyclerViewSelectProducts.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchViewSelect.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { productsViewModel.filterProducts(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { productsViewModel.filterProducts(it) }
                return true
            }
        })
    }

    private fun askQuantityAndAdd(product: Product) {
        val context = requireContext()

        val qtyEdit = EditText(context).apply {
            hint = "Ilość"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(qtyEdit)
        }

        AlertDialog.Builder(context)
            .setTitle("Ile sztuk dodać?")
            .setView(layout)
            .setPositiveButton("Dodaj") { _, _ ->
                val qtyText = qtyEdit.text.toString().trim()
                val qty = qtyText.toIntOrNull() ?: 1
                addOrIncrementProduct(product, qty)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    // jeśli item z tym product id istnieje to zwiększ ilosć, a jak nie to stwórz
    private fun addOrIncrementProduct(product: Product, quantityToAdd: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || listId.isEmpty()) {
            Toast.makeText(requireContext(), "Błąd: brak użytkownika lub listy", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val itemsRef = FirebaseDatabase.getInstance()
            .getReference("users/$uid/shoppingLists/$listId/items")

        itemsRef.orderByChild("productId").equalTo(product.id).limitToFirst(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    // jest już taki produkt  zwiększamy ilość
                    val child = snap.children.first()
                    val currentQty = child.child("quantity").getValue(Int::class.java) ?: 1
                    child.ref.child("quantity").setValue(currentQty + quantityToAdd)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Dodano ${quantityToAdd} szt. ${product.name} (razem ${currentQty + quantityToAdd})",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Błąd przy aktualizacji ilości",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // nie ma jeszcze tego produktu na liście, tworzymy nowy wpis
                    val ref = itemsRef.push()
                    val item = ShoppingListItem(
                        id = ref.key ?: "",
                        productId = product.id,
                        quantity = quantityToAdd,
                        checked = false
                    )
                    ref.setValue(item)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Dodano ${quantityToAdd} szt. ${product.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Błąd przy dodawaniu do listy",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Błąd odczytu listy", Toast.LENGTH_SHORT).show()
            }
    }
}
