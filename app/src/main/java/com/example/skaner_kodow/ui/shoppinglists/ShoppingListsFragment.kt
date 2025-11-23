package com.example.skaner_kodow.ui.shoppinglists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.databinding.FragmentShoppingListsBinding
import com.example.skaner_kodow.R

class ShoppingListsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingListsBinding
    private lateinit var viewModel: ShoppingListsViewModel
    private lateinit var adapter: ShoppingListsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingListsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ShoppingListsViewModel::class.java)

        setupRecyclerView()
        setupFab()
        setupObservers()

        viewModel.observeShoppingLists()

        return binding.root
    }

    private fun setupRecyclerView() {

        adapter = ShoppingListsAdapter(
            onClick = { list ->
                findNavController().navigate(
                    R.id.shoppingListDetailsFragment,
                    bundleOf("listId" to list.id)
                )
            },
            onLongClick = { list ->
                showDeleteListDialog(list)
            }
        )

        binding.recyclerViewLists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLists.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    private fun setupObservers() {
        viewModel.lists.observe(viewLifecycleOwner) { lists ->
            adapter.submitList(lists)
            binding.tvEmpty.visibility = if (lists.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // tworzenie listy
    private fun showCreateListDialog() {
        val editText = EditText(requireContext())
        editText.hint = "Nazwa listy"

        AlertDialog.Builder(requireContext())
            .setTitle("Nowa lista zakupowa")
            .setView(editText)
            .setPositiveButton("Utwórz") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Podaj nazwę listy", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addShoppingList(name)
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    // usuwanie listy
    private fun showDeleteListDialog(list: ShoppingList) {
        AlertDialog.Builder(requireContext())
            .setTitle("Usuń listę")
            .setMessage("Czy na pewno chcesz usunąć listę \"${list.name}\"?")
            .setPositiveButton("Usuń") { _, _ ->
                viewModel.deleteList(list.id)
                Toast.makeText(requireContext(), "Lista została usunięta", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
}
