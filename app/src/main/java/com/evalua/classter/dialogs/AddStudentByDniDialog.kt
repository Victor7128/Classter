package com.evalua.classter.dialogs

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.evalua.classter.R
import com.evalua.classter.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddStudentByDniDialog(
    context: Context,
    private val guardianUserId: Int,
    private val onStudentAdded: () -> Unit
) : Dialog(context) {

    private lateinit var etSearchName: EditText
    private lateinit var etDni: EditText
    private lateinit var etFullName: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private var foundStudentUserId: Int? = null
    private var foundStudentName: String? = null
    private var foundStudentDni: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_add_student_by_dni)

        prefs = context.getSharedPreferences("guardian_students_${guardianUserId}", Context.MODE_PRIVATE)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        etSearchName = findViewById(R.id.etSearchName)
        etDni = findViewById(R.id.etDni)
        etFullName = findViewById(R.id.etFullName)
        btnSearch = findViewById(R.id.btnSearch)
        btnAdd = findViewById(R.id.btnAdd)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }


    private fun setupListeners() {
        btnSearch.setOnClickListener {
            searchStudentByDni()
        }

        btnAdd.setOnClickListener {
            addStudentRelationship()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun searchStudentByDni() {
        val searchName = etSearchName.text.toString().trim()
        val dni = etDni.text.toString().trim()

        if (searchName.isEmpty()) {
            showError("Ingrese el nombre o apellido del estudiante")
            return
        }

        if (searchName.length < 3) {
            showError("Ingrese al menos 3 caracteres del nombre")
            return
        }

        if (dni.isEmpty()) {
            showError("Ingrese el DNI del estudiante para validar")
            return
        }

        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            showError("El DNI debe tener 8 dígitos")
            return
        }

        hideError()
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AddStudentDialog", "Buscando estudiante: nombre='$searchName', dni='$dni'")

                // Buscar por nombre
                val students = RetrofitClient.apiService.searchStudentsByName(searchName)
                Log.d("AddStudentDialog", "Resultados encontrados: ${students.size}")

                var studentFound: Pair<Int, String>? = null

                if (students.isNotEmpty()) {
                    // Filtrar estudiantes con cuenta
                    val studentsWithAccount = students.filter { it.userId != null }
                    Log.d("AddStudentDialog", "Estudiantes con cuenta: ${studentsWithAccount.size}")

                    // Verificar perfiles para encontrar el DNI correcto
                    for (student in studentsWithAccount.take(20)) {
                        try {
                            val profile = RetrofitClient.apiService.getStudentProfile(student.userId!!)
                            val profileDni = profile.profile.dni

                            Log.d("AddStudentDialog", "Verificando: ${student.fullName} (DNI: $profileDni)")

                            if (profileDni == dni) {
                                studentFound = Pair(student.userId, profile.profile.fullName)
                                Log.d("AddStudentDialog", "✓ ¡DNI coincide!")
                                break
                            }
                        } catch (e: Exception) {
                            Log.w("AddStudentDialog", "Error obteniendo perfil: ${e.message}")
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (studentFound == null) {
                        showError("⚠️ No se encontró ningún estudiante con DNI: $dni\n\n" +
                                "Verifique que:\n" +
                                "• El DNI sea correcto\n" +
                                "• El estudiante tenga una cuenta registrada\n" +
                                "• El DNI esté guardado en su perfil")
                        etFullName.setText("")
                        foundStudentUserId = null
                        foundStudentName = null
                        foundStudentDni = null
                        btnAdd.isEnabled = false
                    } else {
                        val (userId, fullName) = studentFound
                        Log.d("AddStudentDialog", "Estudiante encontrado: $fullName (user_id: $userId)")

                        // Obtener información de matrícula
                        try {
                            val enrollments = RetrofitClient.apiService.getStudentEnrollments(userId)
                            val enrollmentInfo = if (enrollments.isNotEmpty()) {
                                val e = enrollments.first()
                                "${e.gradeNumber}° ${e.sectionLetter}"
                            } else {
                                "Sin matrícula"
                            }

                            etFullName.setText(fullName)
                            foundStudentUserId = userId
                            foundStudentName = fullName
                            foundStudentDni = dni
                            btnAdd.isEnabled = true
                            hideError()
                            Toast.makeText(
                                context,
                                "✓ $fullName - $enrollmentInfo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            // Si no podemos obtener matrícula, aún así permitir agregar
                            etFullName.setText(fullName)
                            foundStudentUserId = userId
                            foundStudentName = fullName
                            foundStudentDni = dni
                            btnAdd.isEnabled = true
                            hideError()
                            Toast.makeText(
                                context,
                                "✓ $fullName encontrado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    showLoading(false)
                }
            } catch (e: Exception) {
                Log.e("AddStudentDialog", "Error al buscar estudiante", e)
                withContext(Dispatchers.Main) {
                    showError("❌ Error al buscar estudiante:\n${e.message}\n\nVerifique su conexión e intente nuevamente.")
                    etFullName.setText("")
                    foundStudentUserId = null
                    foundStudentName = null
                    foundStudentDni = null
                    btnAdd.isEnabled = false
                    showLoading(false)
                }
            }
        }
    }

    private fun addStudentRelationship() {
        val studentUserId = foundStudentUserId
        val studentName = foundStudentName
        val studentDni = foundStudentDni

        if (studentUserId == null || studentName == null || studentDni == null) {
            showError("Primero busque al estudiante por DNI")
            return
        }

        try {
            // Obtener lista actual de estudiantes guardados
            val studentsJson = prefs.getString("students_list", "[]")
            val type = object : TypeToken<MutableList<GuardianStudentData>>() {}.type
            val studentsList: MutableList<GuardianStudentData> = gson.fromJson(studentsJson, type)

            // Verificar si ya está agregado
            if (studentsList.any { it.userId == studentUserId }) {
                showError("Este estudiante ya está en tu lista")
                return
            }

            // Agregar nuevo estudiante
            val newStudent = GuardianStudentData(
                userId = studentUserId,
                dni = studentDni,
                fullName = studentName
            )
            studentsList.add(newStudent)

            // Guardar en SharedPreferences
            val newJson = gson.toJson(studentsList)
            prefs.edit().putString("students_list", newJson).apply()

            // Crear notificación PUSH (aparecerá en la barra de notificaciones)
            val notificationManager = com.evalua.classter.utils.NotificationManager(context)
            notificationManager.addNotification(
                title = "✓ Estudiante agregado",
                message = "$studentName ahora está en tu lista. Podrás ver sus calificaciones y asistencias.",
                type = com.evalua.classter.models.NotificationType.STUDENT_ADDED,
                showPush = true  // ← Esto hace que aparezca como notificación push en el celular
            )

            Toast.makeText(context, "✓ $studentName agregado exitosamente", Toast.LENGTH_SHORT).show()
            dismiss()
            onStudentAdded()
        } catch (e: Exception) {
            showError("Error al guardar: ${e.message}")
        }
    }

    // Clase de datos para guardar estudiantes localmente
    data class GuardianStudentData(
        val userId: Int,
        val dni: String,
        val fullName: String
    )

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSearch.isEnabled = !show
        btnAdd.isEnabled = !show && foundStudentUserId != null
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        tvError.text = ""
        tvError.visibility = android.view.View.GONE
    }
}

