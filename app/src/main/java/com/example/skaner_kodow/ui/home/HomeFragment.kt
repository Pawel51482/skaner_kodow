package com.example.skaner_kodow.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.skaner_kodow.BarcodeScannerActivity
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentHomeBinding
import com.example.skaner_kodow.ui.products.Product
import com.example.skaner_kodow.ui.promotions.Promotion
import com.example.skaner_kodow.ui.shoppinglists.ShoppingList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val SCAN_REQUEST_CODE = 1

    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var promotionsAdapter: HomePromotionsAdapter
    private lateinit var topPromotionsAdapter: HomePromotionsAdapter
    private lateinit var shoppingListsAdapter: HomeShoppingListsAdapter

    private var lastScannedProductId: String? = null
    private var lastActivePromotionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        homeViewModel.text.observe(viewLifecycleOwner) { text ->
            binding.tvResult.text = text
        }

        binding.btnOpenScanner.setOnClickListener {
            val intent = Intent(requireContext(), BarcodeScannerActivity::class.java)
            startActivityForResult(intent, SCAN_REQUEST_CODE)
        }

        binding.buttonDetails.visibility = View.GONE
        binding.ivLastProduct.visibility = View.GONE
        binding.tvPromotionInfo.visibility = View.GONE

        setupPromotionsSection()
        setupTopPromotionsSection()
        setupShoppingListsSection()

        loadTodayPromotions()
        loadTopRatedPromotions()
        loadShoppingListsPreview()

        return binding.root
    }

    //  PROMOCJE WAŻNE DZISIAJ

    private fun setupPromotionsSection() {
        promotionsAdapter = HomePromotionsAdapter(
            onClick = { promotion ->
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
                    R.id.action_nav_home_to_promotionDetailsFragment,
                    bundle
                )
            },
            showRating = false
        )

        binding.rvTodayPromotions.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = promotionsAdapter
        }

        binding.tvSeeAllPromotions.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_promotions)
        }
    }

    // TOP PROMO

    private fun setupTopPromotionsSection() {
        topPromotionsAdapter = HomePromotionsAdapter(
            onClick = { promotion ->
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
                    R.id.action_nav_home_to_promotionDetailsFragment,
                    bundle
                )
            },
            showRating = true
        )

        binding.rvTopPromotions.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = topPromotionsAdapter
        }
    }

    // LISTY ZAKUPOWE

    private fun setupShoppingListsSection() {
        shoppingListsAdapter = HomeShoppingListsAdapter { list ->
            val bundle = Bundle().apply {
                putString("listId", list.id)
            }
            findNavController().navigate(
                R.id.action_nav_home_to_shoppingListDetailsFragment,
                bundle
            )
        }

        binding.rvShoppingListsPreview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = shoppingListsAdapter
        }

        binding.tvNewList.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_shopping_lists)
        }
    }

    // SKANER

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val scannedCode = data.getStringExtra("scanned_code")
            if (scannedCode != null) {
                checkBarcodeInDatabase(scannedCode)
            } else {
                Toast.makeText(context, "Nie zeskanowano kodu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBarcodeInDatabase(barcode: String) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("products")

        productsRef.orderByChild("barcode").equalTo(barcode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (!snapshot.exists()) {
                        homeViewModel.updateText("Nie znaleziono kodu w bazie: $barcode")
                        binding.ivLastProduct.visibility = View.GONE
                        binding.buttonDetails.visibility = View.GONE
                        binding.tvPromotionInfo.visibility = View.GONE
                        lastScannedProductId = null
                        lastActivePromotionId = null
                        return
                    }

                    val productSnapshot = snapshot.children.first()
                    val product = productSnapshot.getValue(Product::class.java)
                    val id = productSnapshot.key

                    if (product == null || id == null) return

                    lastScannedProductId = id

                    val sb = StringBuilder()
                    sb.append("Znaleziono: ${product.name}\n")
                    sb.append("Kod: ${product.barcode}\n")

                    if (product.price > 0.0) {
                        sb.append("Cena: ${"%.2f".format(product.price)} zł\n")
                    }
                    if (!product.priceUpdatedAt.isNullOrEmpty()) {
                        sb.append("Aktualizacja ceny: ${product.priceUpdatedAt}")
                    }

                    homeViewModel.updateText(sb.toString().trim())

                    if (!product.imageUrl.isNullOrEmpty()) {
                        binding.ivLastProduct.visibility = View.VISIBLE
                        Glide.with(this@HomeFragment)
                            .load(product.imageUrl)
                            .into(binding.ivLastProduct)
                    } else {
                        binding.ivLastProduct.visibility = View.GONE
                    }

                    binding.buttonDetails.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            val bundle = Bundle().apply {
                                putString("id", id)
                                putString("name", product.name)
                                putString("description", product.description)
                                putString("barcode", product.barcode)
                                putString("imageUrl", product.imageUrl)
                                putDouble("price", product.price)
                                putString("priceUpdatedAt", product.priceUpdatedAt)
                            }
                            findNavController().navigate(
                                R.id.action_nav_home_to_productDetailsFragment,
                                bundle
                            )
                        }
                    }

                    checkActivePromotionForBarcode(product.barcode)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkActivePromotionForBarcode(barcode: String) {
        val promotionsRef = FirebaseDatabase.getInstance().getReference("promotions")

        promotionsRef.orderByChild("barcode").equalTo(barcode)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val today = sdf.parse(sdf.format(Date()))

                    var activePromo: Promotion? = null
                    var promoKey: String? = null

                    for (child in snapshot.children) {
                        val promo = child.getValue(Promotion::class.java) ?: continue
                        try {
                            val start = sdf.parse(promo.startDate)
                            val end = sdf.parse(promo.endDate)
                            if (start != null && end != null && today != null) {
                                if (!today.before(start) && !today.after(end)) {
                                    activePromo = promo
                                    promoKey = child.key
                                    break
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }

                    if (activePromo != null && promoKey != null) {
                        lastActivePromotionId = promoKey

                        binding.tvPromotionInfo.visibility = View.VISIBLE
                        binding.tvPromotionInfo.text =
                            "Dla tego produktu jest aktywna promocja:\n${activePromo!!.title}"

                        binding.tvPromotionInfo.setOnClickListener {
                            val bundle = Bundle().apply {
                                putString("id", promoKey)
                                putString("title", activePromo!!.title)
                                putString("description", activePromo!!.description)
                                putString("barcode", activePromo!!.barcode)
                                putString("startDate", activePromo!!.startDate)
                                putString("endDate", activePromo!!.endDate)
                                putString("imageUrl", activePromo!!.imageUrl)
                            }
                            findNavController().navigate(
                                R.id.action_nav_home_to_promotionDetailsFragment,
                                bundle
                            )
                        }
                    } else {
                        binding.tvPromotionInfo.visibility = View.GONE
                        lastActivePromotionId = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.tvPromotionInfo.visibility = View.GONE
                    lastActivePromotionId = null
                }
            })
    }

    // ===== ŁADOWANIE PROMOCJI I LIST ==========

    // Promocje które konczą się dzisiaj, posortowane wg oceny
    private fun loadTodayPromotions() {
        val promosRef = FirebaseDatabase.getInstance().getReference("promotions")
        val votesRef = FirebaseDatabase.getInstance().getReference("promotionVotes")

        promosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val today = sdf.parse(sdf.format(Date()))

                val todayPromos = mutableListOf<Pair<String, Promotion>>() // <promoId, promo>

                for (child in snapshot.children) {
                    val promo = child.getValue(Promotion::class.java) ?: continue
                    try {
                        val end = sdf.parse(promo.endDate)
                        if (end != null && today != null && end == today) {
                            val promoId = child.key ?: continue
                            val fixed = promo.copy(id = promoId)
                            todayPromos.add(promoId to fixed)
                        }
                    } catch (_: Exception) {
                    }
                }

                if (todayPromos.isEmpty()) {
                    binding.tvPromotionsEmpty.visibility = View.VISIBLE
                    promotionsAdapter.submitList(emptyList())
                    return
                }

                val withRatings = mutableListOf<Pair<Promotion, Int>>()
                var processed = 0

                for ((promoId, promo) in todayPromos) {
                    votesRef.child(promoId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(voteSnap: DataSnapshot) {
                                var rating = 0
                                for (voteChild in voteSnap.children) {
                                    val value = voteChild.getValue(Int::class.java) ?: 0
                                    rating += value // 1 = plus, -1 = minus
                                }

                                withRatings.add(promo to rating)
                                processed++

                                if (processed == todayPromos.size) {
                                    val sorted = withRatings
                                        .sortedByDescending { it.second }
                                        .map { it.first }
                                        .take(3)

                                    if (sorted.isEmpty()) {
                                        binding.tvPromotionsEmpty.visibility = View.VISIBLE
                                        promotionsAdapter.submitList(emptyList())
                                    } else {
                                        binding.tvPromotionsEmpty.visibility = View.GONE
                                        promotionsAdapter.submitList(sorted)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                processed++
                                if (processed == todayPromos.size) {
                                    if (withRatings.isEmpty()) {
                                        binding.tvPromotionsEmpty.visibility = View.VISIBLE
                                        promotionsAdapter.submitList(emptyList())
                                    } else {
                                        val sorted = withRatings
                                            .sortedByDescending { it.second }
                                            .map { it.first }
                                            .take(3)
                                        binding.tvPromotionsEmpty.visibility = View.GONE
                                        promotionsAdapter.submitList(sorted)
                                    }
                                }
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Błąd wczytywania promocji: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // TOP promo
    private fun loadTopRatedPromotions() {
        val promosRef = FirebaseDatabase.getInstance().getReference("promotions")
        val votesRef = FirebaseDatabase.getInstance().getReference("promotionVotes")

        promosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val today = sdf.parse(sdf.format(Date()))

                val activePromos = mutableListOf<Pair<String, Promotion>>() // <promoId, promo>

                for (child in snapshot.children) {
                    val promo = child.getValue(Promotion::class.java) ?: continue
                    try {
                        val start = sdf.parse(promo.startDate)
                        val end = sdf.parse(promo.endDate)

                        if (start != null && end != null && today != null) {
                            if (!today.before(start) && !today.after(end)) {
                                val promoId = child.key ?: continue
                                val fixed = promo.copy(id = promoId)
                                activePromos.add(promoId to fixed)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                if (activePromos.isEmpty()) {
                    binding.tvTopPromotionsEmpty.visibility = View.VISIBLE
                    topPromotionsAdapter.submitList(emptyList())
                    return
                }

                val withRatings = mutableListOf<Pair<Promotion, Int>>()
                var processed = 0

                for ((promoId, promo) in activePromos) {
                    votesRef.child(promoId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(voteSnap: DataSnapshot) {
                                var rating = 0
                                for (voteChild in voteSnap.children) {
                                    val value = voteChild.getValue(Int::class.java) ?: 0
                                    rating += value // 1 = plus, -1 = minus
                                }

                                withRatings.add(promo to rating)
                                processed++

                                if (processed == activePromos.size) {
                                    applyTopPromotions(withRatings)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                processed++
                                if (processed == activePromos.size) {
                                    applyTopPromotions(withRatings)
                                }
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Błąd wczytywania promocji: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun applyTopPromotions(withRatings: List<Pair<Promotion, Int>>) {
        if (withRatings.isEmpty()) {
            binding.tvTopPromotionsEmpty.visibility = View.VISIBLE
            topPromotionsAdapter.submitList(emptyList())
            return
        }

        val sorted = withRatings
            .sortedByDescending { it.second }  // sort od najlepszej
            .take(3)

        val onlyPromos = sorted.map { it.first }

        // mapa: promoId -> rating
        val ratingMap = sorted.associate { (promo, rating) ->
            promo.id to rating
        }

        binding.tvTopPromotionsEmpty.visibility = View.GONE
        topPromotionsAdapter.updateRatings(ratingMap)
        topPromotionsAdapter.submitList(onlyPromos)
    }

    private fun loadShoppingListsPreview() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref =
            FirebaseDatabase.getInstance().getReference("users/$uid/shoppingLists")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val lists = mutableListOf<ShoppingList>()
                for (child in snapshot.children) {
                    val list = child.getValue(ShoppingList::class.java) ?: continue

                    val itemsCount = child.child("items").childrenCount.toInt()

                    val fixed = list.copy(
                        id = list.id.ifEmpty { child.key ?: "" },
                        itemsCount = itemsCount
                    )

                    lists.add(fixed)
                }

                shoppingListsAdapter.submitList(lists.take(3))
                binding.tvShoppingListsEmpty.visibility =
                    if (lists.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
