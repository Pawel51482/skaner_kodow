package com.example.skaner_kodow.ui.promotions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skaner_kodow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class PromotionsAdapter(
    private val onClick: (Promotion) -> Unit,
    private val onLongClick: (Promotion) -> Unit
) : ListAdapter<Promotion, PromotionsAdapter.PromotionViewHolder>(DiffCallback()) {

    // Ulubione
    private var favoriteIds: Set<String> = emptySet()
    private var onFavoriteClick: (String) -> Unit = {}

    // Zaktualizuj listę ID ulubionych promocji
    fun submitFavorites(favs: Set<String>) {
        favoriteIds = favs
        notifyDataSetChanged()
    }

    // listener kliknięcia w serduszko
    fun setOnFavoriteClickListener(listener: (String) -> Unit) {
        onFavoriteClick = listener
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    class PromotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.ivPromotionImage)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val desc: TextView = itemView.findViewById(R.id.tvDescription)
        private val barcode: TextView = itemView.findViewById(R.id.tvBarcode)
        private val dates: TextView = itemView.findViewById(R.id.tvDates)
        private val favIcon: ImageView? = itemView.findViewById(R.id.ivFavorite)
        private val ivVotePlus: ImageView? = itemView.findViewById(R.id.ivVotePlus)
        private val ivVoteMinus: ImageView? = itemView.findViewById(R.id.ivVoteMinus)
        private val tvPlusCount: TextView? = itemView.findViewById(R.id.tvPlusCount)
        private val tvMinusCount: TextView? = itemView.findViewById(R.id.tvMinusCount)

        fun bind(
            promotion: Promotion,
            favIds: Set<String>,
            onFavClick: (String) -> Unit,
            auth: FirebaseAuth,
            db: FirebaseDatabase
        ) {
            title.text = promotion.title
            // opis skrócony na liście
            desc.text = promotion.description
            barcode.text = "Kod: ${promotion.barcode}"
            dates.text = "${promotion.startDate} – ${promotion.endDate}"

            // Obrazek
            if (!promotion.imageUrl.isNullOrEmpty()) {
                image.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(promotion.imageUrl)
                    .into(image)
            } else {
                image.visibility = View.GONE
            }

            // Serduszko ulubionych
            favIcon?.let {
                val isFav = favIds.contains(promotion.id)
                it.setImageResource(
                    if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                it.setOnClickListener { _ -> onFavClick(promotion.id) }
                it.visibility = View.VISIBLE
            }


            val uid = auth.currentUser?.uid
            val promoId = promotion.id

            if (ivVotePlus != null && ivVoteMinus != null &&
                tvPlusCount != null && tvMinusCount != null &&
                promoId.isNotEmpty()
            ) {
                // domyślny stan
                tvPlusCount.text = "0"
                tvMinusCount.text = "0"
                ivVotePlus.alpha = 0.5f
                ivVoteMinus.alpha = 0.5f

                if (uid == null) {
                    // niezalogowany - tlyko zabezpieczenie
                    ivVotePlus.isEnabled = false
                    ivVoteMinus.isEnabled = false
                    return
                }

                val votesRef = db.getReference("promotionVotes").child(promoId)

                // nasłuch zmian tylko dla tej jednej promocji - aktualizuje licznik i własną ocene
                votesRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var plus = 0
                        var minus = 0
                        var myVote = 0

                        for (child in snapshot.children) {
                            val v = child.getValue(Int::class.java) ?: 0
                            if (v > 0) plus++
                            if (v < 0) minus++
                            if (child.key == uid) {
                                myVote = v
                            }
                        }

                        tvPlusCount.text = plus.toString()
                        tvMinusCount.text = minus.toString()

                        ivVotePlus.tag = myVote
                        ivVoteMinus.tag = myVote

                        ivVotePlus.alpha = if (myVote == 1) 1.0f else 0.5f
                        ivVoteMinus.alpha = if (myVote == -1) 1.0f else 0.5f
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

                // Pozytywna ocena
                ivVotePlus.setOnClickListener {
                    val ctx = itemView.context
                    // nie można głosować na swoją promocję
                    if (promotion.addedBy == uid) {
                        Toast.makeText(ctx, "Nie możesz ocenić swojej promocji", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val currentVote = (ivVotePlus.tag as? Int) ?: 0
                    val newRef = db.getReference("promotionVotes").child(promoId).child(uid)

                    if (currentVote == 1) {
                        // kliknięcie drugi raz = cofnięcie głosu
                        newRef.setValue(null)
                    } else {
                        newRef.setValue(1)
                    }
                }

                // ocena negatywna
                ivVoteMinus.setOnClickListener {
                    val ctx = itemView.context
                    if (promotion.addedBy == uid) {
                        Toast.makeText(ctx, "Nie możesz ocenić swojej promocji", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val currentVote = (ivVoteMinus.tag as? Int) ?: 0
                    val newRef = db.getReference("promotionVotes").child(promoId).child(uid)

                    if (currentVote == -1) {
                        // kliknięcie drugi raz - cofnięcie głosu
                        newRef.setValue(null)
                    } else {
                        newRef.setValue(-1)
                    }
                }

            } else {
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promotion, parent, false)
        return PromotionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, favoriteIds, onFavoriteClick, auth, db)

        holder.itemView.setOnClickListener {
            onClick(item)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Promotion>() {
        override fun areItemsTheSame(oldItem: Promotion, newItem: Promotion): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Promotion, newItem: Promotion): Boolean =
            oldItem == newItem
    }
}
