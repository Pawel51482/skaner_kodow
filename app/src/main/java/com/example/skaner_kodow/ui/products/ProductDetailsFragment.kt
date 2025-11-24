package com.example.skaner_kodow.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentProductDetailsBinding
import com.example.skaner_kodow.ui.shoppinglists.ShoppingListsViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class ProductDetailsFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailsBinding
    private lateinit var shoppingListsViewModel: ShoppingListsViewModel

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

        val productId = arguments?.getString("id")

        // inicjalizacja ViewModel od list zakupowych + nasłuchiwanie list
        shoppingListsViewModel = ViewModelProvider(requireActivity())
            .get(ShoppingListsViewModel::class.java)
        shoppingListsViewModel.observeShoppingLists()

        binding.textViewProductName.text = productName
        binding.textViewProductBarcode.text = productBarcode
        binding.textViewProductDescription.text = productDescription

        setupFavoriteProduct(productId)

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

        // Obsługa przycisku "dodaj do listy zakupowej"
        binding.btnAddToShoppingList.setOnClickListener {
            if (productId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Brak ID produktu", Toast.LENGTH_SHORT).show()
            } else {
                showAddToShoppingListDialog(productId)
            }
        }

        return binding.root
    }

    // wybor listy zakupowej
    private fun showAddToShoppingListDialog(productId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Musisz być zalogowany", Toast.LENGTH_SHORT).show()
            return
        }

        val lists = shoppingListsViewModel.lists.value ?: emptyList()
        if (lists.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Nie masz żadnych list zakupowych. Utwórz listę w zakładce 'Listy zakupowe'.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val names = lists.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj do listy zakupowej")
            .setItems(names) { _, which ->
                val listId = lists[which].id
                addProductToShoppingList(listId, productId)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    // zapis produktu do  listy zakupowej
    private fun addProductToShoppingList(listId: String, productId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val itemsRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("shoppingLists")
            .child(listId)
            .child("items")

        val newRef = itemsRef.push()
        val itemId = newRef.key ?: return

        val data = mapOf(
            "id" to itemId,
            "productId" to productId,
            "quantity" to 1,
            "checked" to false
        )

        newRef.setValue(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Dodano produkt do listy zakupowej",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Błąd przy dodawaniu do listy",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    // dodanie do ulubioncyh
    private fun setupFavoriteProduct(productId: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || productId.isNullOrEmpty()) {
            // brak usera albo brak id – ukryj ikonke
            binding.ivFavoriteProduct.visibility = View.GONE
            return
        }

        val favRef = FirebaseDatabase.getInstance()
            .getReference("users/$uid/favorites/products/$productId")

        // Odczyt stanu na start, czy produkt jest już w ulubionych
        favRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isFav = snapshot.exists()
                binding.ivFavoriteProduct.tag = isFav
                binding.ivFavoriteProduct.setImageResource(
                    if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Kliknięcie – toggle ulubionych
        binding.ivFavoriteProduct.setOnClickListener {
            val currentFlag = binding.ivFavoriteProduct.tag as? Boolean ?: false
            if (currentFlag) {
                favRef.setValue(null)
                binding.ivFavoriteProduct.tag = false
                binding.ivFavoriteProduct.setImageResource(R.drawable.ic_heart_outline)
                Toast.makeText(requireContext(), "Usunięto z ulubionych", Toast.LENGTH_SHORT).show()
            } else {
                favRef.setValue(true)
                binding.ivFavoriteProduct.tag = true
                binding.ivFavoriteProduct.setImageResource(R.drawable.ic_heart_filled)
                Toast.makeText(requireContext(), "Dodano do ulubionych", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
