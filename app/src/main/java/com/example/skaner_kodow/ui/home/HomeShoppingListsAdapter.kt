package com.example.skaner_kodow.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skaner_kodow.databinding.ItemHomeShoppingListBinding
import com.example.skaner_kodow.ui.shoppinglists.ShoppingList

class HomeShoppingListsAdapter(
    private val onClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<HomeShoppingListsAdapter.ListViewHolder>() {

    private val items = mutableListOf<ShoppingList>()

    fun submitList(list: List<ShoppingList>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemHomeShoppingListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ListViewHolder(
        private val binding: ItemHomeShoppingListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(list: ShoppingList) {
            binding.tvListName.text = list.name
            binding.tvItemsCount.text = "${list.itemsCount} pozycji"
            binding.root.setOnClickListener { onClick(list) }
        }
    }
}
