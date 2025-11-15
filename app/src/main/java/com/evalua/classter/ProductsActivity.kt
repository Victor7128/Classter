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
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import kotlinx.coroutines.launch

class ProductsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ProductsActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvProducts: RecyclerView
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var loadingOverlay: View
    private lateinit var adapter: ProductsAdapter

    private var sessionId: Int = -1
    private var sessionTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "=== onCreate INICIADO ===")
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Intentando inflar layout...")
            setContentView(R.layout.activity_products)
            Log.d(TAG, "✅ Layout inflado correctamente")

            Log.d(TAG, "Obteniendo extras del Intent...")
            sessionId = intent.getIntExtra("SESSION_ID", -1)
            sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: "Productos"
            Log.d(TAG, "SESSION_ID: $sessionId")
            Log.d(TAG, "SESSION_TITLE: $sessionTitle")

            if (sessionId == -1) {
                Log.e(TAG, "❌ SESSION_ID inválido")
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            Log.d(TAG, "Inicializando vistas...")
            initializeViews()

            Log.d(TAG, "Configurando componentes...")
            setupToolbar()
            setupRecyclerView()
            setupSwipeRefresh()
            setupFab()

            Log.d(TAG, "Cargando productos...")
            loadProducts()

            Log.d(TAG, "=== onCreate COMPLETADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPCIÓN FATAL en onCreate", e)
            Log.e(TAG, "Mensaje: ${e.message}")
            Log.e(TAG, "Causa: ${e.cause}")
            e.printStackTrace()
            Toast.makeText(this, "Error fatal: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            Log.d(TAG, "  Buscando toolbar...")
            toolbar = findViewById(R.id.toolbar)
            Log.d(TAG, "  ✅ toolbar encontrado: ${true}")

            Log.d(TAG, "  Buscando swipeRefresh...")
            swipeRefresh = findViewById(R.id.swipeRefresh)
            Log.d(TAG, "  ✅ swipeRefresh encontrado: ${true}")

            Log.d(TAG, "  Buscando rvProducts...")
            rvProducts = findViewById(R.id.rvProducts)
            Log.d(TAG, "  ✅ rvProducts encontrado: ${true}")

            Log.d(TAG, "  Buscando fabAddProduct...")
            fabAddProduct = findViewById(R.id.fabAddProduct)
            Log.d(TAG, "  ✅ fabAddProduct encontrado: ${true}")

            Log.d(TAG, "  Buscando loadingOverlay...")
            loadingOverlay = findViewById(R.id.loadingOverlay)
            Log.d(TAG, "  ✅ loadingOverlay encontrado: ${true}")

            Log.d(TAG, "✅ Todas las vistas inicializadas correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR al inicializar vistas", e)
            throw e
        }
    }

    private fun setupToolbar() {
        try {
            Log.d(TAG, "Configurando toolbar...")
            toolbar.title = "Producto - $sessionTitle"
            toolbar.setNavigationOnClickListener {
                Log.d(TAG, "Click en navegación, cerrando activity")
                finish()
            }
            Log.d(TAG, "✅ Toolbar configurado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR en setupToolbar", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d(TAG, "Configurando RecyclerView...")
            adapter = ProductsAdapter(
                products = emptyList(),
                onProductClick = { product ->
                    Log.d(TAG, "Click en producto: ${product.name}")
                    Toast.makeText(this, "Producto: ${product.name}", Toast.LENGTH_SHORT).show()
                },
                onEditClick = { product ->
                    Log.d(TAG, "Click en editar producto: ${product.name}")
                    showEditProductDialog(product)
                },
                onDeleteClick = { product ->
                    Log.d(TAG, "Click en eliminar producto: ${product.name}")
                    showDeleteConfirmationDialog(product)
                }
            )
            rvProducts.layoutManager = LinearLayoutManager(this)
            rvProducts.adapter = adapter
            Log.d(TAG, "✅ RecyclerView configurado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR en setupRecyclerView", e)
            throw e
        }
    }

    private fun setupSwipeRefresh() {
        try {
            Log.d(TAG, "Configurando SwipeRefresh...")
            swipeRefresh.setOnRefreshListener {
                Log.d(TAG, "SwipeRefresh activado")
                loadProducts()
            }
            Log.d(TAG, "✅ SwipeRefresh configurado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR en setupSwipeRefresh", e)
            throw e
        }
    }

    private fun setupFab() {
        try {
            Log.d(TAG, "Configurando FAB...")
            fabAddProduct.setOnClickListener {
                Log.d(TAG, "Click en FAB")
                showAddProductDialog()
            }
            Log.d(TAG, "✅ FAB configurado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR en setupFab", e)
            throw e
        }
    }

    private fun loadProducts() {
        Log.d(TAG, "=== loadProducts INICIADO ===")
        try {
            Log.d(TAG, "Mostrando loading overlay...")
            loadingOverlay.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = true
            Log.d(TAG, "Loading overlay visible")

            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Llamando API para obtener productos...")
                    val products = RetrofitClient.apiService.getProducts(sessionId)
                    Log.d(TAG, "✅ Productos recibidos: ${products.size}")

                    Log.d(TAG, "Actualizando adapter...")
                    adapter.updateProducts(products)
                    Log.d(TAG, "✅ Adapter actualizado")

                    if (products.isEmpty()) {
                        Log.d(TAG, "No hay productos, mostrando estado vacío")
                        fabAddProduct.visibility = View.VISIBLE
                        findViewById<View>(R.id.layoutEmptyState)?.visibility = View.VISIBLE
                        rvProducts.visibility = View.GONE
                    } else {
                        Log.d(TAG, "Hay ${products.size} producto(s), ocultando estado vacío")
                        fabAddProduct.visibility = View.GONE
                        findViewById<View>(R.id.layoutEmptyState)?.visibility = View.GONE
                        rvProducts.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ ERROR en llamada API", e)
                    Toast.makeText(
                        this@ProductsActivity,
                        "Error al cargar productos: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    Log.d(TAG, "Ocultando loading overlay...")
                    loadingOverlay.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    Log.d(TAG, "=== loadProducts FINALIZADO ===")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPCIÓN en loadProducts", e)
            throw e
        }
    }

    private fun showAddProductDialog() {
        Log.d(TAG, "Mostrando diálogo de agregar producto...")
        try {
            // Inflar el layout personalizado
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
            val etProductName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
            val etProductDescription = dialogView.findViewById<TextInputEditText>(R.id.etProductDescription)
            val btnAccept = dialogView.findViewById<MaterialButton>(R.id.btnAccept)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

            // Crear el diálogo SIN botones predeterminados
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Configurar el botón Crear
            btnAccept.setOnClickListener {
                val name = etProductName.text.toString().trim()
                val description = etProductDescription.text.toString().trim()

                Log.d(TAG, "Intentando crear producto: name='$name'")

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    etProductName.error = "Campo requerido"
                    return@setOnClickListener
                }

                if (name.length > 100) {
                    Toast.makeText(this, "El nombre no puede exceder 100 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (description.length > 200) {
                    Toast.makeText(this, "La descripción no puede exceder 200 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                dialog.dismiss()
                createProduct(name, description)
            }

            // Configurar el botón Cancelar
            btnCancel.setOnClickListener {
                Log.d(TAG, "Diálogo cancelado")
                dialog.dismiss()
            }

            dialog.show()
            Log.d(TAG, "✅ Diálogo mostrado correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR mostrando diálogo", e)
            Toast.makeText(this, "Error al mostrar diálogo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showEditProductDialog(product: Product) {
        Log.d(TAG, "Mostrando diálogo de editar producto: ${product.name}")
        try {
            // Inflar el layout personalizado
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
            val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
            val etProductName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
            val etProductDescription = dialogView.findViewById<TextInputEditText>(R.id.etProductDescription)
            val btnAccept = dialogView.findViewById<MaterialButton>(R.id.btnAccept)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

            // Cambiar el título del diálogo
            tvDialogTitle.text = "Editar Producto"
            btnAccept.text = "Guardar"

            // Pre-llenar los campos
            etProductName.setText(product.name)
            etProductDescription.setText(product.description ?: "")

            // Crear el diálogo
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Configurar el botón Guardar
            btnAccept.setOnClickListener {
                val name = etProductName.text.toString().trim()
                val description = etProductDescription.text.toString().trim()

                Log.d(TAG, "Intentando actualizar producto: name='$name'")

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    etProductName.error = "Campo requerido"
                    return@setOnClickListener
                }

                if (name.length > 100) {
                    Toast.makeText(this, "El nombre no puede exceder 100 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (description.length > 200) {
                    Toast.makeText(this, "La descripción no puede exceder 200 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                dialog.dismiss()
                updateProduct(product.id, name, description)
            }

            // Configurar el botón Cancelar
            btnCancel.setOnClickListener {
                Log.d(TAG, "Edición cancelada")
                dialog.dismiss()
            }

            dialog.show()
            Log.d(TAG, "✅ Diálogo de edición mostrado")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR mostrando diálogo de edición", e)
            Toast.makeText(this, "Error al mostrar diálogo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createProduct(name: String, description: String) {
        Log.d(TAG, "Creando producto: $name")
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = mapOf(
                    "name" to name,
                    "description" to description
                )
                Log.d(TAG, "Enviando request a API...")
                val newProduct = RetrofitClient.apiService.createProduct(sessionId, request)
                Log.d(TAG, "✅ Producto creado: ${newProduct.name}")

                Toast.makeText(
                    this@ProductsActivity,
                    "Producto creado: ${newProduct.name}",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR creando producto", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al crear producto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    private fun updateProduct(productId: Int, name: String, description: String) {
        Log.d(TAG, "Actualizando producto ID: $productId")
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = UpdateProductRequest(name = name, description = description)
                Log.d(TAG, "Enviando actualización a API...")
                val updatedProduct = RetrofitClient.apiService.updateProduct(productId, request)
                Log.d(TAG, "✅ Producto actualizado: ${updatedProduct.name}")

                Toast.makeText(
                    this@ProductsActivity,
                    "Producto actualizado",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR actualizando producto", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al actualizar producto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    private fun showProductOptionsMenu(product: Product, view: View) {
        Log.d(TAG, "Mostrando menú de opciones para: ${product.name}")
        try {
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_product_options, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        Log.d(TAG, "Opción: Editar")
                        showEditProductDialog(product)
                        true
                    }
                    R.id.action_delete -> {
                        Log.d(TAG, "Opción: Eliminar")
                        showDeleteConfirmationDialog(product)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
            Log.d(TAG, "✅ Menú mostrado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR mostrando menú", e)
        }
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        Log.d(TAG, "Mostrando confirmación de eliminación: ${product.name}")
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
        Log.d(TAG, "Eliminando producto ID: $productId")
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Llamando API para eliminar...")
                RetrofitClient.apiService.deleteProduct(productId)
                Log.d(TAG, "✅ Producto eliminado")

                Toast.makeText(
                    this@ProductsActivity,
                    "Producto eliminado. Ahora puedes crear uno nuevo.",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR eliminando producto", e)
                Toast.makeText(
                    this@ProductsActivity,
                    "Error al eliminar producto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                loadingOverlay.visibility = View.GONE
            }
        }
    }
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "=== onStart() ===")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== onResume() ===")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "=== onPause() ===")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "=== onStop() ===")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== onDestroy() - Activity siendo destruida ===")
    }

    override fun finish() {
        Log.d(TAG, "❌ finish() llamado - Activity cerrándose")
        Log.d(TAG, "Stack trace:", Exception("Trace de finish()"))
        super.finish()
    }
}