package com.evalua.classter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.evalua.classter.adapters.ReportStudentAdapter
import com.evalua.classter.models.*
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import org.json.JSONObject

class ReportsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ReportsActivity"
    }
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvSectionTitle: TextView
    private lateinit var tvCompetencyName: TextView
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvStudents: RecyclerView
    private lateinit var studentAdapter: ReportStudentAdapter

    private lateinit var btnGenerateQR: MaterialButton
    private lateinit var qrBottomSheet: BottomSheetDialog
    private lateinit var ivQRCode: ImageView
    private lateinit var btnShareQR: MaterialButton

    private var sectionId: Int = -1
    private var competencyId: Int = -1
    private var sectionName: String = ""
    private var consolidatedData: ConsolidatedResponse? = null
    private val editedAverages = mutableMapOf<String, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        initViews()
        getIntentData()
        setupToolbar()
        setupButtons()
        setupSwipeRefresh()
        loadReportsData()
        setupQRViews()
        setupQRButton()
    }

    private fun setupQRViews() {
        btnGenerateQR = findViewById(R.id.btnGenerateQR)

        // Configurar Bottom Sheet
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_qr, null)
        val btnPreview = bottomSheetView.findViewById<MaterialButton>(R.id.btnPreview)
        btnPreview.setOnClickListener {
            handleQRClick()
        }
        ivQRCode = bottomSheetView.findViewById(R.id.ivQRCode)
        btnShareQR = bottomSheetView.findViewById(R.id.btnShare)

        qrBottomSheet = BottomSheetDialog(this).apply {
            setContentView(bottomSheetView)
        }

        // Configurar compartir
        btnShareQR.setOnClickListener {
            shareQRCode()
        }
    }

    private fun setupQRButton() {
        btnGenerateQR.setOnClickListener {
            lifecycleScope.launch {
                try {
                    showLoading(true)

                    // 1. Primero generamos y guardamos el HTML
                    val fileName = "consolidado_${sectionName.replace(" ", "_")}_${System.currentTimeMillis()}.html"
                    val success = generatePdfReport(fileName)

                    if (!success) {
                        Toast.makeText(this@ReportsActivity,
                            "Error generando el reporte",
                            Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // 2. Obtenemos la URI del archivo local
                    val file = File(getExternalFilesDir(null), fileName)
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        this@ReportsActivity,
                        "${packageName}.provider",
                        file
                    )

                    // 3. Creamos los datos del QR con la URI local
                    val qrData = QRData(
                        sectionId = sectionId,
                        fileName = fileName,
                        fileUri = fileUri.toString(),
                        timestamp = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (3600 * 1000),
                        sectionName = sectionName
                    )

                    // 4. Generamos el QR
                    val qrBitmap = generateQRCode(qrData.toJson())
                    ivQRCode.setImageBitmap(qrBitmap)

                    // Guardamos la data para poder acceder al archivo después
                    ivQRCode.tag = qrData.toJson()

                    // 5. Mostramos el Bottom Sheet con instrucciones actualizadas
                    qrBottomSheet.show()

                    // 6. Mostrar instrucciones al usuario
                    Toast.makeText(this@ReportsActivity,
                        "✅ QR generado.\nAl escanear se abrirá el reporte HTML guardado en tu dispositivo",
                        Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    Log.e(TAG, "Error generando QR", e)
                    Toast.makeText(this@ReportsActivity,
                        "Error generando QR: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun generateQRCode(content: String): Bitmap {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                512,
                512
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generando QR", e)
            throw e
        }
    }

    private fun shareQRCode() {
        // Guardar QR temporalmente
        val qrBitmap = (ivQRCode.drawable as BitmapDrawable).bitmap
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            qrBitmap,
            "Consolidado_${sectionName}_QR",
            "Código QR para consolidado de $sectionName"
        )

        val uri = path.toUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Consolidado de $sectionName")
            putExtra(Intent.EXTRA_TEXT, "Escanea este código QR para ver el consolidado de notas")
        }

        startActivity(Intent.createChooser(intent, "Compartir código QR"))
    }

    data class QRData(
        val sectionId: Int,
        val fileName: String,
        val fileUri: String,
        val timestamp: Long,
        val expiresAt: Long,
        val sectionName: String
    ) {
        fun toJson(): String = JSONObject().apply {
            put("sectionId", sectionId)
            put("fileName", fileName)
            put("fileUri", fileUri)
            put("timestamp", timestamp)
            put("expiresAt", expiresAt)
            put("sectionName", sectionName)
            // Quitamos la URL base y usamos directamente el fileUri local
        }.toString()
    }

    private fun handleQRClick() {
        val currentQRData = try {
            val jsonStr = (ivQRCode.tag as? String) ?: return
            // Parsear el JSON a QRData
            JSONObject(jsonStr).let { json ->
                QRData(
                    sectionId = json.getInt("sectionId"),
                    fileName = json.getString("fileName"),
                    fileUri = json.getString("fileUri"),
                    timestamp = json.getLong("timestamp"),
                    expiresAt = json.getLong("expiresAt"),
                    sectionName = json.getString("sectionName")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing QR data", e)
            return
        }

        // Abrir el archivo HTML
        try {
            val file = File(getExternalFilesDir(null), currentQRData.fileName)
            if (!file.exists()) {
                Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Abrir reporte"))
        } catch (e: Exception) {
            Log.e(TAG, "Error opening HTML file", e)
            Toast.makeText(this, "Error abriendo el archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvSectionTitle = findViewById(R.id.tvSectionTitle)
        tvCompetencyName = findViewById(R.id.tvCompetencyName)
        btnExportExcel = findViewById(R.id.btnExportExcel)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefreshLayout)
        rvStudents = findViewById(R.id.rvStudents)

        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.visibility = View.VISIBLE
    }

    private fun getIntentData() {
        sectionId = intent.getIntExtra("SECTION_ID", -1)
        competencyId = intent.getIntExtra("COMPETENCY_ID", -1)
        sectionName = intent.getStringExtra("SECTION_NAME") ?: "Sección"

        Log.d(TAG, "Intent data: SECTION_ID=$sectionId, COMPETENCY_ID=$competencyId, SECTION_NAME=$sectionName")

        if (sectionId == -1) {
            Toast.makeText(this, "Error: ID de sección no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Consolidado - $sectionName"
        }
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupButtons() {
        btnExportExcel.setOnClickListener {
            generatePdfReport()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadReportsData()
        }
    }

    private fun loadReportsData() {
        Log.d(TAG, "🚀 Cargando consolidado de sección: $sectionId")
        showLoading(true)

        lifecycleScope.launch {
            try {
                val consolidatedResponse = RetrofitClient.apiService.getConsolidatedReport(sectionId)
                consolidatedData = consolidatedResponse
                displayConsolidatedTable(consolidatedResponse)

                Log.d(TAG, "✅ Consolidado cargado:")
                Log.d(TAG, "  Students: ${consolidatedResponse.students.size}")
                Log.d(TAG, "  Sessions: ${consolidatedResponse.sessions.size}")
                Log.d(TAG, "  Competencies: ${consolidatedResponse.competencies.size}")
                Log.d(TAG, "  Abilities: ${consolidatedResponse.abilities.size}")
                Log.d(TAG, "  Criteria: ${consolidatedResponse.criteria.size}")
                Log.d(TAG, "  Values: ${consolidatedResponse.values.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando consolidado", e)
                Toast.makeText(this@ReportsActivity,
                    "Error de red. Cargando datos de prueba...", Toast.LENGTH_SHORT).show()
                loadTestConsolidatedData()
            } finally {
                showLoading(false)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun displayConsolidatedTable(data: ConsolidatedResponse) {
        tvSectionTitle.text = "Consolidado - $sectionName"

        // ✅ NUEVO: Actualizar badges dinámicamente
        updateHeaderBadges(data)

        // Texto de competencias
        tvCompetencyName.text = if (data.competencies.size == 1)
            data.competencies.first().display_name
        else
            "${data.competencies.size} Competencias | ${data.sessions.size} Sesiones"

        val abilitiesByCompetency = data.abilities.groupBy { it.competency_id }
        val criteriaByAbility = data.criteria.groupBy { it.ability_id }

        studentAdapter = ReportStudentAdapter(
            students = data.students,
            sessions = data.sessions,
            competencies = data.competencies,
            abilitiesByCompetency = abilitiesByCompetency,
            criteriaByAbility = criteriaByAbility,
            values = data.values,
            observations = data.observations,
            onEditAverage = { studentId, abilityId, value ->
                handleAverageChange(studentId, abilityId, value)
            },
            onEditFinalAverage = { studentId, value ->
                handleFinalAverageChange(studentId, value)
            },
            onObservationClick = { studentId, abilityId, criterionId ->
                showObservationDialog(studentId, abilityId, criterionId)
            }
        )
        rvStudents.adapter = studentAdapter

        Toast.makeText(this,
            "✅ ${data.students.size} estudiantes | ${data.sessions.size} sesiones",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ✅ NUEVA FUNCIÓN: Actualizar badges del header
    private fun updateHeaderBadges(data: ConsolidatedResponse) {
        val tvStudentsBadge = findViewById<TextView>(R.id.tvStudentsBadge)
        val tvSessionsBadge = findViewById<TextView>(R.id.tvSessionsBadge)

        tvStudentsBadge?.text = "👥 ${data.students.size} ${if (data.students.size == 1) "Estudiante" else "Estudiantes"}"
        tvSessionsBadge?.text = "📚 ${data.sessions.size} ${if (data.sessions.size == 1) "Sesión" else "Sesiones"}"
    }

    private fun handleAverageChange(studentId: Int, abilityId: Int?, grade: String?) {
        val key = "${studentId}_${abilityId ?: "final"}"
        editedAverages[key] = grade
        Log.d(TAG, "📝 Promedio editado: Student $studentId, Ability $abilityId = $grade")
    }

    private fun handleFinalAverageChange(studentId: Int, grade: String?) {
        val key = "${studentId}_final"
        editedAverages[key] = grade
        Log.d(TAG, "📝 Promedio final editado: Student $studentId = $grade")
    }

    private fun showObservationDialog(studentId: Int, abilityId: Int, criterionId: Int) {
        val studentName = getStudentName(studentId)
        val abilityName = getAbilityName(abilityId)
        val currentObservation = consolidatedData?.observations?.find {
            it.student_id == studentId && it.ability_id == abilityId
        }?.observation ?: ""

        val editText = android.widget.EditText(this).apply {
            setText(currentObservation)
            hint = "Observación para $abilityName..."
            setPadding(48, 48, 48, 16)
            minLines = 3
            maxLines = 8
            textSize = 14f
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📝 Observación")
            .setMessage("Estudiante: $studentName\nCapacidad: $abilityName")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val observation = editText.text.toString().trim()
                saveObservation(studentId, abilityId, observation.ifEmpty { null })
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpiar") { _, _ ->
                saveObservation(studentId, abilityId, null)
            }
            .show()

        editText.requestFocus()
        editText.selectAll()
    }

    private fun saveObservation(studentId: Int, abilityId: Int, observation: String?) {
        Log.d(TAG, "💾 Guardando observación: Student $studentId, Ability $abilityId")

        val message = if (observation.isNullOrEmpty()) {
            "🗑️ Observación eliminada"
        } else {
            "💾 Observación guardada"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        updateLocalObservation(studentId, abilityId, observation)
        studentAdapter.notifyDataSetChanged()
    }

    private fun updateLocalObservation(studentId: Int, abilityId: Int, observation: String?) {
        consolidatedData?.let { data ->
            val mutableObs = data.observations.toMutableList()
            val existingIndex = mutableObs.indexOfFirst {
                it.student_id == studentId && it.ability_id == abilityId
            }

            if (observation.isNullOrEmpty()) {
                if (existingIndex != -1) {
                    mutableObs.removeAt(existingIndex)
                }
            } else {
                if (existingIndex != -1) {
                    mutableObs[existingIndex] = ConsolidatedObservation(studentId, abilityId, observation)
                } else {
                    mutableObs.add(ConsolidatedObservation(studentId, abilityId, observation))
                }
            }

            consolidatedData = data.copy(observations = mutableObs)
        }
    }

    private fun generatePdfReport() {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val fileName = "consolidado_${sectionName.replace(" ", "_")}_$currentDate.html"

        Toast.makeText(this, "🎨 Generando reporte profesional...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val success = generatePdfReport(fileName)

                if (success) {
                    Toast.makeText(this@ReportsActivity,
                        "✅ Reporte HTML generado\n📱 Fácil de leer y compartir",
                        Toast.LENGTH_LONG).show()

                    // Compartir el archivo
                    shareHtmlFile(fileName)
                } else {
                    Toast.makeText(this@ReportsActivity,
                        "❌ Error al generar reporte",
                        Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en exportación", e)
                Toast.makeText(this@ReportsActivity,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun generatePdfReport(fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val consolidatedData = consolidatedData ?: return@withContext false

                // Crear contenido HTML bonito
                val htmlContent = buildBeautifulHtmlReport(consolidatedData)

                val downloadsDir = getExternalFilesDir(null) ?: filesDir
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { fos ->
                    fos.write(htmlContent.toByteArray(Charsets.UTF_8))
                }

                Log.d(TAG, "✅ Reporte HTML exportado: ${file.absolutePath}")
                true

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error generando reporte", e)
                false
            }
        }
    }

    private fun shareHtmlFile(fileName: String) {
        val file = File(getExternalFilesDir(null), fileName)

        try {
            if (!file.exists()) {
                Toast.makeText(this, "❌ Archivo no encontrado", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Consolidado $sectionName")
                putExtra(Intent.EXTRA_TEXT, "Consolidado de evaluaciones - $sectionName\n\n📝 Para ver correctamente:\n• Ábrelo con cualquier navegador\n• O guárdalo y ábrelo con Word/Excel")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Compartir Reporte"))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error compartiendo archivo", e)
            Toast.makeText(this,
                "✅ Archivo guardado en: ${file.absolutePath}\n\n📝 Para abrir:\n• Cópialo a tu PC\n• Ábrelo con cualquier navegador\n• Se verá profesional y organizado",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildBeautifulHtmlReport(data: ConsolidatedResponse): String {
        val builder = StringBuilder()

        builder.append("""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Consolidado $sectionName</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Arial, sans-serif; 
            line-height: 1.6; 
            color: #333; 
            background: #f8f9fa;
            padding: 20px;
        }
        .container { 
            max-width: 1200px; 
            margin: 0 auto; 
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .header { 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 { 
            font-size: 28px; 
            margin-bottom: 10px;
            font-weight: 600;
        }
        .header .subtitle { 
            font-size: 16px; 
            opacity: 0.9;
        }
        .summary-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            padding: 20px;
            background: #f8f9fa;
        }
        .card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .card .number { 
            font-size: 24px; 
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
        }
        .card .label { 
            font-size: 14px; 
            color: #666;
        }
        .student-section {
            padding: 20px;
        }
        .student-card {
            background: white;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            margin-bottom: 15px;
            overflow: hidden;
        }
        .student-header {
            background: #f8f9fa;
            padding: 15px 20px;
            border-bottom: 1px solid #e9ecef;
            display: flex;
            justify-content: space-between;  // ← CORREGIDO
            align-items: center;
        }
        .student-name {
            font-size: 18px;
            font-weight: 600;
            color: #495057;
        }
        .student-number {
            background: #667eea;
            color: white;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 14px;
        }
        .student-content {
            padding: 20px;
        }
        .session-group {
            margin-bottom: 20px;
        }
        .session-title {
            font-size: 16px;
            font-weight: 600;
            color: #495057;
            margin-bottom: 10px;
            padding-bottom: 5px;
            border-bottom: 2px solid #667eea;
        }
        .competency-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 15px;
        }
        .competency-card {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 6px;
            border-left: 4px solid #667eea;
        }
        .competency-name {
            font-weight: 600;
            margin-bottom: 10px;
            color: #495057;
        }
        .ability-item {
            margin-bottom: 8px;
            padding-left: 10px;
        }
        .ability-name {
            font-weight: 500;
            margin-bottom: 5px;
        }
        .criteria-list {
            list-style: none;
        }
        .criterion-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 5px 0;
            border-bottom: 1px solid #e9ecef;
        }
        .criterion-name {
            flex: 1;
        }
        .grade {
            padding: 2px 8px;
            border-radius: 4px;
            font-weight: 600;
            font-size: 12px;
        }
        .grade-ad { background: #d4edda; color: #155724; }
        .grade-a { background: #d1ecf1; color: #0c5460; }
        .grade-b { background: #fff3cd; color: #856404; }
        .grade-c { background: #f8d7da; color: #721c24; }
        .final-average {
            background: #495057;
            color: white;
            padding: 8px 15px;
            border-radius: 6px;
            font-weight: 600;
            margin-top: 10px;
            text-align: center;
        }
        .observations {
            background: #e7f3ff;
            padding: 15px;
            border-radius: 6px;
            margin-top: 15px;
            border-left: 4px solid #007bff;
        }
        .observations-title {
            font-weight: 600;
            margin-bottom: 8px;
            color: #0056b3;
        }
        .observation-item {
            margin-bottom: 5px;
            padding-left: 10px;
        }
        .legend {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 6px;
            margin-top: 20px;
        }
        .legend-title {
            font-weight: 600;
            margin-bottom: 10px;
        }
        .legend-items {
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .legend-color {
            width: 16px;
            height: 16px;
            border-radius: 3px;
        }
        .footer {
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            color: #666;
            font-size: 12px;
            border-top: 1px solid #e9ecef;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 Consolidado de Evaluaciones</h1>
            <div class="subtitle">$sectionName</div>
        </div>
        
        <div class="summary-cards">
            <div class="card">
                <div class="number">${data.students.size}</div>
                <div class="label">Estudiantes</div>
            </div>
            <div class="card">
                <div class="number">${data.sessions.size}</div>
                <div class="label">Sesiones</div>
            </div>
            <div class="card">
                <div class="number">${data.competencies.size}</div>
                <div class="label">Competencias</div>
            </div>
            <div class="card">
                <div class="number">${data.criteria.size}</div>
                <div class="label">Criterios</div>
            </div>
        </div>
        
        <div class="student-section">
    """)

        // Estudiantes
        data.students.forEachIndexed { index, student ->
            builder.append("""
        <div class="student-card">
            <div class="student-header">
                <div class="student-name">${student.full_name}</div>
                <div class="student-number">Estudiante ${index + 1}</div>
            </div>
            <div class="student-content">
        """)

            // Sesiones - CORRECCIÓN: Filtrar por session.id
            data.sessions.sortedBy { it.number }.forEach { session ->
                builder.append("""
            <div class="session-group">
                <div class="session-title">${session.title ?: "Sesión ${session.number}"}</div>
                <div class="competency-grid">
            """)

                // ✅ CORRECCIÓN: Solo mostrar competencias de ESTA sesión
                val competenciesInSession = data.competencies.filter { it.session_id == session.id }

                if (competenciesInSession.isEmpty()) {
                    builder.append("""
                <div class="competency-card">
                    <div class="competency-name">No hay competencias evaluadas</div>
                </div>
                """)
                } else {
                    competenciesInSession.forEach { competency ->
                        builder.append("""
                    <div class="competency-card">
                        <div class="competency-name">${competency.display_name}</div>
                    """)

                        val abilities = data.abilities.filter { it.competency_id == competency.id }
                        abilities.forEach { ability ->
                            builder.append("""
                        <div class="ability-item">
                            <div class="ability-name">${ability.display_name}</div>
                            <ul class="criteria-list">
                        """)

                            val criteria = data.criteria.filter { it.ability_id == ability.id }
                            criteria.forEach { criterion ->
                                val value = data.values.find {
                                    it.student_id == student.id && it.criterion_id == criterion.id
                                }?.value ?: "-"

                                val gradeClass = when (value) {
                                    "AD" -> "grade-ad"
                                    "A" -> "grade-a"
                                    "B" -> "grade-b"
                                    "C" -> "grade-c"
                                    else -> ""
                                }

                                builder.append("""
                            <li class="criterion-item">
                                <span class="criterion-name">${criterion.display_name}</span>
                                <span class="grade $gradeClass">$value</span>
                            </li>
                            """)
                            }
                            builder.append("</ul></div>")
                        }
                        builder.append("</div>")
                    }
                }
                builder.append("</div></div>")
            }

            // Promedio final
            val finalAverage = calculateStudentFinalAverage(student.id) ?: "-"
            val finalGradeClass = when (finalAverage) {
                "AD" -> "grade-ad"
                "A" -> "grade-a"
                "B" -> "grade-b"
                "C" -> "grade-c"
                else -> ""
            }

            builder.append("""
            <div class="final-average $finalGradeClass">
                Promedio Final: $finalAverage
            </div>
        """)

            // Observaciones
            val studentObservations = data.observations.filter { it.student_id == student.id }
            if (studentObservations.isNotEmpty()) {
                builder.append("""
                <div class="observations">
                    <div class="observations-title">📝 Observaciones</div>
            """)

                studentObservations.forEach { obs ->
                    val abilityName = data.abilities.find { it.id == obs.ability_id }?.display_name ?: ""
                    builder.append("""
                    <div class="observation-item">
                        <strong>$abilityName:</strong> ${obs.observation}
                    </div>
                """)
                }

                builder.append("</div>")
            }

            builder.append("</div></div>")
        }

        // Leyenda
        builder.append("""
        <div class="legend">
            <div class="legend-title">Leyenda de Calificaciones</div>
            <div class="legend-items">
                <div class="legend-item">
                    <div class="legend-color" style="background-color: #d4edda;"></div>
                    <span>AD - Logro destacado</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background-color: #d1ecf1;"></div>
                    <span>A - Logro esperado</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background-color: #fff3cd;"></div>
                    <span>B - En proceso</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background-color: #f8d7da;"></div>
                    <span>C - En inicio</span>
                </div>
            </div>
        </div>
        
        <div class="footer">
            Generado el ${java.text.SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
        </div>
    </div>
</body>
</html>
    """)

        return builder.toString()
    }

    private fun calculateStudentFinalAverage(studentId: Int): String? {
        val consolidatedData = consolidatedData ?: return null

        val allGrades = mutableListOf<String>()

        // Recoger todas las calificaciones del estudiante
        consolidatedData.values
            .filter { it.student_id == studentId }
            .forEach { value ->
                if (value.value in listOf("AD", "A", "B", "C")) {
                    allGrades.add(value.value)
                }
            }

        if (allGrades.isEmpty()) return null

        val gradeValues = mapOf("AD" to 4, "A" to 3, "B" to 2, "C" to 1)
        val sum = allGrades.mapNotNull { gradeValues[it] }.sum()
        if (sum == 0) return null

        val average = sum.toDouble() / allGrades.size
        return when {
            average >= 3.5 -> "AD"
            average >= 2.5 -> "A"
            average >= 1.5 -> "B"
            else -> "C"
        }
    }

    private fun loadTestConsolidatedData() {
        Log.d(TAG, "🧪 Cargando datos de prueba con sesiones...")

        try {
            val testData = ConsolidatedResponse(
                students = listOf(
                    ConsolidatedStudent(1, "GARCÍA LÓPEZ, ANA MARÍA"),
                    ConsolidatedStudent(2, "MARTÍNEZ PÉREZ, CARLOS EDUARDO"),
                    ConsolidatedStudent(3, "RODRÍGUEZ SILVA, MARÍA FERNANDA"),
                    ConsolidatedStudent(4, "LÓPEZ TORRES, JUAN PABLO"),
                    ConsolidatedStudent(5, "GONZÁLEZ RAMÍREZ, SOFÍA VALENTINA")
                ),
                sessions = listOf(
                    ConsolidatedSession(1, "Sesión 1 - Identificación del problema", 1),
                    ConsolidatedSession(2, "Sesión 2 - Diseño de prototipo", 2),
                    ConsolidatedSession(3, "Sesión 3 - Evaluación final", 3)
                ),
                competencies = listOf(
                    ConsolidatedCompetency(1, 1, "Diseña y construye soluciones tecnológicas")
                ),
                abilities = listOf(
                    ConsolidatedAbility(1, 1, "Determina una alternativa de solución"),
                    ConsolidatedAbility(2, 1, "Diseña la solución tecnológica"),
                    ConsolidatedAbility(3, 1, "Implementa y valida"),
                    ConsolidatedAbility(4, 1, "Evalúa y comunica")
                ),
                criteria = listOf(
                    ConsolidatedCriterion(1, 1, "Propone alternativa tecnológica"),
                    ConsolidatedCriterion(2, 1, "Identifica el problema claramente"),
                    ConsolidatedCriterion(3, 2, "Representa en dibujo técnico"),
                    ConsolidatedCriterion(4, 2, "Selecciona materiales adecuados"),
                    ConsolidatedCriterion(5, 3, "Construye el prototipo"),
                    ConsolidatedCriterion(6, 3, "Realiza pruebas y ajustes"),
                    ConsolidatedCriterion(7, 4, "Fluidez en la exposición"),
                    ConsolidatedCriterion(8, 4, "Pertinencia del prototipo")
                ),
                values = listOf(
                    // Estudiante 1 - Ana María
                    ConsolidatedValue(1, 1, "A"),
                    ConsolidatedValue(1, 2, "A"),
                    ConsolidatedValue(1, 3, "AD"),
                    ConsolidatedValue(1, 4, "A"),
                    ConsolidatedValue(1, 5, "AD"),
                    ConsolidatedValue(1, 6, "A"),
                    ConsolidatedValue(1, 7, "AD"),
                    ConsolidatedValue(1, 8, "AD"),

                    // Estudiante 2 - Carlos Eduardo
                    ConsolidatedValue(2, 1, "B"),
                    ConsolidatedValue(2, 2, "A"),
                    ConsolidatedValue(2, 3, "A"),
                    ConsolidatedValue(2, 4, "B"),
                    ConsolidatedValue(2, 5, "A"),
                    ConsolidatedValue(2, 7, "B"),
                    ConsolidatedValue(2, 8, "A"),

                    // Estudiante 3 - María Fernanda
                    ConsolidatedValue(3, 1, "AD"),
                    ConsolidatedValue(3, 2, "AD"),
                    ConsolidatedValue(3, 3, "AD"),
                    ConsolidatedValue(3, 4, "A"),
                    ConsolidatedValue(3, 5, "AD"),
                    ConsolidatedValue(3, 6, "AD"),
                    ConsolidatedValue(3, 7, "AD"),
                    ConsolidatedValue(3, 8, "AD"),

                    // Estudiante 4 - Juan Pablo
                    ConsolidatedValue(4, 1, "C"),
                    ConsolidatedValue(4, 2, "B"),
                    ConsolidatedValue(4, 3, "B"),
                    ConsolidatedValue(4, 5, "B"),
                    ConsolidatedValue(4, 6, "C"),
                    ConsolidatedValue(4, 7, "C"),
                    ConsolidatedValue(4, 8, "B"),

                    // Estudiante 5 - Sofía Valentina
                    ConsolidatedValue(5, 1, "A"),
                    ConsolidatedValue(5, 2, "A"),
                    ConsolidatedValue(5, 3, "A"),
                    ConsolidatedValue(5, 4, "A"),
                    ConsolidatedValue(5, 5, "A"),
                    ConsolidatedValue(5, 6, "A"),
                    ConsolidatedValue(5, 7, "B"),
                    ConsolidatedValue(5, 8, "A")
                ),
                observations = listOf(
                    ConsolidatedObservation(1, 4, "Excelente dominio del tema y presentación muy clara"),
                    ConsolidatedObservation(2, 1, "Necesita reforzar la fundamentación de propuestas"),
                    ConsolidatedObservation(4, 1, "Requiere mayor dedicación y compromiso con las tareas")
                )
            )
            consolidatedData = testData
            displayConsolidatedTable(testData)
            Toast.makeText(this, "📊 Datos de prueba cargados (3 sesiones)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando datos de prueba", e)
            Toast.makeText(this, "❌ Error crítico", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnExportExcel.isEnabled = !show
    }

    private fun getStudentName(studentId: Int): String {
        return consolidatedData?.students?.find { it.id == studentId }?.full_name ?: "Estudiante"
    }
    private fun getAbilityName(abilityId: Int): String {
        return consolidatedData?.abilities?.find { it.id == abilityId }?.display_name ?: "Capacidad"
    }

    private fun getCriterionName(criterionId: Int): String {
        return consolidatedData?.criteria?.find { it.id == criterionId }?.display_name ?: "Criterio"
    }
}