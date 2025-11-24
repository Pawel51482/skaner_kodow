package com.example.skaner_kodow.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.ItemHomePromotionBinding
import com.example.skaner_kodow.ui.promotions.Promotion

class HomePromotionsAdapter(
    private val onClick: (Promotion) -> Unit,
    private val showRating: Boolean = false
) : RecyclerView.Adapter<HomePromotionsAdapter.PromoViewHolder>() {

    private val items = mutableListOf<Promotion>()
    private val ratings = mutableMapOf<String, Int>()

    fun submitList(list: List<Promotion>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun updateRatings(map: Map<String, Int>) {
        ratings.clear()
        ratings.putAll(map)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val binding = ItemHomePromotionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PromoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PromoViewHolder(
        private val binding: ItemHomePromotionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(promotion: Promotion) {
            binding.tvPromotionTitle.text = promotion.title
            binding.tvPromotionSubtitle.text = promotion.description

            if (promotion.imageUrl.isNotEmpty()) {
                Glide.with(binding.root)
                    .load(promotion.imageUrl)
                    .into(binding.ivPromotion)
            } else {
                binding.ivPromotion.setImageDrawable(null)
            }

            if (showRating) {
                val ratingValue = ratings[promotion.id] ?: 0
                binding.tvPromotionRating.visibility = View.VISIBLE
                binding.tvPromotionRating.text = ratingValue.toString()

                // ukrycie opisu dla top promo
                binding.tvPromotionSubtitle.visibility = View.GONE

            } else {
                binding.tvPromotionRating.visibility = View.GONE

                // dla promo ważnych dzisiaj pokazujemy opis
                binding.tvPromotionSubtitle.visibility = View.VISIBLE
                binding.tvPromotionSubtitle.text = promotion.description
            }


            binding.root.setOnClickListener { onClick(promotion) }
        }
    }
}
