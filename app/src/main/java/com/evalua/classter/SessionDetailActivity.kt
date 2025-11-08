package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evalua.classter.models.Competency
import com.evalua.classter.models.Product
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionDetailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SessionDetailActivity"
    }

    private var sessionId: Int = -1
    private var sessionNumber: Int = -1
    private var sessionTitle: String = ""
    private var sessionDate: String = ""
    private var sectionId: Int = -1
    private var sectionName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_detail)

        // Obtener datos del Intent
        sessionId = intent.getIntExtra("SESSION_ID", -1)
        sessionNumber = intent.getIntExtra("SESSION_NUMBER", -1)
        sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: ""
        sessionDate = intent.getStringExtra("SESSION_DATE") ?: ""
        sectionId = intent.getIntExtra("SECTION_ID", -1)
        sectionName = intent.getStringExtra("SECTION_NAME") ?: ""

        Log.d(TAG, "=== Datos recibidos en SessionDetailActivity ===")
        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Session Number: $sessionNumber")
        Log.d(TAG, "Session Title: $sessionTitle")
        Log.d(TAG, "Session Date: $sessionDate")
        Log.d(TAG, "Section ID: $sectionId")
        Log.d(TAG, "Section Name: $sectionName")

        if (sessionId == -1 || sessionTitle.isEmpty()) {
            Log.e(TAG, "¡ERROR! Faltan datos en el Intent")
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupUI()
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sesión $sessionNumber"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        val tvSessionTitle = findViewById<TextView>(R.id.tvSessionTitle)
        val tvSessionDate = findViewById<TextView>(R.id.tvSessionDate)
        val tvSectionInfo = findViewById<TextView>(R.id.tvSectionInfo)
        val tvGradeInfo = findViewById<TextView>(R.id.tvGradeInfo)
        val tvBimesterInfo = findViewById<TextView>(R.id.tvBimesterInfo) // ← NUEVO

        tvSessionTitle.text = sessionTitle
        tvSessionDate.text = formatDate(sessionDate)

        // Extraer información de la sección: "5° A - II Bimestre"
        val sectionParts = sectionName.split(" - ")
        if (sectionParts.size >= 2) {
            val gradeSection = sectionParts[0] // "5° A"
            val bimesterPart = sectionParts[1] // "II Bimestre"

            val gradeParts = gradeSection.split(" ")
            if (gradeParts.size >= 2) {
                tvGradeInfo.text = gradeParts[0] // "5°"
                tvSectionInfo.text = gradeParts[1] // "A"
            }

            // Extraer solo el número/nombre del bimestre
            val bimester = bimesterPart.replace(" Bimestre", "").trim()
            tvBimesterInfo.text = bimester // "II"
        }

        Log.d(TAG, "UI configurada:")
        Log.d(TAG, "  Título: ${tvSessionTitle.text}")
        Log.d(TAG, "  Fecha: ${tvSessionDate.text}")
        Log.d(TAG, "  Sección: ${tvSectionInfo.text}")
        Log.d(TAG, "  Grado: ${tvGradeInfo.text}")
        Log.d(TAG, "  Bimestre: ${tvBimesterInfo.text}") // ← NUEVO
    }

    private fun setupClickListeners() {
        Log.d(TAG, "=== Iniciando setupClickListeners ===")

        // Card Producto
        val cardProduct = findViewById<MaterialCardView>(R.id.cardProduct)
        Log.d(TAG, "cardProduct encontrado: ${cardProduct != null}")
        if (cardProduct != null) {
            cardProduct.setOnClickListener {
                Log.d(TAG, "✅ Click en Producto detectado")
                try {
                    openProductsActivity()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al abrir ProductsActivity", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.e(TAG, "❌ cardProduct es NULL")
        }

        // Card Competencias
        val cardCompetencies = findViewById<MaterialCardView>(R.id.cardCompetencies)
        Log.d(TAG, "cardCompetencies encontrado: ${cardCompetencies != null}")
        if (cardCompetencies != null) {
            cardCompetencies.setOnClickListener {
                Log.d(TAG, "✅ Click en Competencias detectado")
                try {
                    openCompetenciesDashboard()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al abrir CompetenciesDashboard", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.e(TAG, "❌ cardCompetencies es NULL")
        }

        // Card Evaluación
        val cardEvaluation = findViewById<MaterialCardView>(R.id.cardEvaluation)
        Log.d(TAG, "cardEvaluation encontrado: ${cardEvaluation != null}")
        if (cardEvaluation != null) {
            cardEvaluation.setOnClickListener {
                Log.d(TAG, "✅ Click en Evaluación detectado")
                try {
                    showEvaluationSelector()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al mostrar selector", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.e(TAG, "❌ cardEvaluation es NULL")
        }

        Log.d(TAG, "=== setupClickListeners completado ===")
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun openProductsActivity() {
        val intent = Intent(this, ProductsActivity::class.java).apply {
            putExtra("SESSION_ID", sessionId)
            putExtra("SESSION_TITLE", sessionTitle)
        }

        Log.d(TAG, "Abriendo ProductsActivity con:")
        Log.d(TAG, "  SESSION_ID: $sessionId")
        Log.d(TAG, "  SESSION_TITLE: $sessionTitle")

        startActivity(intent)
    }

    private fun openCompetenciesDashboard() {
        val intent = Intent(this, CompetenciesDashboardActivity::class.java).apply {
            putExtra("SESSION_ID", sessionId)
            putExtra("SESSION_TITLE", sessionTitle)
        }

        Log.d(TAG, "Abriendo CompetenciesDashboardActivity con:")
        Log.d(TAG, "  SESSION_ID: $sessionId")
        Log.d(TAG, "  SESSION_TITLE: $sessionTitle")

        startActivity(intent)
    }

    private fun showEvaluationSelector() {
        lifecycleScope.launch {
            try {
                // Cargar competencias y productos en paralelo
                val competenciesDeferred = async {
                    RetrofitClient.apiService.getCompetencies(sessionId)
                }
                val productsDeferred = async {
                    RetrofitClient.apiService.getProducts(sessionId)
                }

                val competencies = competenciesDeferred.await()
                val products = productsDeferred.await()

                Log.d(TAG, "Competencias para evaluación: ${competencies.size}")
                Log.d(TAG, "Productos para evaluación: ${products.size}")

                when {
                    competencies.isEmpty() -> {
                        Toast.makeText(
                            this@SessionDetailActivity,
                            "⚠️ Primero debes agregar competencias a esta sesión",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    products.isEmpty() -> {
                        Toast.makeText(
                            this@SessionDetailActivity,
                            "⚠️ Primero debes agregar productos a esta sesión",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        // ✅ PRODUCTO ÚNICO - Solo elegir competencia
                        val uniqueProduct = products[0]
                        showCompetencyOnlySelector(competencies, uniqueProduct)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data for evaluation", e)
                Toast.makeText(
                    this@SessionDetailActivity,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openEvaluationActivity(competency: Competency, product: Product) {
        val intent = Intent(this, EvaluationActivity::class.java).apply {
            putExtra("SESSION_ID", sessionId)
            putExtra("SESSION_TITLE", sessionTitle)
            putExtra("COMPETENCY_ID", competency.id)
            putExtra("COMPETENCY_NAME", competency.name)
            putExtra("PRODUCT_ID", product.id)
            putExtra("PRODUCT_NAME", product.name)
        }

        Log.d(TAG, "Abriendo EvaluationActivity con:")
        Log.d(TAG, "  SESSION_ID: $sessionId")
        Log.d(TAG, "  COMPETENCY_ID: ${competency.id}")
        Log.d(TAG, "  PRODUCT_ID: ${product.id}")

        startActivity(intent)
    }

    private fun showCompetencyOnlySelector(
        competencies: List<Competency>,
        product: Product
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_competency_selector, null)
        val spinnerCompetencies = dialogView.findViewById<Spinner>(R.id.spinnerCompetencies)
        val tvProductInfo = dialogView.findViewById<TextView>(R.id.tvProductInfo)

        // Mostrar el producto seleccionado automáticamente
        tvProductInfo.text = "Producto: ${product.name}"

        // Setup spinner de competencias
        val competencyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            competencies.map { it.name }
        )
        competencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCompetencies.adapter = competencyAdapter

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Abrir Evaluación") { _, _ ->
                val selectedCompetency = competencies[spinnerCompetencies.selectedItemPosition]
                openEvaluationActivity(selectedCompetency, product)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "=== onRestart() - Volviendo de otra Activity ===")
    }
}