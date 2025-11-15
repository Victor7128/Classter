package com.evalua.classter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.adapters.StudentAdapter
import com.evalua.classter.dialogs.AddStudentDialogFragment
import com.evalua.classter.dialogs.EditStudentDialogFragment
import com.evalua.classter.dialogs.ImportInstructionsDialogFragment
import com.evalua.classter.dialogs.CsvImportInstructionsDialogFragment
import com.evalua.classter.dialogs.TxtImportInstructionsDialogFragment
import com.evalua.classter.models.Student
import com.evalua.classter.network.BatchStudentsRequest
import com.evalua.classter.network.CreateStudentRequest
import com.evalua.classter.network.ImportStudentsResponse
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class StudentsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "StudentsActivity"
    }
    private lateinit var rvStudents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvSectionInfo: TextView
    private lateinit var fabAddStudent: FloatingActionButton
    private lateinit var adapter: StudentAdapter
    private val studentsList = mutableListOf<Student>()
    private var sectionId: Int = -1
    private var sectionLetter: String = ""
    private var gradeNumber: Int = -1
    private var bimesterName: String = ""

    // Launcher para CSV
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileImport(uri, isCsv = true)
            }
        }
    }

    // Launcher para TXT
    private val txtPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileImport(uri, isCsv = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_students)

        // Obtener datos del intent
        sectionId = intent.getIntExtra("section_id", -1)
        sectionLetter = intent.getStringExtra("section_letter") ?: ""
        gradeNumber = intent.getIntExtra("grade_number", -1)
        bimesterName = intent.getStringExtra("bimester_name") ?: ""

        Log.d(TAG, "Received: sectionId=$sectionId, letter=$sectionLetter, grade=$gradeNumber, bimester=$bimesterName")

        if (sectionId == -1) {
            Toast.makeText(this, "Error: ID de sección inválido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI()
        checkPermissions()
        loadStudents()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Permission granted")
        } else {
            Toast.makeText(this, "Se necesitan permisos para leer archivos", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android 13+: No se requiere permiso específico para archivos seleccionados")
        } else
            when {
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Permission already granted")
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    Toast.makeText(
                        this,
                        "Se necesita permiso para leer archivos",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
    }

    private fun setupUI() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Estudiantes"

        tvSectionInfo = findViewById(R.id.tvSectionInfo)
        tvSectionInfo.text = "$bimesterName Bimestre - $gradeNumber° - Sección $sectionLetter"

        rvStudents = findViewById(R.id.recyclerViewStudents)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        fabAddStudent = findViewById(R.id.fabAddStudent)

        rvStudents.layoutManager = LinearLayoutManager(this)

        adapter = StudentAdapter(
            students = studentsList,
            onEditStudent = { student ->
                showEditStudentDialog(student)
            },
            onDeleteStudent = { student ->
                confirmDeleteStudent(student)
            }
        )
        rvStudents.adapter = adapter

        fabAddStudent.setOnClickListener {
            showAddStudentDialog()
        }

        // ✅ NUEVO: Botón de agregar en empty state
        val btnEmptyAddStudent = findViewById<View>(R.id.btnEmptyAddStudent)
        btnEmptyAddStudent?.setOnClickListener {
            showAddStudentDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_students, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_import_manual -> {
                showBatchImportDialog()
                true
            }
            R.id.action_import_txt -> {
                openTxtPicker()
                true
            }
            R.id.action_import_csv -> {
                openCsvPicker()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ✅ ACTUALIZADO: Usar getStudents() con corrutinas
    private fun loadStudents() {
        // ✅ Mostrar loading
        val layoutLoading = findViewById<View>(R.id.layoutLoading)
        if (layoutLoading != null) {
            layoutLoading.visibility = View.VISIBLE
        } else {
            progressBar.visibility = ProgressBar.VISIBLE
        }

        // Ocultar empty state
        val layoutEmpty = findViewById<View>(R.id.layoutEmptyState)
        layoutEmpty?.visibility = View.GONE
        tvEmptyState.visibility = TextView.GONE

        lifecycleScope.launch {
            try {
                val students = RetrofitClient.apiService.getStudents(sectionId)
                updateStudentsList(students)

                // ✅ Ocultar loading
                if (layoutLoading != null) {
                    layoutLoading.visibility = View.GONE
                } else {
                    progressBar.visibility = ProgressBar.GONE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading students", e)

                // ✅ Ocultar loading
                if (layoutLoading != null) {
                    layoutLoading.visibility = View.GONE
                } else {
                    progressBar.visibility = ProgressBar.GONE
                }

                Toast.makeText(
                    this@StudentsActivity,
                    "Error al cargar estudiantes: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateStudentsList(students: List<Student>) {
        studentsList.clear()
        studentsList.addAll(students)
        adapter.notifyDataSetChanged()

        // ✅ CORREGIDO: Actualizar contador
        val tvStudentCount = findViewById<TextView>(R.id.tvStudentCount)
        tvStudentCount?.text = "${students.size} ${if (students.size == 1) "estudiante" else "estudiantes"}"

        // ✅ CORREGIDO: Manejar estados visuales
        if (students.isEmpty()) {
            // Ocultar RecyclerView
            rvStudents.visibility = RecyclerView.GONE

            // Mostrar empty state (usa el ID correcto del nuevo layout)
            val layoutEmpty = findViewById<View>(R.id.layoutEmptyState)
            if (layoutEmpty != null) {
                layoutEmpty.visibility = View.VISIBLE
            } else {
                // Si no existe layoutEmptyState, usar el TextView antiguo
                tvEmptyState.visibility = TextView.VISIBLE
            }
        } else {
            // Mostrar RecyclerView
            rvStudents.visibility = RecyclerView.VISIBLE

            // Ocultar empty state
            val layoutEmpty = findViewById<View>(R.id.layoutEmptyState)
            if (layoutEmpty != null) {
                layoutEmpty.visibility = View.GONE
            } else {
                tvEmptyState.visibility = TextView.GONE
            }
        }
    }

    private fun showAddStudentDialog() {
        // ✅ Pasar la lista de estudiantes existentes
        val dialog = AddStudentDialogFragment(
            existingStudents = studentsList,  // ✅ NUEVO
            onStudentAdded = { fullName ->
                createStudent(fullName)
            }
        )
        dialog.show(supportFragmentManager, "AddStudentDialog")
    }

    private fun showEditStudentDialog(student: Student) {
        val dialog = EditStudentDialogFragment(student) { updatedName ->
            updateStudent(student.id, updatedName)
        }
        dialog.show(supportFragmentManager, "EditStudentDialog")
    }

    // ✅ MANTENER: Estos usan Call<> porque son operaciones de modificación
    private fun createStudent(fullName: String) {
        // ✅ Validación de seguridad antes de enviar
        val isDuplicate = studentsList.any {
            it.full_name.equals(fullName, ignoreCase = true)
        }

        if (isDuplicate) {
            Toast.makeText(
                this,
                "⚠️ El estudiante \"$fullName\" ya existe en la lista",
                Toast.LENGTH_LONG
            ).show()
            return  // ✅ No enviar al backend
        }

        progressBar.visibility = ProgressBar.VISIBLE

        val request = CreateStudentRequest(full_name = fullName)
        RetrofitClient.instance.createStudent(sectionId, request)
            .enqueue(object : Callback<Student> {
                override fun onResponse(call: Call<Student>, response: Response<Student>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        val newStudent = response.body()
                        if (newStudent != null) {
                            adapter.addStudent(newStudent)

                            if (studentsList.isNotEmpty()) {
                                tvEmptyState.visibility = TextView.GONE
                                rvStudents.visibility = RecyclerView.VISIBLE
                            }

                            Toast.makeText(
                                this@StudentsActivity,
                                "✅ Estudiante agregado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // ✅ Mejorar manejo de errores del backend
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = when {
                            errorBody?.contains("duplicate", ignoreCase = true) == true ||
                                    errorBody?.contains("already exists", ignoreCase = true) == true ->
                                "⚠️ El estudiante ya existe en el servidor"
                            else ->
                                "❌ Error al agregar estudiante"
                        }

                        Toast.makeText(
                            this@StudentsActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Student>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@StudentsActivity,
                        "❌ Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateStudent(studentId: Int, fullName: String) {
        progressBar.visibility = ProgressBar.VISIBLE

        val request = CreateStudentRequest(full_name = fullName)
        RetrofitClient.instance.updateStudent(studentId, request)
            .enqueue(object : Callback<Student> {
                override fun onResponse(call: Call<Student>, response: Response<Student>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        val updatedStudent = response.body()
                        if (updatedStudent != null) {
                            adapter.updateStudent(updatedStudent)

                            Toast.makeText(
                                this@StudentsActivity,
                                "Estudiante actualizado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@StudentsActivity,
                            "Error al actualizar estudiante",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Student>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@StudentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun confirmDeleteStudent(student: Student) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Está seguro que desea eliminar a ${student.full_name}?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteStudent(student)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteStudent(student: Student) {
        progressBar.visibility = ProgressBar.VISIBLE

        RetrofitClient.instance.deleteStudent(student.id)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        adapter.removeStudent(student)

                        if (studentsList.isEmpty()) {
                            tvEmptyState.visibility = TextView.VISIBLE
                            rvStudents.visibility = RecyclerView.GONE
                        }

                        Toast.makeText(
                            this@StudentsActivity,
                            "${student.full_name} eliminado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@StudentsActivity,
                            "Error al eliminar estudiante",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@StudentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ============ IMPORTACIÓN MANUAL ============
    private fun showBatchImportDialog() {
        val instructionsDialog = ImportInstructionsDialogFragment {
            showTextInputDialog()
        }
        instructionsDialog.show(supportFragmentManager, "ImportInstructions")
    }

    private fun showTextInputDialog() {
        val dialog = AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Ingrese nombres separados por saltos de línea"
        input.minLines = 5
        input.maxLines = 10

        dialog.setTitle("Importar estudiantes")
            .setView(input)
            .setPositiveButton("Importar") { _, _ ->
                val text = input.text.toString()
                val names = text.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (names.isNotEmpty()) {
                    importStudentsBatch(names)
                } else {
                    Toast.makeText(this, "No se ingresaron nombres", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun importStudentsBatch(names: List<String>) {
        progressBar.visibility = ProgressBar.VISIBLE

        val students = names.map { CreateStudentRequest(full_name = it) }
        val request = BatchStudentsRequest(students = students)

        RetrofitClient.instance.importStudentsJson(sectionId, request)
            .enqueue(object : Callback<ImportStudentsResponse> {
                override fun onResponse(
                    call: Call<ImportStudentsResponse>,
                    response: Response<ImportStudentsResponse>
                ) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        val result = response.body()
                        showImportResult(result)
                        loadStudents()
                    } else {
                        Toast.makeText(
                            this@StudentsActivity,
                            "Error al importar estudiantes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ImportStudentsResponse>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@StudentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ============ IMPORTACIÓN TXT ============
    private fun openTxtPicker() {
        val instructionsDialog = TxtImportInstructionsDialogFragment {
            openTxtFileSelector()
        }
        instructionsDialog.show(supportFragmentManager, "TxtInstructions")
    }

    private fun openTxtFileSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        txtPickerLauncher.launch(intent)
    }

    // ============ IMPORTACIÓN CSV ============
    private fun openCsvPicker() {
        val instructionsDialog = CsvImportInstructionsDialogFragment {
            openCsvFileSelector()
        }
        instructionsDialog.show(supportFragmentManager, "CsvInstructions")
    }

    private fun openCsvFileSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values"))
        }
        filePickerLauncher.launch(intent)
    }

    // ============ MANEJO DE ARCHIVOS ============
    private fun handleFileImport(uri: Uri, isCsv: Boolean) {
        Log.d(TAG, "=== HANDLE FILE IMPORT ===")
        Log.d(TAG, "URI: $uri")
        Log.d(TAG, "Is CSV: $isCsv")

        try {
            val fileName = getFileName(uri)
            Log.d(TAG, "File name: $fileName")

            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Input stream is null!")
                Toast.makeText(this, "No se pudo abrir el archivo", Toast.LENGTH_LONG).show()
                return
            }

            val content = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            Log.d(TAG, "File content length: ${content.length} characters")
            Log.d(TAG, "File content preview: ${content.take(100)}")

            if (content.isEmpty()) {
                Toast.makeText(this, "El archivo está vacío", Toast.LENGTH_LONG).show()
                return
            }

            val extension = if (isCsv) "csv" else "txt"
            val tempFile = File(cacheDir, "import_${System.currentTimeMillis()}.$extension")
            tempFile.writeText(content)

            Log.d(TAG, "Temp file created: ${tempFile.absolutePath}")
            Log.d(TAG, "Temp file size: ${tempFile.length()} bytes")

            if (isCsv) {
                uploadCsvFile(tempFile)
            } else {
                uploadTxtFile(tempFile)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            Toast.makeText(this, "Error de permisos: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file", e)
            Toast.makeText(this, "Error al leer el archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }
        return fileName ?: "unknown"
    }

    private fun uploadTxtFile(file: File) {
        Log.d(TAG, "=== UPLOAD TXT FILE ===")
        Log.d(TAG, "Section ID: $sectionId")
        Log.d(TAG, "File path: ${file.absolutePath}")
        Log.d(TAG, "File exists: ${file.exists()}")
        Log.d(TAG, "File size: ${file.length()} bytes")

        progressBar.visibility = ProgressBar.VISIBLE

        val requestFile = file.asRequestBody("text/plain".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        Log.d(TAG, "Making API call to: ${RetrofitClient.instance}")

        RetrofitClient.instance.importStudentsTxt(sectionId, body)
            .enqueue(object : Callback<ImportStudentsResponse> {
                override fun onResponse(
                    call: Call<ImportStudentsResponse>,
                    response: Response<ImportStudentsResponse>
                ) {
                    Log.d(TAG, "Response received")
                    Log.d(TAG, "Response code: ${response.code()}")
                    Log.d(TAG, "Response message: ${response.message()}")
                    Log.d(TAG, "Response body: ${response.body()}")

                    progressBar.visibility = ProgressBar.GONE
                    file.delete()

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "Success! Result: $result")
                        showImportResult(result)
                        loadStudents()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error body: $errorBody")
                        Toast.makeText(
                            this@StudentsActivity,
                            errorBody ?: "Error al importar TXT",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ImportStudentsResponse>, t: Throwable) {
                    Log.e(TAG, "API call failed!", t)
                    Log.e(TAG, "Error message: ${t.message}")
                    Log.e(TAG, "Error cause: ${t.cause}")

                    progressBar.visibility = ProgressBar.GONE
                    file.delete()
                    Toast.makeText(
                        this@StudentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun uploadCsvFile(file: File) {
        progressBar.visibility = ProgressBar.VISIBLE

        val requestFile = file.asRequestBody("text/csv".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        RetrofitClient.instance.importStudentsCsv(sectionId, body)
            .enqueue(object : Callback<ImportStudentsResponse> {
                override fun onResponse(
                    call: Call<ImportStudentsResponse>,
                    response: Response<ImportStudentsResponse>
                ) {
                    progressBar.visibility = ProgressBar.GONE
                    file.delete()

                    if (response.isSuccessful) {
                        val result = response.body()
                        showImportResult(result)
                        loadStudents()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@StudentsActivity,
                            errorBody ?: "Error al importar CSV",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ImportStudentsResponse>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    file.delete()
                    Toast.makeText(
                        this@StudentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showImportResult(result: ImportStudentsResponse?) {
        Log.d(TAG, "=== IMPORT RESULT ===")
        Log.d(TAG, "Result is null: ${result == null}")

        if (result == null) {
            Toast.makeText(this, "Error: Respuesta vacía del servidor", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Imported: ${result.imported}")
        Log.d(TAG, "Successes: ${result.successes}")
        Log.d(TAG, "Errors: ${result.errors}")

        val message = buildString {
            append("Importados: ${result.imported}\n\n")

            if (result.successes.isNotEmpty()) {
                append("✓ Exitosos (${result.successes.size}):\n")
                result.successes.take(5).forEach { append("  • $it\n") }
                if (result.successes.size > 5) {
                    append("  ... y ${result.successes.size - 5} más\n")
                }
                append("\n")
            }

            if (result.errors.isNotEmpty()) {
                append("✗ Errores (${result.errors.size}):\n")
                result.errors.take(5).forEach { append("  • $it\n") }
                if (result.errors.size > 5) {
                    append("  ... y ${result.errors.size - 5} más")
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Resultado de importación")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}