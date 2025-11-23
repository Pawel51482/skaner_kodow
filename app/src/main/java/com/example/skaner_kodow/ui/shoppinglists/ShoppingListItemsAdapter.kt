package com.example.skaner_kodow.ui.shoppinglists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.ItemShoppingListItemBinding
import com.example.skaner_kodow.ui.products.Product

class ShoppingListItemsAdapter(
    private val onCheckClick: (String, Boolean) -> Unit,
    private val onLongClick: (ShoppingListItem) -> Unit
) : RecyclerView.Adapter<ShoppingListItemsAdapter.ItemViewHolder>() {

    private var items: List<Pair<ShoppingListItem, Product>> = emptyList()

    inner class ItemViewHolder(val binding: ItemShoppingListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: Pair<ShoppingListItem, Product>) {
            val (item, product) = pair

            binding.tvName.text = if (item.customName.isNotEmpty()) {
                item.customName
            } else {
                product.name
            }
            binding.tvQuantity.text = "Ilość: ${item.quantity}"

            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.isChecked = item.checked
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                onCheckClick(item.id, isChecked)
            }

            // jeśli użytkownik dodał swoje zdjęcie użyj go, inaczej zdjęcie z produktu
            val imageUrlToShow = if (item.itemImageUrl.isNotEmpty()) {
                item.itemImageUrl
            } else {
                product.imageUrl
            }

            Glide.with(binding.root.context)
                .load(imageUrlToShow)
                .into(binding.ivProduct)

            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemShoppingListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<Pair<ShoppingListItem, Product>>) {
        items = newList
        notifyDataSetChanged()
    }
}
