package com.example.skaner_kodow.ui.shoppinglists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skaner_kodow.databinding.ItemShoppingListBinding

class ShoppingListsAdapter(
    private val onClick: (ShoppingList) -> Unit,
    private val onLongClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListsAdapter.ShoppingListViewHolder>() {

    private var lists: List<ShoppingList> = emptyList()

    inner class ShoppingListViewHolder(
        val binding: ItemShoppingListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShoppingList) {
            binding.tvListName.text = item.name
            binding.tvCreatedAt.text = item.createdAt

            val countText = if (item.itemsCount == 1) {
                "1 pozycja"
            } else {
                "${item.itemsCount} pozycji"
            }
            binding.tvItemsCount.text = countText

            // zwykły klik - wejście w szczegóły
            binding.root.setOnClickListener {
                onClick(item)
            }

            // długi klik - menu usuwania listy
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val binding = ItemShoppingListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShoppingListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        holder.bind(lists[position])
    }

    override fun getItemCount(): Int = lists.size

    fun submitList(newLists: List<ShoppingList>) {
        lists = newLists
        notifyDataSetChanged()
    }
}
