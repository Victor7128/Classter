package com.evalua.classter

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.evalua.classter.adapters.CompetenciasTemplateAdapter
import com.evalua.classter.models.*
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class CompetenciesActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CompetenciesActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var spinnerAreas: Spinner
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvCompetencias: RecyclerView
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnGuardarCompetencias: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: CompetenciasTemplateAdapter

    private var sessionId: Int = -1
    private var sessionTitle: String = ""
    private var areas: List<Area> = emptyList()
    private var todasCompetencias: List<CompetenciaTemplate> = emptyList()
    private var competenciasExistentes: List<Competency> = emptyList()

    // Datos del usuario
    private lateinit var sharedPreferences: SharedPreferences
    private var userAreaId: Int = 0
    private var userRole: String = ""
    private var userAreaName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_competencies)

        // Obtener datos del usuario
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userAreaId = sharedPreferences.getInt("user_area_id", 0)
        userRole = sharedPreferences.getString("user_role", "") ?: ""
        userAreaName = sharedPreferences.getString("user_area_name", "") ?: ""

        toolbar = findViewById(R.id.toolbar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvCompetencias = findViewById(R.id.rvCompetencias)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        btnGuardarCompetencias = findViewById(R.id.btnGuardarCompetencias)
        progressBar = findViewById(R.id.progressBar)

        sessionId = intent.getIntExtra("SESSION_ID", -1)
        sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: "Competencias"

        if (sessionId == -1) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupButtons()
        loadInitialData()
        setupAreaInfo()
    }
    private fun setupAreaInfo() {
        val tvAreaInfo = findViewById<TextView>(R.id.tvAreaInfo)
        if (userAreaName.isNotEmpty()) {
            tvAreaInfo.text = "Área: $userAreaName"
        } else {
            tvAreaInfo.text = "Área: No asignada"
            tvAreaInfo.setBackgroundColor(Color.parseColor("#FFEBEE")) // Color de advertencia
        }
    }

    private fun setupToolbar() {
        val areaInfo = if (userAreaName.isNotEmpty()) " - $userAreaName" else ""
        toolbar.title = "Competencias$areaInfo - $sessionTitle"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CompetenciasTemplateAdapter(
            competencias = emptyList(),
            onSelectionChanged = { selectedCompetencias ->
                updateSelectionUI(selectedCompetencias)
            }
        )
        rvCompetencias.layoutManager = LinearLayoutManager(this)
        rvCompetencias.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadCompetenciasByArea()
        }
    }

    private fun setupButtons() {
        btnGuardarCompetencias.setOnClickListener {
            guardarCompetenciasSeleccionadas()
        }
    }

    private fun loadInitialData() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Cargar áreas solo si es ADMIN
                val areasDeferred = async {
                    if (userRole == "ADMIN") {
                        RetrofitClient.curriculumApiService.getAreas()
                    } else {
                        emptyList()
                    }
                }

                val competenciasExistentesDeferred = async {
                    RetrofitClient.apiService.getCompetencies(sessionId)
                }

                areas = areasDeferred.await()
                competenciasExistentes = competenciasExistentesDeferred.await()

                Log.d(TAG, "Rol del usuario: $userRole")
                Log.d(TAG, "Área del usuario: $userAreaId - $userAreaName")
                Log.d(TAG, "Competencias existentes: ${competenciasExistentes.size}")

                setupAreaSpinner()
                loadAllCompetencias()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                Toast.makeText(
                    this@CompetenciesActivity,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadAllCompetencias() {
        lifecycleScope.launch {
            try {
                // Cargar competencias según el rol del usuario
                todasCompetencias = if (userRole == "ADMIN") {
                    // ADMIN: Usar el endpoint /competencias que ya devuelve area_id y area_nombre
                    withContext(Dispatchers.IO) {
                        RetrofitClient.curriculumApiService.getAllCompetencias()
                    }
                } else {
                    // DOCENTE: Obtener competencias de su área
                    if (userAreaId > 0) {
                        withContext(Dispatchers.IO) {
                            // ✅ Primero obtener el área para tener su nombre
                            val area: Area = RetrofitClient.curriculumApiService.getArea(userAreaId)

                            // ✅ Luego obtener las competencias del área
                            val competenciasDetalladas: List<CompetenciaDetallada> =
                                RetrofitClient.curriculumApiService.getCompetenciasByArea(userAreaId)

                            // ✅ Convertir CompetenciaDetallada a CompetenciaTemplate
                            competenciasDetalladas.map { comp ->
                                CompetenciaTemplate(
                                    id = comp.id,
                                    nombre = comp.nombre,
                                    area_id = area.id,
                                    area_nombre = area.nombre
                                )
                            }
                        }
                    } else {
                        emptyList()
                    }
                }

                Log.d(TAG, "✅ Competencias cargadas: ${todasCompetencias.size}")
                adapter.updateCompetencias(todasCompetencias)

                // Mejorar el mensaje de estado
                if (todasCompetencias.isEmpty()) {
                    val message = when {
                        userRole != "ADMIN" && userAreaId == 0 ->
                            "No tienes un área asignada. Contacta al administrador."
                        userRole != "ADMIN" && userAreaId > 0 ->
                            "No se encontraron competencias para tu área ($userAreaName)"
                        else ->
                            "No hay competencias disponibles en el sistema"
                    }
                    Toast.makeText(this@CompetenciesActivity, message, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading all competencias", e)
                Toast.makeText(
                    this@CompetenciesActivity,
                    "Error al cargar competencias: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadCompetenciasByArea(areaId: Int? = null) {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val competencias = if (areaId == null) {
                    todasCompetencias
                } else {
                    // Solo para ADMIN - filtrar por área específica
                    withContext(Dispatchers.IO) {
                        val area: Area = RetrofitClient.curriculumApiService.getArea(areaId)
                        val competenciasDetalladas: List<CompetenciaDetallada> =
                            RetrofitClient.curriculumApiService.getCompetenciasByArea(areaId)

                        competenciasDetalladas.map { comp ->
                            CompetenciaTemplate(
                                id = comp.id,
                                nombre = comp.nombre,
                                area_id = area.id,
                                area_nombre = area.nombre
                            )
                        }
                    }
                }

                Log.d(TAG, "Competencias filtradas: ${competencias.size}")
                adapter.updateCompetencias(competencias)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading competencias by area", e)
                Toast.makeText(
                    this@CompetenciesActivity,
                    "Error al filtrar competencias: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateSelectionUI(selectedCompetencias: List<CompetenciaTemplate>) {
        val count = selectedCompetencias.size
        tvSelectedCount.text = if (count == 0) {
            "Ninguna competencia seleccionada"
        } else {
            "$count competencia${if (count > 1) "s" else ""} seleccionada${if (count > 1) "s" else ""}"
        }

        btnGuardarCompetencias.isEnabled = count > 0
    }

    private fun guardarCompetenciasSeleccionadas() {
        val selected = adapter.getSelectedCompetencias()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una competencia", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Guardar Competencias")
            .setMessage("¿Deseas guardar ${selected.size} competencia(s) para esta sesión?")
            .setPositiveButton("Guardar") { _, _ ->
                saveCompetenciasToSession(selected)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveCompetenciasToSession(competencias: List<CompetenciaTemplate>) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                var savedCount = 0
                var errorCount = 0

                for (competencia in competencias) {
                    try {
                        val request = NewCompetencyRequest(
                            name = competencia.nombre,
                            description = "Área: ${competencia.area_nombre}"
                        )
                        RetrofitClient.apiService.createCompetency(sessionId, request)
                        savedCount++
                        Log.d(TAG, "Competencia guardada: ${competencia.nombre}")
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "Error guardando competencia: ${competencia.nombre}", e)
                    }
                }

                val message = if (errorCount == 0) {
                    "✅ $savedCount competencia(s) guardada(s) exitosamente"
                } else {
                    "⚠️ $savedCount guardada(s), $errorCount con error(es)"
                }

                Toast.makeText(this@CompetenciesActivity, message, Toast.LENGTH_LONG).show()

                if (savedCount > 0) {
                    adapter.clearSelection()
                    finish()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error general guardando competencias", e)
                Toast.makeText(
                    this@CompetenciesActivity,
                    "Error al guardar competencias: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupAreaSpinner() {
        // Configurar spinner según el rol del usuario
        if (userRole == "ADMIN") {
            // ADMIN puede seleccionar entre todas las áreas
            val areasWithAll = listOf(Area(0, "Todas las áreas")) + areas
            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                areasWithAll.map { it.nombre }
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAreas.adapter = spinnerAdapter

            spinnerAreas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedArea = areasWithAll[position]
                    Log.d(TAG, "Área seleccionada: ${selectedArea.nombre}")
                    loadCompetenciasByArea(if (selectedArea.id == 0) null else selectedArea.id)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else {
            // DOCENTE solo ve su área
            val userAreaList = if (userAreaName.isNotEmpty()) {
                listOf(Area(userAreaId, userAreaName))
            } else {
                listOf(Area(0, "Sin área asignada"))
            }

            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                userAreaList.map { it.nombre }
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAreas.adapter = spinnerAdapter

            // Deshabilitar el spinner para docentes
            spinnerAreas.isEnabled = false

            // Mostrar mensaje informativo
            if (userAreaName.isEmpty()) {
                Toast.makeText(this, "No tienes un área asignada", Toast.LENGTH_LONG).show()
            }
        }
    }
}