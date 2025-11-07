package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Product

class ProductsAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onProductOptionsClick: (Product, View) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductNumber: TextView = view.findViewById(R.id.tvProductNumber)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductDescription: TextView = view.findViewById(R.id.tvProductDescription)
        val btnProductOptions: ImageButton = view.findViewById(R.id.btnProductOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductNumber.text = product.number.toString()
        holder.tvProductName.text = product.name

        // Mostrar descripción solo si no está vacía
        if (product.description.isNullOrEmpty()) {
            holder.tvProductDescription.visibility = View.GONE
        } else {
            holder.tvProductDescription.visibility = View.VISIBLE
            holder.tvProductDescription.text = product.description
        }

        holder.itemView.setOnClickListener {
            onProductClick(product)
        }

        holder.btnProductOptions.setOnClickListener {
            onProductOptionsClick(product, it)
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}