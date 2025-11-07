package com.evalua.classter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evalua.classter.adapters.ProductsAdapter
import com.evalua.classter.models.Product
import com.evalua.classter.models.UpdateProductRequest
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.widget.ProgressBar
import kotlinx.coroutines.launch
import android.widget.TextView

class ProductsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ProductsActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvProducts: RecyclerView
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ProductsAdapter

    private var sessionId: Int = -1
    private var sessionTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        toolbar = findViewById(R.id.toolbar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvProducts = findViewById(R.id.rvProducts)
        fabAddProduct = findViewById(R.id.fabAddProduct)
        progressBar = findViewById(R.id.progressBar)

        sessionId = intent.getIntExtra("SESSION_ID", -1)
        sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: "Productos"

        if (sessionId == -1) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        loadProducts()
    }

    private fun setupToolbar() {
        toolbar.title = "Producto - $sessionTitle"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            products = emptyList(),
            onProductClick = { product ->
                // TODO: Navegar a detalles del producto o competencias
                Toast.makeText(this, "Producto: ${product.name}", Toast.LENGTH_SHORT).show()
            },
            onProductOptionsClick = { product, view ->
                showProductOptionsMenu(product, view)
            }
        )
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun setupFab() {
        fabAddProduct.setOnClickListener {
            showAddProductDialog()
        }
    }

    private fun loadProducts() {
        progressBar.visibility = View.VISIBLE
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val products = RetrofitClient.apiService.getProducts(sessionId)
                Log.d(TAG, "Products loaded: ${products.size}")
                adapter.updateProducts(products)

                if (products.isEmpty()) {
                    fabAddProduct.visibility = View.VISIBLE
                    Toast.makeText(
                        this@ProductsActivity,
                        "No hay producto registrado. Crea uno para esta sesión.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    fabAddProduct.visibility = View.GONE
                    Log.d(TAG, "Producto ya existe, ocultando FAB")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading products", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al cargar productos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAddProductDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val etProductName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etProductDescription = dialogView.findViewById<TextInputEditText>(R.id.etProductDescription)

        MaterialAlertDialogBuilder(this)
            .setTitle("Crear Producto")
            .setMessage("Solo se permite un producto por sesión.")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = etProductName.text.toString().trim()
                val description = etProductDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createProduct(name, description)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditProductDialog(product: Product) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val etProductName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etProductDescription = dialogView.findViewById<TextInputEditText>(R.id.etProductDescription)

        etProductName.setText(product.name)
        etProductDescription.setText(product.description ?: "")

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Producto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etProductName.text.toString().trim()
                val description = etProductDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateProduct(product.id, name, description)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createProduct(name: String, description: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = mapOf(
                    "name" to name,
                    "description" to description
                )
                val newProduct = RetrofitClient.apiService.createProduct(sessionId, request)
                Log.d(TAG, "Product created: $newProduct")

                Toast.makeText(
                    this@ProductsActivity,
                    "Producto creado: ${newProduct.name}",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating product", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al crear producto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateProduct(productId: Int, name: String, description: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = UpdateProductRequest(name = name, description = description)
                val updatedProduct = RetrofitClient.apiService.updateProduct(productId, request)
                Log.d(TAG, "Product updated: $updatedProduct")

                Toast.makeText(
                    this@ProductsActivity,
                    "Producto actualizado",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating product", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al actualizar producto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showProductOptionsMenu(product: Product, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_product_options, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditProductDialog(product)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog(product)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que deseas eliminar el producto: ${product.name}?\n\nPodrás crear uno nuevo después.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteProduct(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProduct(productId: Int) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteProduct(productId)
                Log.d(TAG, "Product deleted: $productId")

                Toast.makeText(
                    this@ProductsActivity,
                    "Producto eliminado. Ahora puedes crear uno nuevo.",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting product", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al eliminar producto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}