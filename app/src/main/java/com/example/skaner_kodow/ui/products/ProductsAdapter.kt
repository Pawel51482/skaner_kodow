package com.example.skaner_kodow.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.ItemProductBinding
import java.util.Locale

class ProductsAdapter(
    private val isAdmin: Boolean,
    private val onProductClick: (Product) -> Unit,
    private val onProductLongClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    private var products: List<Product> = emptyList()

    inner class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.textViewName.text = product.name

            if (product.price > 0.0) {
                binding.textViewPrice.visibility = View.VISIBLE
                val priceFormatted =
                    String.format(Locale.getDefault(), "%.2f zł", product.price)
                binding.textViewPrice.text = priceFormatted
            } else {
                binding.textViewPrice.visibility = View.GONE
            }

            Glide.with(binding.imageViewProduct.context)
                .load(product.imageUrl)
                .into(binding.imageViewProduct)

            binding.root.setOnClickListener {
                onProductClick(product)
            }

            if (isAdmin) {
                binding.root.setOnLongClickListener {
                    onProductLongClick(product)
                    true
                }
            } else {
                // zwykły user – brak menu
                binding.root.setOnLongClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun submitList(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
