package com.evalua.classter

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.evalua.classter.adapters.CompetenciesDashboardAdapter
import com.evalua.classter.adapters.CompetenciasTemplateAdapter
import com.evalua.classter.models.*
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompetenciesDashboardActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CompetenciesDashboard"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etBuscarCompetencias: TextInputEditText
    private lateinit var spinnerAreas: Spinner
    private lateinit var cardAgregarCompetencia: MaterialCardView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvCompetencies: RecyclerView
    private lateinit var loadingOverlay: View  // ✅ CAMBIO: De ProgressBar a View
    private lateinit var adapter: CompetenciesDashboardAdapter

    private var sessionId: Int = -1
    private var sessionTitle: String = ""
    private var areas: List<Area> = emptyList()
    private var todasCompetenciasTemplate: List<CompetenciaTemplate> = emptyList()
    private var competenciasActuales: List<Competency> = emptyList()
    private var competencyAbilities: Map<Int, List<Ability>> = emptyMap()
    private var abilityCriteria: Map<Int, List<Criterion>> = emptyMap()

    private lateinit var layoutEmptyState: LinearLayout

    // Datos del usuario
    private lateinit var sharedPreferences: SharedPreferences
    private var userAreaId: Int = 0
    private var userRole: String = ""
    private var userAreaName: String = ""

    // Estados de expansión
    private var expandedCompetencyId: Int? = null
    private var expandedAbilityId: Int? = null

    private lateinit var tvAreaInfo: TextView
    private lateinit var cardAreaInfo: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "=== onCreate INICIADO ===")
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Intentando inflar layout...")
            setContentView(R.layout.activity_competencies_dashboard)
            Log.d(TAG, "✅ Layout inflado correctamente")

            Log.d(TAG, "Obteniendo SharedPreferences...")
            sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            userAreaId = sharedPreferences.getInt("user_area_id", 0)
            userRole = sharedPreferences.getString("user_role", "") ?: ""
            userAreaName = sharedPreferences.getString("user_area_name", "") ?: ""
            Log.d(TAG, "Usuario: Rol=$userRole, Área=$userAreaName (ID=$userAreaId)")

            Log.d(TAG, "Inicializando vistas...")
            initViews()

            Log.d(TAG, "Obteniendo extras del Intent...")
            sessionId = intent.getIntExtra("SESSION_ID", -1)
            sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: "Competencias"
            Log.d(TAG, "SESSION_ID: $sessionId")
            Log.d(TAG, "SESSION_TITLE: $sessionTitle")

            if (sessionId == -1) {
                Log.e(TAG, "❌ SESSION_ID inválido")
                Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            Log.d(TAG, "Configurando componentes...")
            setupToolbar()
            setupRecyclerView()
            setupSwipeRefresh()
            setupSearchAndFilters()
            setupButtons()

            Log.d(TAG, "Actualizando info del área...")
            updateAreaInfo()

            Log.d(TAG, "Cargando datos iniciales...")
            loadInitialData()

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

    private fun initViews() {
        try {
            Log.d(TAG, "  Buscando toolbar...")
            toolbar = findViewById(R.id.toolbar)
            Log.d(TAG, "  ✅ toolbar encontrado: ${toolbar != null}")

            Log.d(TAG, "  Buscando etBuscarCompetencias...")
            etBuscarCompetencias = findViewById(R.id.etBuscarCompetencias)
            Log.d(TAG, "  ✅ etBuscarCompetencias encontrado: ${etBuscarCompetencias != null}")

            Log.d(TAG, "  Buscando spinnerAreas...")
            spinnerAreas = findViewById(R.id.spinnerAreas)
            Log.d(TAG, "  ✅ spinnerAreas encontrado: ${spinnerAreas != null}")

            Log.d(TAG, "  Buscando cardAgregarCompetencia...")
            cardAgregarCompetencia = findViewById(R.id.cardAgregarCompetencia)
            Log.d(TAG, "  ✅ cardAgregarCompetencia encontrado: ${cardAgregarCompetencia != null}")

            Log.d(TAG, "  Buscando swipeRefresh...")
            swipeRefresh = findViewById(R.id.swipeRefresh)
            Log.d(TAG, "  ✅ swipeRefresh encontrado: ${swipeRefresh != null}")

            Log.d(TAG, "  Buscando rvCompetencies...")
            rvCompetencies = findViewById(R.id.rvCompetencies)
            Log.d(TAG, "  ✅ rvCompetencies encontrado: ${rvCompetencies != null}")

            Log.d(TAG, "  Buscando loadingOverlay...")
            loadingOverlay = findViewById(R.id.loadingOverlay)
            Log.d(TAG, "  ✅ loadingOverlay encontrado: ${loadingOverlay != null}")

            Log.d(TAG, "  Buscando tvAreaInfo...")
            tvAreaInfo = findViewById(R.id.tvAreaInfo)
            Log.d(TAG, "  ✅ tvAreaInfo encontrado: ${tvAreaInfo != null}")

            Log.d(TAG, "  Buscando cardAreaInfo...")
            cardAreaInfo = findViewById(R.id.cardAreaInfo)
            Log.d(TAG, "  ✅ cardAreaInfo encontrado: ${cardAreaInfo != null}")

            Log.d(TAG, "  Buscando layoutEmptyState...")
            layoutEmptyState = findViewById(R.id.tvEmptyState)  // Sí, el ID sigue siendo tvEmptyState
            Log.d(TAG, "  ✅ layoutEmptyState encontrado: ${layoutEmptyState != null}")

            Log.d(TAG, "✅ Todas las vistas inicializadas correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR al inicializar vistas", e)
            throw e
        }
    }

    private fun setupToolbar() {
        val areaInfo = if (userAreaName.isNotEmpty()) " - $userAreaName" else ""
        toolbar.title = "Competencias$areaInfo - $sessionTitle"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun updateAreaInfo() {
        Log.d(TAG, "🔍 Actualizando información del área - Rol: $userRole, Área: '$userAreaName'")

        when {
            userRole == "ADMIN" -> {
                tvAreaInfo.text = "👑 Modo ADMIN - Todas las áreas disponibles"
                cardAreaInfo.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            }

            userRole == "DOCENTE" && userAreaId > 0 && userAreaName.isNotEmpty() -> {
                tvAreaInfo.text = "📚 Tu área: $userAreaName"
                cardAreaInfo.setCardBackgroundColor(Color.parseColor("#E8F5E8"))
            }

            userRole == "DOCENTE" && (userAreaId == 0 || userAreaName.isEmpty()) -> {
                tvAreaInfo.text = "⚠️ Área no asignada - Contacta al administrador"
                cardAreaInfo.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            }

            else -> {
                tvAreaInfo.text = "Rol: $userRole - Sin área"
                cardAreaInfo.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
            }
        }

        cardAreaInfo.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = CompetenciesDashboardAdapter(
            competencies = emptyList(),
            competencyAbilities = emptyMap(),
            abilityCriteria = emptyMap(),
            expandedCompetencyId = expandedCompetencyId,
            expandedAbilityId = expandedAbilityId,
            onCompetencyExpanded = { competencyId, isExpanded ->
                Log.d(TAG, "Competency expanded: $competencyId, expanded: $isExpanded")
                expandedCompetencyId = if (isExpanded) competencyId else null
                if (!isExpanded) expandedAbilityId = null
                loadAbilitiesForCompetency(competencyId)
            },
            onDeleteCompetency = { competency ->
                showDeleteCompetencyDialog(competency)
            },
            onAddAbility = { competencyId ->
                showAddAbilityDialog(competencyId)
            },
            onAbilityExpanded = { abilityId, isExpanded ->
                Log.d(TAG, "Ability expanded: $abilityId, expanded: $isExpanded")
                expandedAbilityId = if (isExpanded) abilityId else null
                loadCriteriaForAbility(abilityId)
            },
            onDeleteAbility = { ability ->
                showDeleteAbilityDialog(ability)
            },
            onAddCriterion = { abilityId ->
                showAddCriterionDialog(abilityId)
            },
            onEditCriterion = { criterion ->
                showEditCriterionDialog(criterion)
            },
            onDeleteCriterion = { criterion ->
                showDeleteCriterionDialog(criterion)
            }
        )
        rvCompetencies.layoutManager = LinearLayoutManager(this)
        rvCompetencies.adapter = adapter

        showEmptyState(true, "Cargando competencias...")
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadCompetencies()
        }
    }

    private fun setupSearchAndFilters() {
        setupAreaSpinner()

        etBuscarCompetencias.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCompetencies()
            }
        })
    }

    private fun setupButtons() {
        cardAgregarCompetencia.setOnClickListener {
            showSelectCompetencyDialog()
        }
    }

    private fun loadInitialData() {
        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO
        showEmptyState(true, "Cargando competencias...")

        lifecycleScope.launch {
            try {
                val areasDeferred = async {
                    if (userRole == "ADMIN") {
                        RetrofitClient.curriculumApiService.getAreas()
                    } else {
                        emptyList()
                    }
                }

                val competenciasTemplateDeferred = async {
                    if (userRole == "ADMIN") {
                        RetrofitClient.curriculumApiService.getAllCompetencias()
                    } else {
                        if (userAreaId > 0) {
                            val area: Area = RetrofitClient.curriculumApiService.getArea(userAreaId)
                            val competenciasDetalladas: List<CompetenciaDetallada> =
                                RetrofitClient.curriculumApiService.getCompetenciasByArea(userAreaId)

                            competenciasDetalladas.map { comp ->
                                CompetenciaTemplate(
                                    id = comp.id,
                                    nombre = comp.nombre,
                                    area_id = area.id,
                                    area_nombre = area.nombre
                                )
                            }
                        } else {
                            emptyList()
                        }
                    }
                }

                areas = areasDeferred.await()
                todasCompetenciasTemplate = competenciasTemplateDeferred.await()

                Log.d(TAG, "📊 Datos cargados - Áreas: ${areas.size}, Competencias: ${todasCompetenciasTemplate.size}")

                loadCompetencies()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading initial data", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState(true, "Error al cargar competencias")
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun setupAreaSpinner() {
        if (userRole == "ADMIN") {
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
                    filterCompetencies()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            findViewById<MaterialCardView>(R.id.cardFiltroArea)?.visibility = View.VISIBLE
        } else {
            findViewById<MaterialCardView>(R.id.cardFiltroArea)?.visibility = View.GONE
        }
    }

    private fun loadCompetencies() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando competencias para sesión ID: $sessionId")
                competenciasActuales = RetrofitClient.apiService.getCompetencies(sessionId)
                Log.d(TAG, "✅ Competencias cargadas: ${competenciasActuales.size}")

                competencyAbilities = emptyMap()
                abilityCriteria = emptyMap()

                updateAdapter()

                if (competenciasActuales.isEmpty()) {
                    showEmptyState(true, "No hay competencias en esta sesión.\n\nToca el botón de abajo para agregar competencias de tu área.")
                } else {
                    showEmptyState(false, "")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading competencies", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar competencias: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState(true, "Error al cargar competencias")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // ✅ CAMBIAR:
    private fun showEmptyState(show: Boolean, message: String) {
        if (show) {
            val tvMessage = layoutEmptyState.findViewById<TextView>(R.id.tvEmptyMessage)
            tvMessage?.text = message
            layoutEmptyState.visibility = View.VISIBLE
            rvCompetencies.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvCompetencies.visibility = View.VISIBLE
        }
    }

    private fun filterCompetencies() {
        val searchText = etBuscarCompetencias.text.toString().lowercase()
        val selectedAreaPosition = spinnerAreas.selectedItemPosition

        val filteredCompetencies = competenciasActuales.filter { competency ->
            val matchesSearch = searchText.isEmpty() ||
                    competency.name.lowercase().contains(searchText) ||
                    competency.description?.lowercase()?.contains(searchText) == true

            val matchesArea = if (userRole == "ADMIN") {
                selectedAreaPosition == 0 ||
                        competency.description?.contains(areas[selectedAreaPosition - 1].nombre) == true
            } else {
                true
            }

            matchesSearch && matchesArea
        }

        val filteredAbilities = competencyAbilities.filterKeys { competencyId ->
            filteredCompetencies.any { it.id == competencyId }
        }

        adapter.updateData(
            newCompetencies = filteredCompetencies,
            newAbilities = filteredAbilities,
            newCriteria = abilityCriteria,
            keepExpanded = true
        )

        if (filteredCompetencies.isEmpty() && competenciasActuales.isNotEmpty()) {
            showEmptyState(true, "No se encontraron competencias que coincidan con tu búsqueda.")
        } else if (filteredCompetencies.isEmpty()) {
            showEmptyState(true, "No hay competencias en esta sesión.\n\nToca el botón de abajo para agregar competencias de tu área.")
        } else {
            showEmptyState(false, "")
        }
    }

    private fun updateAdapter() {
        adapter.updateData(
            newCompetencies = competenciasActuales,
            newAbilities = competencyAbilities,
            newCriteria = abilityCriteria,
            keepExpanded = true
        )
        adapter.updateExpandedStates(expandedCompetencyId, expandedAbilityId)
    }

    private fun loadAbilitiesForCompetency(competencyId: Int) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando capacidades para competencia: $competencyId")
                val abilities = RetrofitClient.apiService.getAbilities(competencyId)
                Log.d(TAG, "✅ Capacidades cargadas: ${abilities.size}")

                competencyAbilities = competencyAbilities.toMutableMap().apply {
                    put(competencyId, abilities)
                }

                updateAdapter()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading abilities for competency $competencyId", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar capacidades: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadCriteriaForAbility(abilityId: Int) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando criterios para capacidad: $abilityId")
                val criteria = RetrofitClient.apiService.getCriteria(abilityId)
                Log.d(TAG, "✅ Criterios cargados: ${criteria.size}")

                abilityCriteria = abilityCriteria.toMutableMap().apply {
                    put(abilityId, criteria)
                }

                updateAdapter()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading criteria for ability $abilityId", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al cargar criterios: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSelectCompetencyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_competencies, null)
        val rvCompetenciasDialog = dialogView.findViewById<RecyclerView>(R.id.rvCompetenciasDialog)
        val tvAreaInfoDialog = dialogView.findViewById<TextView>(R.id.tvAreaInfo)

        if (userRole == "ADMIN") {
            tvAreaInfoDialog.text = "👑 Modo ADMIN - Todas las áreas disponibles"
            tvAreaInfoDialog.setBackgroundColor(Color.parseColor("#E3F2FD"))

            val selectionAdapter = CompetenciasTemplateAdapter(
                competencias = todasCompetenciasTemplate,
                onSelectionChanged = { }
            )
            rvCompetenciasDialog.layoutManager = LinearLayoutManager(this)
            rvCompetenciasDialog.adapter = selectionAdapter

        } else {
            tvAreaInfoDialog.text = "Área: $userAreaName"
            tvAreaInfoDialog.setBackgroundColor(Color.parseColor("#E8F5E8"))

            val competenciasDelArea = if (userAreaId > 0) {
                todasCompetenciasTemplate.filter { it.area_id == userAreaId }
            } else {
                emptyList()
            }

            val selectionAdapter = CompetenciasTemplateAdapter(
                competencias = competenciasDelArea,
                onSelectionChanged = { }
            )
            rvCompetenciasDialog.layoutManager = LinearLayoutManager(this)
            rvCompetenciasDialog.adapter = selectionAdapter

            if (competenciasDelArea.isEmpty()) {
                Toast.makeText(this,
                    "No hay competencias disponibles para tu área ($userAreaName)",
                    Toast.LENGTH_LONG).show()
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Agregar Seleccionadas") { _, _ ->
                val selectionAdapter = rvCompetenciasDialog.adapter as? CompetenciasTemplateAdapter
                val selected = selectionAdapter?.getSelectedCompetencias() ?: emptyList()
                addSelectedCompetencies(selected)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun addSelectedCompetencies(competencias: List<CompetenciaTemplate>) {
        if (competencias.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una competencia", Toast.LENGTH_SHORT).show()
            return
        }

        val competenciasExistentesNombres = competenciasActuales.map { it.name.lowercase() }
        val competenciasNuevas = competencias.filter { competencia ->
            !competenciasExistentesNombres.contains(competencia.nombre.lowercase())
        }
        val competenciasDuplicadas = competencias.filter { competencia ->
            competenciasExistentesNombres.contains(competencia.nombre.lowercase())
        }

        if (competenciasNuevas.isEmpty()) {
            val mensaje = if (competenciasDuplicadas.size == 1) {
                "⚠️ La competencia ya existe en esta sesión"
            } else {
                "⚠️ Las competencias seleccionadas ya existen en esta sesión"
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            return
        }

        if (competenciasDuplicadas.isNotEmpty()) {
            val mensaje = "⚠️ ${competenciasDuplicadas.size} competencia(s) ya existe(n), se agregarán ${competenciasNuevas.size} nueva(s)"
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }

        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                var savedCount = 0
                var errorCount = 0

                for (competencia in competenciasNuevas) {
                    try {
                        val request = NewCompetencyRequest(
                            name = competencia.nombre,
                            description = "Área: ${competencia.area_nombre}"
                        )
                        RetrofitClient.apiService.createCompetency(sessionId, request)
                        savedCount++
                        Log.d(TAG, "✅ Competencia guardada: ${competencia.nombre}")
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "❌ Error guardando competencia: ${competencia.nombre}", e)
                    }
                }

                val message = when {
                    errorCount == 0 && savedCount > 0 ->
                        "✅ $savedCount competencia(s) agregada(s) exitosamente"
                    errorCount > 0 && savedCount > 0 ->
                        "⚠️ $savedCount agregada(s), $errorCount con error(es)"
                    else ->
                        "❌ No se pudieron agregar las competencias"
                }

                Toast.makeText(this@CompetenciesDashboardActivity, message, Toast.LENGTH_LONG).show()

                if (savedCount > 0) {
                    loadCompetencies()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error general guardando competencias", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun showAddAbilityDialog(competencyId: Int) {
        lifecycleScope.launch {
            try {
                val competencia = competenciasActuales.find { it.id == competencyId }
                if (competencia == null) {
                    Toast.makeText(this@CompetenciesDashboardActivity,
                        "Error: Competencia no encontrada", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val competenciaTemplate = todasCompetenciasTemplate.find {
                    it.nombre.equals(competencia.name, ignoreCase = true)
                }

                if (competenciaTemplate == null) {
                    Toast.makeText(this@CompetenciesDashboardActivity,
                        "No se encontró esta competencia en el catálogo", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val capacidades = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getCapacidadesByCompetencia(competenciaTemplate.id)
                }

                if (capacidades.isEmpty()) {
                    Toast.makeText(this@CompetenciesDashboardActivity,
                        "No hay capacidades disponibles para esta competencia", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                showCapacidadesSelectionDialog(competencyId, capacidades)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading capacidades", e)
                Toast.makeText(this@CompetenciesDashboardActivity,
                    "Error al cargar capacidades: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCapacidadesSelectionDialog(competencyId: Int, capacidades: List<CapacidadDetallada>) {
        val capacidadNames = capacidades.map { it.nombre }.toTypedArray()
        val selectedItems = BooleanArray(capacidades.size)

        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar Capacidades")
            .setMultiChoiceItems(capacidadNames, selectedItems) { _, which, isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton("Agregar") { _, _ ->
                val selectedCapacidades = capacidades.filterIndexed { index, _ ->
                    selectedItems[index]
                }
                addSelectedCapacidades(competencyId, selectedCapacidades)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addSelectedCapacidades(competencyId: Int, capacidades: List<CapacidadDetallada>) {
        if (capacidades.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una capacidad", Toast.LENGTH_SHORT).show()
            return
        }

        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                val capacidadesExistentes = try {
                    RetrofitClient.apiService.getAbilities(competencyId)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron obtener capacidades existentes", e)
                    emptyList()
                }

                val capacidadesExistentesNombres = capacidadesExistentes.map { it.name?.lowercase() }

                val capacidadesNuevas = capacidades.filter { capacidad ->
                    !capacidadesExistentesNombres.contains(capacidad.nombre.lowercase())
                }
                val capacidadesDuplicadas = capacidades.filter { capacidad ->
                    capacidadesExistentesNombres.contains(capacidad.nombre.lowercase())
                }

                if (capacidadesNuevas.isEmpty()) {
                    val mensaje = if (capacidadesDuplicadas.size == 1) {
                        "⚠️ La capacidad ya existe en esta competencia"
                    } else {
                        "⚠️ Las ${capacidadesDuplicadas.size} capacidades seleccionadas ya existen en esta competencia"
                    }
                    Toast.makeText(this@CompetenciesDashboardActivity, mensaje, Toast.LENGTH_LONG).show()
                    loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
                    return@launch
                }

                if (capacidadesDuplicadas.isNotEmpty()) {
                    val mensaje = "⚠️ ${capacidadesDuplicadas.size} capacidad(es) ya existe(n), se agregarán ${capacidadesNuevas.size} nueva(s)"
                    Toast.makeText(this@CompetenciesDashboardActivity, mensaje, Toast.LENGTH_LONG).show()
                }

                var savedCount = 0
                var errorCount = 0

                for (capacidad in capacidadesNuevas) {
                    try {
                        val request = mapOf(
                            "name" to capacidad.nombre,
                            "description" to (capacidad.descripcion ?: "Capacidad curricular")
                        )
                        RetrofitClient.apiService.createAbility(competencyId, request)
                        savedCount++
                        Log.d(TAG, "✅ Capacidad guardada: ${capacidad.nombre}")
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "❌ Error guardando capacidad: ${capacidad.nombre}", e)
                    }
                }

                val message = when {
                    errorCount == 0 && savedCount > 0 ->
                        "✅ $savedCount capacidad(es) agregada(s) exitosamente"
                    errorCount > 0 && savedCount > 0 ->
                        "⚠️ $savedCount agregada(s), $errorCount con error(es)"
                    else ->
                        "❌ No se pudieron agregar las capacidades"
                }

                Toast.makeText(this@CompetenciesDashboardActivity, message, Toast.LENGTH_LONG).show()

                if (savedCount > 0) {
                    loadAbilitiesForCompetency(competencyId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error general guardando capacidades", e)
                Toast.makeText(this@CompetenciesDashboardActivity,
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun showAddCriterionDialog(abilityId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_criterion, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCriterionName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etCriterionDescription)

        MaterialAlertDialogBuilder(this)
            .setTitle("Crear Criterio de Evaluación")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre del criterio es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createCriterion(abilityId, name, description)
            }
            .setNegativeButton("Cancelar", null)
            .show()

        etName.requestFocus()
    }

    private fun createCriterion(abilityId: Int, name: String, description: String) {
        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                val criteriosExistentes = try {
                    RetrofitClient.apiService.getCriteria(abilityId)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron obtener criterios existentes", e)
                    emptyList()
                }

                val criterioExiste = criteriosExistentes.any {
                    it.name?.lowercase() == name.lowercase()
                }

                if (criterioExiste) {
                    Toast.makeText(
                        this@CompetenciesDashboardActivity,
                        "⚠️ Ya existe un criterio con el nombre \"$name\" en esta capacidad",
                        Toast.LENGTH_LONG
                    ).show()
                    loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
                    return@launch
                }

                val request = mapOf(
                    "name" to name,
                    "description" to description
                )
                val newCriterion = RetrofitClient.apiService.createCriterion(abilityId, request)
                Log.d(TAG, "Criterion created: $newCriterion")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "✅ Criterio creado: $name",
                    Toast.LENGTH_SHORT
                ).show()

                loadCriteriaForAbility(abilityId)

            } catch (e: Exception) {
                Log.e(TAG, "Error creating criterion", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al crear criterio: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun showDeleteCompetencyDialog(competency: Competency) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Competencia")
            .setMessage("¿Estás seguro de que deseas eliminar la competencia \"${competency.name}\"?\n\n⚠️ Esta competencia proviene del Currículo Nacional y se eliminará de esta sesión junto con todas sus capacidades y criterios asociados.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCompetency(competency.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteAbilityDialog(ability: Ability) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Capacidad")
            .setMessage("¿Estás seguro de que deseas eliminar la capacidad \"${ability.name}\"?\n\n⚠️ Esta capacidad proviene del Currículo Nacional y se eliminará junto con todos sus criterios asociados.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAbility(ability.id, ability.competency_id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteCriterionDialog(criterion: Criterion) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Criterio")
            .setMessage("¿Estás seguro de que deseas eliminar el criterio \"${criterion.name}\"?\n\nEste criterio fue creado manualmente y se eliminará permanentemente.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCriterion(criterion.id, criterion.ability_id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCompetency(competencyId: Int) {
        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteCompetency(competencyId)
                Log.d(TAG, "Competency deleted: $competencyId")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Competencia eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                loadCompetencies()

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting competency", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al eliminar competencia: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun deleteAbility(abilityId: Int, competencyId: Int) {
        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteAbility(abilityId)
                Log.d(TAG, "Ability deleted: $abilityId")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Capacidad eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                loadAbilitiesForCompetency(competencyId)

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ability", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al eliminar capacidad: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun deleteCriterion(criterionId: Int, abilityId: Int) {
        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteCriterion(criterionId)
                Log.d(TAG, "Criterion deleted: $criterionId")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Criterio eliminado",
                    Toast.LENGTH_SHORT
                ).show()

                loadCriteriaForAbility(abilityId)

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting criterion", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al eliminar criterio: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }

    private fun showEditCriterionDialog(criterion: Criterion) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_criterion, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCriterionName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etCriterionDescription)

        etName.setText(criterion.name)
        etDescription.setText(criterion.description ?: "")

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Criterio de Evaluación")
            .setView(dialogView)
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre del criterio es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (name == criterion.name && description == (criterion.description ?: "")) {
                    Toast.makeText(this, "No se realizaron cambios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateCriterion(criterion.id, criterion.ability_id, name, description)
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Eliminar") { _, _ ->
                showDeleteCriterionDialog(criterion)
            }
            .show()

        etName.selectAll()
        etName.requestFocus()
    }

    private fun updateCriterion(criterionId: Int, abilityId: Int, name: String, description: String) {
        loadingOverlay.visibility = View.VISIBLE  // ✅ CAMBIO

        lifecycleScope.launch {
            try {
                val criteriosExistentes = try {
                    RetrofitClient.apiService.getCriteria(abilityId)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron obtener criterios existentes", e)
                    emptyList()
                }

                val criterioConMismoNombre = criteriosExistentes.find {
                    it.name?.lowercase() == name.lowercase() && it.id != criterionId
                }

                if (criterioConMismoNombre != null) {
                    Toast.makeText(
                        this@CompetenciesDashboardActivity,
                        "⚠️ Ya existe otro criterio con el nombre \"$name\" en esta capacidad",
                        Toast.LENGTH_LONG
                    ).show()
                    loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
                    return@launch
                }

                val request = UpdateCriterionRequest(
                    name = name,
                    description = if (description.isEmpty()) null else description
                )
                val updatedCriterion = RetrofitClient.apiService.updateCriterion(criterionId, request)
                Log.d(TAG, "Criterion updated: $updatedCriterion")

                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "✅ Criterio actualizado: $name",
                    Toast.LENGTH_SHORT
                ).show()

                loadCriteriaForAbility(abilityId)

            } catch (e: Exception) {
                Log.e(TAG, "Error updating criterion", e)
                Toast.makeText(
                    this@CompetenciesDashboardActivity,
                    "Error al actualizar criterio: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingOverlay.visibility = View.GONE  // ✅ CAMBIO
            }
        }
    }
}