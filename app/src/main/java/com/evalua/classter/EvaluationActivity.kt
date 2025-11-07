package com.evalua.classter

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.evrencoskun.tableview.TableView
import com.evrencoskun.tableview.listener.ITableViewListener
import com.evalua.classter.adapters.TableViewAdapter
import com.evalua.classter.models.*
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.withContext
import java.util.Locale

class EvaluationActivity : AppCompatActivity(), ITableViewListener {
    companion object {
        private const val TAG = "EvaluationActivity"
    }

    // ✅ Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvSessionInfo: TextView
    private lateinit var tvCompetencyName: TextView
    private lateinit var tableView: TableView
    private lateinit var btnSaveAll: MaterialButton
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var llAbilitiesContainer: LinearLayout
    private lateinit var llAbilitiesList: LinearLayout
    private lateinit var tvRegistrationDate: TextView
    private val currentDate: String by lazy {
        getCurrentFormattedDate()
    }

    // ✅ TableView components
    private lateinit var tableAdapter: TableViewAdapter

    // ✅ Datos del Intent
    private var sessionId: Int = -1
    private var competencyId: Int = -1
    private var productId: Int = -1
    private var sessionTitle: String = ""
    private var evaluationContext: EvaluationContext? = null
    private val currentEvaluations = mutableMapOf<String, String?>()
    private val currentObservations = mutableMapOf<String, String?>()
    private val pendingChanges = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evaluation)

        initViews()
        setupRegistrationDate()
        getIntentData()
        setupToolbar()
        setupTableView()
        setupButtons()

        loadEvaluationData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvSessionInfo = findViewById(R.id.tvSessionInfo)
        tvCompetencyName = findViewById(R.id.tvCompetencyName)

        // ✅ NUEVAS VIEWS para fechas y usuario
        tvRegistrationDate = findViewById(R.id.tvRegistrationDate)


        llAbilitiesContainer = findViewById(R.id.llAbilitiesContainer)
        llAbilitiesList = findViewById(R.id.llAbilitiesList)

        tableView = findViewById(R.id.tableView)
        btnSaveAll = findViewById(R.id.btnSaveAll)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRegistrationDate() {
        tvRegistrationDate.text = currentDate
        Log.d(TAG, "📅 Fecha de registro establecida: $currentDate")
    }

    private fun getCurrentFormattedDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
        return dateFormat.format(calendar.time)
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateTimeFormat.format(calendar.time)
    }

    private fun getIntentData() {
        sessionId = intent.getIntExtra("SESSION_ID", -1)
        competencyId = intent.getIntExtra("COMPETENCY_ID", -1)
        productId = intent.getIntExtra("PRODUCT_ID", -1)
        sessionTitle = intent.getStringExtra("SESSION_TITLE") ?: "Evaluación"

        Log.d(TAG, "Intent data: SESSION_ID=$sessionId, COMPETENCY_ID=$competencyId, PRODUCT_ID=$productId")

        if (sessionId == -1 || competencyId == -1 || productId == -1) {
            Toast.makeText(this, "Error: Datos de evaluación no válidos", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        toolbar.title = "Evaluación - $sessionTitle"
        toolbar.setNavigationOnClickListener {
            if (hasUnsavedChanges()) {
                showUnsavedChangesDialog()
            } else {
                finish()
            }
        }
    }

    private fun setupTableView() {
        // ✅ Crear adapter
        tableAdapter = TableViewAdapter(
            context = this,
            onGradeChanged = { studentId, criterionId, grade ->
                handleGradeChange(studentId, criterionId, grade)
            },
            onObservationClicked = { studentId, criterionId ->
                showObservationDialog(studentId, criterionId)
            }
        )

        // ✅ Configuración TableView
        tableView.setAdapter(tableAdapter)

        // ✅ AUMENTAR SIGNIFICATIVAMENTE el ancho para nombres completos
        tableView.rowHeaderWidth = 450 // ✅ Aumentado de 400 a 450

        // ✅ Separadores
        tableView.isShowHorizontalSeparators = true
        tableView.isShowVerticalSeparators = true

        try {
            tableView.separatorColor = "#DDDDDD".toColorInt()
        } catch (e: Exception) {
            Log.w(TAG, "setSeparatorColor no disponible")
        }

        tableView.tableViewListener = this
    }

    private fun setupButtons() {
        btnSaveAll.setOnClickListener { saveAllEvaluations() }
    }

    private fun loadEvaluationData() {
        Log.d(TAG, "🚀 Iniciando carga de datos...")
        Log.d(TAG, "  SessionId: $sessionId")
        Log.d(TAG, "  CompetencyId: $competencyId")
        Log.d(TAG, "  ProductId: $productId")

        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "📡 Llamando a getEvaluationContext...")

                // ✅ Cargar contexto de evaluación
                val context = RetrofitClient.apiService.getEvaluationContext(
                    sessionId = sessionId,
                    competencyId = competencyId,
                    productId = productId
                )

                Log.d(TAG, "✅ Contexto recibido exitosamente")
                evaluationContext = context

                // ✅ Preparar datos actuales
                prepareCurrentData(context)

                // ✅ Mostrar en TableView
                displayEvaluationTable(context)

                Log.d(TAG, "📊 Tabla mostrada exitosamente:")
                Log.d(TAG, "  Students: ${context.students.size}")
                Log.d(TAG, "  Abilities: ${context.abilities.size}")
                Log.d(TAG, "  Criteria: ${context.criteria.size}")
                Log.d(TAG, "  Values: ${context.values.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando datos de evaluación", e)
                Log.e(TAG, "  Error message: ${e.message}")
                Log.e(TAG, "  Error cause: ${e.cause}")

                Toast.makeText(this@EvaluationActivity,
                    "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun prepareCurrentData(context: EvaluationContext) {
        // ✅ Preparar evaluaciones actuales
        currentEvaluations.clear()
        currentObservations.clear()

        context.values.forEach { value ->
            val key = "${value.student_id}_${value.criterion_id}"
            currentEvaluations[key] = value.value
            currentObservations[key] = value.observation
        }

        pendingChanges.clear()
    }

    private fun displayEvaluationTable(context: EvaluationContext) {
        // ✅ Actualizar información del header
        tvSessionInfo.text = parseSessionInfo(sessionTitle)
        tvCompetencyName.text = "🎯 Competencia: ${context.competency.display_name ?: context.competency.name}"
        tvRegistrationDate.text = currentDate

        // ✅ Mostrar capacidades
        displayAbilities(context.abilities)

        // ✅ Preparar datos para TableView
        val columnHeaders = context.criteria.map { criterion ->
            ColumnHeaderModel(
                id = "criterion_${criterion.id}",
                title = criterion.display_name ?: criterion.name ?: "Criterio ${criterion.id}"
            )
        }

        // ✅ APLICAR FORMATO DE NOMBRE CON SALTO DE LÍNEA
        val rowHeaders = context.students.map { student ->
            RowHeaderModel(
                id = "student_${student.id}",
                title = formatStudentName(student.full_name), // ✅ Usar función de formato
                studentId = student.id
            )
        }

        val cells = mutableListOf<MutableList<CellModel>>()
        context.students.forEach { student ->
            val studentCells = mutableListOf<CellModel>()
            context.criteria.forEach { criterion ->
                val key = "${student.id}_${criterion.id}"
                val cellModel = CellModel(
                    id = "cell_${student.id}_${criterion.id}",
                    studentId = student.id,
                    criterionId = criterion.id,
                    currentValue = currentEvaluations[key],
                    hasObservation = !currentObservations[key].isNullOrEmpty()
                )
                studentCells.add(cellModel)
            }
            cells.add(studentCells)
        }

        // ✅ Establecer datos en TableView
        tableAdapter.setAllItems(columnHeaders, rowHeaders, cells)

        Toast.makeText(this,
            "✅ Evaluaciones del $currentDate cargadas: ${context.students.size} estudiantes",
            Toast.LENGTH_SHORT).show()
    }

    private fun displayAbilities(abilities: List<Ability>) {
        // Limpiar capacidades anteriores
        llAbilitiesList.removeAllViews()

        if (abilities.isNotEmpty()) {
            abilities.forEachIndexed { index, ability ->
                val abilityView = createAbilityView(index + 1, ability)
                llAbilitiesList.addView(abilityView)
            }

            // Mostrar el contenedor
            llAbilitiesContainer.visibility = View.VISIBLE
        } else {
            // Ocultar si no hay capacidades
            llAbilitiesContainer.visibility = View.GONE
        }
    }

    private fun createAbilityView(number: Int, ability: Ability): TextView {
        return TextView(this).apply {
            val abilityText = "${number}. ${ability.display_name ?: ability.name ?: "Capacidad $number"}"
            text = abilityText
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD // ✅ NEGRITA
            setTextColor("#333333".toColorInt())
            setPadding(16, 4, 16, 4)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END

            // ✅ Fondo sutil para cada capacidad
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 2, 0, 2)
            setLayoutParams(layoutParams)

            setBackgroundColor("#FFFFFF".toColorInt())
        }
    }

    private fun handleGradeChange(studentId: Int, criterionId: Int, grade: String?) {
        val key = "${studentId}_$criterionId"

        // ✅ Log con fecha y hora del cambio
        Log.d(TAG, "📝 Cambio registrado el ${getCurrentDateTime()}: Estudiante $studentId, Criterio $criterionId = $grade")

        // ✅ Actualizar datos locales
        currentEvaluations[key] = grade

        // ✅ Marcar como cambio pendiente
        pendingChanges.add(key)

        // ✅ Actualizar botón de guardar con fecha
        updateSaveButton()
    }

    private fun showObservationDialog(studentId: Int, criterionId: Int) {
        val key = "${studentId}_$criterionId"
        val currentObservation = currentObservations[key] ?: ""

        val editText = android.widget.EditText(this).apply {
            setText(currentObservation)
            hint = "Escribe tu observación aquí..."
            setPadding(16, 16, 16, 16)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("📝 Observación - $currentDate")
            .setMessage("Estudiante: ${getStudentName(studentId)}\nCriterio: ${getCriterionName(criterionId)}")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val observation = editText.text.toString().trim()
                currentObservations[key] = if (observation.isNotEmpty()) observation else null

                // ✅ Log con fecha
                Log.d(TAG, "📝 Observación registrada el ${getCurrentDateTime()}: $key")

                updateCellObservation(studentId, criterionId, observation.ifEmpty { null })
                pendingChanges.add(key)
                updateSaveButton()

                Toast.makeText(this, "💾 Observación guardada - $currentDate", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpiar") { _, _ ->
                currentObservations[key] = null
                updateCellObservation(studentId, criterionId, null)
                pendingChanges.add(key)
                updateSaveButton()
                Toast.makeText(this, "🗑️ Observación eliminada - $currentDate", Toast.LENGTH_SHORT).show()
            }
            .show()

        editText.selectAll()
        editText.requestFocus()
    }

    private fun updateCellObservation(studentId: Int, criterionId: Int, observation: String?) {
        // ✅ Buscar y actualizar la celda específica
        val context = evaluationContext ?: return

        val studentIndex = context.students.indexOfFirst { it.id == studentId }
        val criterionIndex = context.criteria.indexOfFirst { it.id == criterionId }

        if (studentIndex >= 0 && criterionIndex >= 0) {
            val key = "${studentId}_$criterionId"
            val updatedCell = CellModel(
                id = "cell_${studentId}_$criterionId",
                studentId = studentId,
                criterionId = criterionId,
                currentValue = currentEvaluations[key],
                hasObservation = !observation.isNullOrEmpty()
            )

            // Actualizar en TableView
            tableAdapter.changeCellItem(criterionIndex, studentIndex, updatedCell)
        }
    }

    private fun updateSaveButton() {
        val changesCount = pendingChanges.size
        btnSaveAll.isEnabled = changesCount > 0
        btnSaveAll.text = if (changesCount > 0) {
            "💾 Guardar Cambios ($changesCount) - $currentDate"
        } else {
            "💾 Guardar Evaluaciones"
        }
    }

    private fun saveAllEvaluations() {
        if (pendingChanges.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show()
            return
        }
        val keysToSave = pendingChanges.toList() // snapshot
        Log.d(TAG, "💾 Guardando ${keysToSave.size} evaluaciones (snapshot) el ${getCurrentDateTime()}")

        showLoading(true)
        btnSaveAll.isEnabled = false

        lifecycleScope.launch {
            val context = evaluationContext
            if (context == null) {
                Toast.makeText(this@EvaluationActivity, "Contexto no disponible", Toast.LENGTH_LONG).show()
                showLoading(false)
                btnSaveAll.isEnabled = true
                return@launch
            }

            val savedKeys = mutableListOf<String>()
            val failedKeys = mutableListOf<String>()
            var savedCount = 0
            var errorCount = 0

            for (key in keysToSave) {
                if (!pendingChanges.contains(key)) {
                    Log.d(TAG, "🔁 Key $key ya no está pendiente, se omite.")
                    continue
                }

                val parts = key.split("_")
                if (parts.size < 2) {
                    Log.e(TAG, "Clave inválida: $key")
                    failedKeys.add(key)
                    errorCount++
                    continue
                }

                val studentId = parts[0].toIntOrNull()
                val criterionId = parts[1].toIntOrNull()
                if (studentId == null || criterionId == null) {
                    Log.e(TAG, "Clave con formato incorrecto: $key")
                    failedKeys.add(key)
                    errorCount++
                    continue
                }

                try {
                    val abilityId = context.criteria.find { it.id == criterionId }?.ability_id
                        ?: throw Exception("Ability no encontrada para criterion $criterionId")

                    val request = EvalValueRequest(
                        session_id = sessionId,
                        competency_id = competencyId,
                        ability_id = abilityId,
                        criterion_id = criterionId,
                        product_id = productId,
                        student_id = studentId,
                        value = currentEvaluations[key],
                        observation = currentObservations[key]
                    )
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        RetrofitClient.apiService.upsertEvaluation(request)
                    }

                    savedCount++
                    savedKeys.add(key)
                    Log.d(TAG, "✅ Evaluación guardada: $key el ${getCurrentDateTime()}")

                } catch (e: Exception) {
                    errorCount++
                    failedKeys.add(key)
                    Log.e(TAG, "❌ Error guardando evaluación $key el ${getCurrentDateTime()}", e)
                }
            }

            pendingChanges.removeAll(savedKeys)
            val message = when {
                errorCount == 0 -> "✅ $savedCount evaluaciones guardadas el $currentDate"
                savedCount > 0 -> "⚠️ $savedCount guardadas, $errorCount con errores (se mantienen para reintento)"
                else -> "❌ Error al guardar evaluaciones del $currentDate"
            }

            Toast.makeText(this@EvaluationActivity, message, Toast.LENGTH_LONG).show()
            updateSaveButton()
            showLoading(false)
            btnSaveAll.isEnabled = true
        }
    }

    override fun onCellClicked(p0: RecyclerView.ViewHolder, row: Int, p2: Int) {
        // Manejar clic en celda si es necesario
    }

    override fun onCellLongPressed(p0: RecyclerView.ViewHolder, row: Int, p2: Int) {
        // Manejar long press en celda si es necesario
    }

    override fun onColumnHeaderClicked(p0: RecyclerView.ViewHolder, column: Int) {
        // Manejar clic en header de columna si es necesario
    }

    override fun onColumnHeaderLongPressed(p0: RecyclerView.ViewHolder, column: Int) {
        // Manejar long press en header de columna si es necesario
    }

    override fun onRowHeaderClicked(p0: RecyclerView.ViewHolder, row: Int) {
        // Manejar clic en header de fila si es necesario
    }

    override fun onRowHeaderLongPressed(p0: RecyclerView.ViewHolder, row: Int) {
        // Manejar long press en header de fila si es necesario
    }

    override fun onCellDoubleClicked(cellView: androidx.recyclerview.widget.RecyclerView.ViewHolder, column: Int, row: Int) {
        // Manejar doble clic en celda
        Log.d(TAG, "Cell double clicked: column=$column, row=$row")
    }

    override fun onColumnHeaderDoubleClicked(columnHeaderView: androidx.recyclerview.widget.RecyclerView.ViewHolder, column: Int) {
        // Manejar doble clic en header de columna
        Log.d(TAG, "Column header double clicked: column=$column")
    }

    override fun onRowHeaderDoubleClicked(rowHeaderView: androidx.recyclerview.widget.RecyclerView.ViewHolder, row: Int) {
        // Manejar doble clic en header de fila
        Log.d(TAG, "Row header double clicked: row=$row")
    }

    private fun hasUnsavedChanges(): Boolean = pendingChanges.isNotEmpty()

    private fun showUnsavedChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Cambios sin guardar")
            .setMessage("Tienes ${pendingChanges.size} cambio(s) sin guardar. ¿Qué deseas hacer?")
            .setPositiveButton("Guardar y salir") { _, _ ->
                lifecycleScope.launch {
                    saveAllEvaluations()
                    finish()
                }
            }
            .setNegativeButton("Salir sin guardar") { _, _ -> finish() }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun parseSessionInfo(title: String): String {
        return title.takeIf { it.isNotEmpty() } ?: "II Bimestre - 4° Grado A"
    }

    private fun getStudentName(studentId: Int): String {
        val student = evaluationContext?.students?.find { it.id == studentId }
        return student?.
        full_name ?: "Estudiante"
    }

    private fun getCriterionName(criterionId: Int): String {
        return evaluationContext?.criteria?.find { it.id == criterionId }?.let {
            it.display_name ?: it.name
        } ?: "Criterio"
    }

    private fun formatStudentName(fullName: String): String {
        // Buscar la coma para dividir apellidos y nombres
        val commaIndex = fullName.indexOf(',')

        return if (commaIndex != -1 && commaIndex < fullName.length - 1) {
            val lastName = fullName.substring(0, commaIndex + 1) // Incluye la coma
            val firstName = fullName.substring(commaIndex + 1).trim()

            // ✅ Formato: "Apellidos,\nNombres"
            "$lastName\n$firstName"
        } else {
            // Si no hay coma, mantener formato original
            fullName
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog()
        } else {
            super.onBackPressed()
        }
    }
}