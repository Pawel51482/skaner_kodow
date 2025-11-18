package com.example.skaner_kodow.ui.promotions

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
import com.example.skaner_kodow.databinding.FragmentPromotionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PromotionsFragment : Fragment() {

    private lateinit var binding: FragmentPromotionsBinding
    private lateinit var viewModel: PromotionsViewModel
    private lateinit var adapter: PromotionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPromotionsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(PromotionsViewModel::class.java)

        setupRecyclerView()
        setupObservers()

        binding.fabAddPromotion.setOnClickListener {
            findNavController().navigate(R.id.action_nav_promotions_to_addPromotionFragment)
        }

        // ULUBIONE
        adapter.setOnFavoriteClickListener { promoId ->
            viewModel.toggleFavoritePromotion(promoId)
        }

        viewModel.observeFavoritePromotions()

        viewModel.promotions.observe(viewLifecycleOwner) { all ->
            val favs = viewModel.favPromotions.value.orEmpty()
            adapter.submitFavorites(favs)
            adapter.submitList(all)
        }

        viewModel.favPromotions.observe(viewLifecycleOwner) { favs ->
            adapter.submitFavorites(favs)
        }


        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = PromotionsAdapter(
            onClick = { promotion ->
                // przejście do szczegółów
                val bundle = Bundle().apply {
                    putString("id", promotion.id)
                    putString("title", promotion.title)
                    putString("description", promotion.description)
                    putString("barcode", promotion.barcode)
                    putString("startDate", promotion.startDate)
                    putString("endDate", promotion.endDate)
                    putString("imageUrl", promotion.imageUrl)
                }
                findNavController().navigate(
                    R.id.promotionDetailsFragment,
                    bundle
                )
            },
            onLongClick = { promotion ->
                tryDeletePromotion(promotion)
            }
        )
        binding.recyclerViewPromotions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPromotions.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.promotions.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    private fun tryDeletePromotion(promotion: Promotion) {
        val options = arrayOf("Edytuj promocję", "Usuń promocję")

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Opcje promocji")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // edytowanie - przechodzimy z danymi
                        val bundle = Bundle().apply {
                            putString("id", promotion.id)
                            putString("title", promotion.title)
                            putString("description", promotion.description)
                            putString("barcode", promotion.barcode)
                            putString("startDate", promotion.startDate)
                            putString("endDate", promotion.endDate)
                            putString("addedBy", promotion.addedBy)
                            putString("imageUrl", promotion.imageUrl)
                        }
                        findNavController().navigate(
                            R.id.editPromotionFragment,
                            bundle
                        )
                    }
                    1 -> {
                        checkPermissionAndDelete(promotion)
                    }
                }
            }
            .create()

        dialog.show()

        // Po otwarciu dialogu sprawdzamy uprawnienia użytkownika
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid ?: "")

        usersRef.get().addOnSuccessListener { snap ->
            val role = snap.child("role").getValue(String::class.java)
            val isAdmin = role == "admin"
            val isOwner = promotion.addedBy == currentUid

            if (!(isAdmin || isOwner)) {
                //  użytkownik nie ma uprawnień - wyszarzamy obie opcje
                val listView = dialog.listView
                listView?.post {
                    for (i in 0 until listView.childCount) {
                        val item = listView.getChildAt(i)
                        item?.isEnabled = false
                        item?.alpha = 0.5f // efekt wyszarzenia
                    }
                }
            }
        }
    }



    private fun checkPermissionAndDelete(promotion: Promotion) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUid = currentUser.uid

        val usersRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUid)

        usersRef.get().addOnSuccessListener { snap ->
            val role = snap.child("role").getValue(String::class.java)
            val isAdmin = role == "admin"
            val isOwner = promotion.addedBy == currentUid

            if (isAdmin || isOwner) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Usuń promocję")
                    .setMessage("Na pewno chcesz usunąć tę promocję?")
                    .setPositiveButton("Usuń") { _, _ ->
                        viewModel.deletePromotion(promotion.id)
                        Toast.makeText(requireContext(), "Promocja usunięta", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Anuluj", null)
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Możesz usunąć tylko swoje promocje",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
