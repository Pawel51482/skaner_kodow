package com.example.skaner_kodow.ui.promotions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skaner_kodow.R

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


    class PromotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.ivPromotionImage)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val desc: TextView = itemView.findViewById(R.id.tvDescription)
        private val barcode: TextView = itemView.findViewById(R.id.tvBarcode)
        private val dates: TextView = itemView.findViewById(R.id.tvDates)
        private val favIcon: ImageView? = itemView.findViewById(R.id.ivFavorite)

        fun bind(
            promotion: Promotion,
            favIds: Set<String>,
            onFavClick: (String) -> Unit
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promotion, parent, false)
        return PromotionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, favoriteIds, onFavoriteClick)

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
