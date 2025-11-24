package com.example.skaner_kodow.ui.promotions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentPromotionDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PromotionDetailsFragment : Fragment() {

    private lateinit var binding: FragmentPromotionDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPromotionDetailsBinding.inflate(inflater, container, false)

        val args = arguments
        val title = args?.getString("title") ?: ""
        val description = args?.getString("description") ?: ""
        val barcode = args?.getString("barcode") ?: ""
        val startDate = args?.getString("startDate") ?: ""
        val endDate = args?.getString("endDate") ?: ""
        val imageUrl = args?.getString("imageUrl") ?: ""
        val promoId = args?.getString("id") ?: ""

        binding.tvTitle.text = title
        binding.tvDescription.text = description
        binding.tvBarcode.text = "Kod produktu: $barcode"
        binding.tvDates.text = "Obowiązuje: $startDate – $endDate"

        if (imageUrl.isNotEmpty()) {
            binding.ivImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .into(binding.ivImage)
            binding.ivImage.setOnClickListener {
                com.example.skaner_kodow.utils.ImagePreview(imageUrl)
                    .show(parentFragmentManager, "promo_image_preview")
            }
        } else {
            binding.ivImage.visibility = View.GONE
        }

        // sprawdzamy czy promocja jest zakończona
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = sdf.parse(sdf.format(Date()))
        val end = try {
            sdf.parse(endDate)
        } catch (e: Exception) {
            null
        }

        // true jeśli endDate < dzisiaj
        val isExpired = today != null && end != null && end.before(today)

        if (isExpired) {
            binding.tvPromoExpiredOverlay.visibility = View.VISIBLE

            // wyszarzamy sekcję głosów + serduszko
            binding.layoutVotesDetails.alpha = 0.5f
            binding.ivVotePlus.isEnabled = false
            binding.ivVoteMinus.isEnabled = false

            binding.ivFavoriteDetails.alpha = 0.5f
            binding.ivFavoriteDetails.isEnabled = false
        } else {
            binding.tvPromoExpiredOverlay.visibility = View.GONE

            binding.layoutVotesDetails.alpha = 1f
            binding.ivVotePlus.isEnabled = true
            binding.ivVoteMinus.isEnabled = true

            binding.ivFavoriteDetails.alpha = 1f
            binding.ivFavoriteDetails.isEnabled = true
        }


        if (promoId.isNotEmpty()) {
            val db = FirebaseDatabase.getInstance()
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid

            // Oceny dla promocji
            val votesRef = db.getReference("promotionVotes").child(promoId)

            votesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var plus = 0
                    var minus = 0
                    var myVote = 0

                    val currentUid = auth.currentUser?.uid

                    for (child in snapshot.children) {
                        val v = child.getValue(Int::class.java) ?: 0
                        if (v > 0) plus++
                        if (v < 0) minus++
                        if (child.key == currentUid) {
                            myVote = v
                        }
                    }

                    binding.tvPlusCount.text = plus.toString()
                    binding.tvMinusCount.text = minus.toString()

                    // zapamiętujemy ocene
                    binding.ivVotePlus.tag = myVote
                    binding.ivVoteMinus.tag = myVote

                    binding.ivVotePlus.alpha = if (myVote == 1) 1.0f else 0.5f
                    binding.ivVoteMinus.alpha = if (myVote == -1) 1.0f else 0.5f
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // ocena pozytywna
            binding.ivVotePlus.setOnClickListener {
                binding.ivVotePlus.setOnClickListener {
                    if (isExpired) {
                        Toast.makeText(requireContext(), "Ta promocja jest już zakończona", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }


                val currentUid = auth.currentUser?.uid ?: return@setOnClickListener

                // nie można głosować na swoją promocję
                if (args?.getString("addedBy") == currentUid) {
                    Toast.makeText(requireContext(), "Nie możesz ocenić swojej promocji", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentVote = (binding.ivVotePlus.tag as? Int) ?: 0
                val myVoteRef = votesRef.child(currentUid)

                if (currentVote == 1) {
                    // drugi klik cofa ocene
                    myVoteRef.setValue(null)
                } else {
                    myVoteRef.setValue(1)
                }
            }

            // ocena negatywna
            binding.ivVoteMinus.setOnClickListener {
                if (isExpired) {
                    Toast.makeText(requireContext(), "Ta promocja jest już zakończona", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentUid = auth.currentUser?.uid ?: return@setOnClickListener

                if (args?.getString("addedBy") == currentUid) {
                    Toast.makeText(requireContext(), "Nie możesz ocenić swojej promocji", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentVote = (binding.ivVoteMinus.tag as? Int) ?: 0
                val myVoteRef = votesRef.child(currentUid)

                if (currentVote == -1) {
                    myVoteRef.setValue(null)
                } else {
                    myVoteRef.setValue(-1)
                }
            }

            // Ulubione
            val currentUid = uid
            if (currentUid != null) {
                val favRef = db.getReference("users/$currentUid/favorites/promotions/$promoId")

                // śledzimy czy promocja jest w ulubionych
                favRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isFav = snapshot.exists()
                        binding.ivFavoriteDetails.setImageResource(
                            if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                        )
                        // zapamiętujemy stan
                        binding.ivFavoriteDetails.tag = isFav
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

                // kliknięcie w serduszko
                binding.ivFavoriteDetails.setOnClickListener {
                    val currentFlag = binding.ivFavoriteDetails.tag as? Boolean ?: false
                    if (currentFlag) {
                        // jesli bylo ulubione - usuń
                        favRef.setValue(null)
                    } else {
                        // jak nie było w ulubionych - dodaj
                        favRef.setValue(true)
                    }
                }
            }
        }
        return binding.root
    }
}
