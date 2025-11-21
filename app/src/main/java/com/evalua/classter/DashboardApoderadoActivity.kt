package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.evalua.classter.adapters.GuardianStudentsAdapter
import com.evalua.classter.dialogs.AddStudentByDniDialog
import com.evalua.classter.dialogs.UserProfileMenuDialog
import com.evalua.classter.models.GuardianStudent
import com.evalua.classter.network.RetrofitClient
import com.evalua.classter.utils.NotificationManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class DashboardApoderadoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DashboardApoderadoActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvWelcome: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var rvStudents: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var cardInfo: MaterialCardView
    private lateinit var fabAddStudent: FloatingActionButton
    private lateinit var ibNotifications: ImageButton
    private lateinit var tvNotificationBadge: TextView

    // Notificaciones en el dashboard
    private lateinit var rvNotificationsDashboard: RecyclerView
    private lateinit var tvNoNotifications: TextView
    private lateinit var tvSeeAllNotifications: TextView
    private lateinit var btnTestNotification: com.google.android.material.button.MaterialButton
    private lateinit var notificationsAdapter: com.evalua.classter.adapters.NotificationsCompactAdapter

    private lateinit var adapter: GuardianStudentsAdapter
    private lateinit var notificationManager: NotificationManager
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_apoderado)

        if (!isUserLoggedIn()) {
            redirectToLogin()
            return
        }

        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt("user_id", 0)
        notificationManager = NotificationManager(this)

        initViews()
        setupToolbar()
        setupUserMenu()
        setupNotifications()
        setupNotificationsDashboard()
        setupRecyclerView()
        setupSwipeRefresh()
        loadUserData()
        loadStudents()

        // Solicitar permiso de notificaciones
        com.evalua.classter.utils.NotificationPermissionHelper.requestNotificationPermission(this)

        // Crear notificaciones de prueba
        createSampleNotifications()

        // Cargar notificaciones en el dashboard
        loadDashboardNotifications()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        com.evalua.classter.utils.NotificationPermissionHelper.handlePermissionResult(
            requestCode,
            grantResults,
            onGranted = {
                Log.d(TAG, "✓ Permiso de notificaciones concedido")
                Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show()
            },
            onDenied = {
                Log.w(TAG, "⚠️ Permiso de notificaciones denegado")
                Toast.makeText(this, "Las notificaciones push no estarán disponibles", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        rvStudents = findViewById(R.id.rvStudents)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        cardInfo = findViewById(R.id.cardInfo)
        fabAddStudent = findViewById(R.id.fabAddStudent)
        ibNotifications = findViewById(R.id.ibNotifications)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)

        // Views de notificaciones en el dashboard
        rvNotificationsDashboard = findViewById(R.id.rvNotificationsDashboard)
        tvNoNotifications = findViewById(R.id.tvNoNotifications)
        tvSeeAllNotifications = findViewById(R.id.tvSeeAllNotifications)
        btnTestNotification = findViewById(R.id.btnTestNotification)

        Log.d(TAG, "✓ Views inicializadas - Notificaciones integradas en el dashboard")

        // Setup FAB click listener
        fabAddStudent.setOnClickListener {
            showAddStudentDialog()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mis Estudiantes"
    }

    private fun showAddStudentDialog() {
        val dialog = AddStudentByDniDialog(this, userId) {
            // Callback cuando se agrega un estudiante
            loadStudents()
        }
        dialog.show()
    }

    private fun setupNotifications() {
        ibNotifications.setOnClickListener {
            try {
                val intent = Intent(this, NotificationsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Notificaciones próximamente", Toast.LENGTH_SHORT).show()
            }
        }
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val unreadCount = notificationManager.getUnreadCount()
        if (unreadCount > 0) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
        } else {
            tvNotificationBadge.visibility = View.VISIBLE // Visible para demo
        }
    }

    private fun setupNotificationsDashboard() {
        notificationsAdapter = com.evalua.classter.adapters.NotificationsCompactAdapter(
            notifications = emptyList(),
            notificationManager = notificationManager,
            onNotificationClick = { notification ->
                // Marcar como leída
                notificationManager.markAsRead(notification.id)
                loadDashboardNotifications()
                updateNotificationBadge()
            },
            onNotificationDismiss = { notification ->
                // Eliminar notificación con animación
                notificationManager.deleteNotification(notification.id)
                loadDashboardNotifications()
                updateNotificationBadge()
                Toast.makeText(this, "Notificación eliminada", Toast.LENGTH_SHORT).show()
            }
        )

        rvNotificationsDashboard.layoutManager = LinearLayoutManager(this)
        rvNotificationsDashboard.adapter = notificationsAdapter

        // Click en "Ver todas"
        tvSeeAllNotifications.setOnClickListener {
            try {
                val intent = Intent(this, NotificationsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Notificaciones próximamente", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón de prueba para generar notificación push
        btnTestNotification.setOnClickListener {
            generateTestNotification()
        }
    }

    /**
     * Verificar nuevas calificaciones para un estudiante específico
     * y notificar al apoderado automáticamente
     */
    private suspend fun checkNewGradesForStudent(studentUserId: Int, studentName: String) {
        try {
            // Obtener calificaciones actuales del estudiante
            val currentGrades = RetrofitClient.apiService.getStudentGrades(studentUserId)

            // Leer calificaciones anteriores guardadas
            val prefs = getSharedPreferences("student_grades_$studentUserId", MODE_PRIVATE)
            val previousGradesJson = prefs.getString("grades", "[]")
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.evalua.classter.models.StudentGrade>>() {}.type
            val previousGrades: List<com.evalua.classter.models.StudentGrade> =
                if (previousGradesJson != "[]") gson.fromJson(previousGradesJson, type) else emptyList()

            // Comparar y detectar nuevas calificaciones
            if (previousGrades.isNotEmpty()) {
                val previousIds = previousGrades.map { "${it.competencyName}_${it.sessionTitle}" }.toSet()
                val newGrades = currentGrades.filter { grade ->
                    val gradeId = "${grade.competencyName}_${grade.sessionTitle}"
                    !previousIds.contains(gradeId)
                }

                // Generar notificación para cada nueva calificación
                newGrades.forEach { grade ->
                    val titulo = "🎓 Nueva calificación - $studentName"
                    val competencia = grade.competencyName ?: "Competencia"
                    val sesion = grade.sessionTitle ?: "Sesión"
                    val mensaje = "$competencia: ${grade.value} - $sesion"

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        notificationManager.addNotification(
                            title = titulo,
                            message = mensaje,
                            type = com.evalua.classter.models.NotificationType.NEW_GRADE,
                            showPush = true,  // ← Notificación push automática para el apoderado
                            extraData = "{\"studentId\":$studentUserId,\"studentName\":\"$studentName\",\"value\":\"${grade.value}\"}"
                        )
                    }

                    Log.d(TAG, "📬 Nueva calificación detectada para $studentName: $competencia - ${grade.value}")
                }

                // Actualizar dashboard si hay nuevas calificaciones
                if (newGrades.isNotEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        loadDashboardNotifications()
                        updateNotificationBadge()
                    }
                }
            }

            // Guardar calificaciones actuales para la próxima comparación
            val currentGradesJson = gson.toJson(currentGrades)
            prefs.edit().putString("grades", currentGradesJson).apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error verificando calificaciones de $studentName: ${e.message}")
        }
    }

    /**
     * Generar notificación de prueba para demostración
     * Simula notificaciones relevantes para apoderados
     */
    private fun generateTestNotification() {
        val tiposNotificacion = listOf(
            Triple("📚 Nuevo estudiante agregado",
                   "Se ha vinculado un nuevo estudiante a tu cuenta",
                   com.evalua.classter.models.NotificationType.STUDENT_ADDED),
            Triple("⭐ Calificación destacada",
                   "Uno de tus estudiantes obtuvo AD en Matemática",
                   com.evalua.classter.models.NotificationType.NEW_GRADE),
            Triple("📝 Actualización de notas",
                   "Se han actualizado las calificaciones del bimestre",
                   com.evalua.classter.models.NotificationType.GRADE_UPDATED),
            Triple("📅 Recordatorio",
                   "Revisa el progreso académico de tus estudiantes",
                   com.evalua.classter.models.NotificationType.REMINDER),
            Triple("ℹ️ Información importante",
                   "Nueva funcionalidad disponible en la app",
                   com.evalua.classter.models.NotificationType.SYSTEM)
        )

        val (titulo, mensaje, tipo) = tiposNotificacion.random()

        // Crear notificación con PUSH habilitado
        notificationManager.addNotification(
            title = titulo,
            message = mensaje,
            type = tipo,
            showPush = true  // ← Esto hace que aparezca como notificación push
        )

        // Recargar notificaciones en el dashboard
        loadDashboardNotifications()
        updateNotificationBadge()

        // Mensaje de confirmación
        Toast.makeText(
            this,
            "✓ Notificación enviada\nRevisa tu barra de notificaciones",
            Toast.LENGTH_LONG
        ).show()

        Log.d(TAG, "📬 Notificación push generada para apoderado: $titulo")
    }

    private fun loadDashboardNotifications() {
        val allNotifications = notificationManager.getNotifications()

        // Mostrar solo las 3 más recientes
        val recentNotifications = allNotifications.take(3)

        if (recentNotifications.isEmpty()) {
            rvNotificationsDashboard.visibility = View.GONE
            tvNoNotifications.visibility = View.VISIBLE
            tvSeeAllNotifications.visibility = View.GONE
        } else {
            rvNotificationsDashboard.visibility = View.VISIBLE
            tvNoNotifications.visibility = View.GONE
            tvSeeAllNotifications.visibility = if (allNotifications.size > 3) View.VISIBLE else View.GONE
            notificationsAdapter.updateNotifications(recentNotifications)
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
        loadDashboardNotifications()
    }

    private fun setupUserMenu() {
        try {
            val ibUserMenu = findViewById<ImageButton>(R.id.ibUserMenu)
            ibUserMenu.setOnClickListener {
                Log.d(TAG, "🔄 Abriendo menú de usuario...")
                val userMenuDialog = UserProfileMenuDialog(this)
                userMenuDialog.show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error configurando menú de usuario", e)
        }
    }

    private fun setupRecyclerView() {
        adapter = GuardianStudentsAdapter(emptyList())
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadStudents()
        }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("user_full_name", "Apoderado")
        tvWelcome.text = "Bienvenido, $userName"
        Log.d(TAG, "👤 Usuario cargado: $userName")
    }

    private fun loadStudents() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Leer estudiantes guardados localmente
                val prefs = getSharedPreferences("guardian_students_$userId", MODE_PRIVATE)
                val studentsJson = prefs.getString("students_list", "[]")

                if (studentsJson == "[]") {
                    showEmptyState(true)
                    tvEmptyState.text = "No tienes estudiantes agregados.\n\nUsa el botón + para agregar un estudiante por su DNI."
                    tvStudentCount.text = "0 estudiantes"
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    return@launch
                }

                // Parsear estudiantes
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<GuardianStudentLocalData>>() {}.type
                val localStudents: List<GuardianStudentLocalData> = gson.fromJson(studentsJson, type)

                // Obtener información completa de cada estudiante del backend
                val guardianStudents = mutableListOf<GuardianStudent>()

                for (student in localStudents) {
                    try {
                        // Obtener matrículas del estudiante
                        val enrollments = RetrofitClient.apiService.getStudentEnrollments(student.userId)

                        if (enrollments.isNotEmpty()) {
                            val enrollment = enrollments.first()
                            guardianStudents.add(
                                GuardianStudent(
                                    studentId = student.userId,
                                    studentName = student.fullName,
                                    sectionLetter = enrollment.sectionLetter,
                                    gradeNumber = enrollment.gradeNumber,
                                    bimesterName = enrollment.bimesterName,
                                    year = enrollment.year
                                )
                            )

                            // Detectar nuevas calificaciones para este estudiante
                            checkNewGradesForStudent(student.userId, student.fullName)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cargando datos de ${student.fullName}: ${e.message}")
                    }
                }

                if (guardianStudents.isEmpty()) {
                    showEmptyState(true)
                    tvEmptyState.text = "No se pudieron cargar los datos de los estudiantes.\n\nIntenta actualizar."
                } else {
                    showEmptyState(false)
                    adapter.updateStudents(guardianStudents)
                }

                tvStudentCount.text = "${guardianStudents.size} estudiante${if (guardianStudents.size != 1) "s" else ""}"

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando estudiantes: ${e.message}")
                showEmptyState(true)
                tvEmptyState.text = "Error al cargar estudiantes.\n\n${e.message}"
                tvStudentCount.text = "0 estudiantes"
            } finally {
                showLoading(false)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // Clase local para guardar estudiantes
    data class GuardianStudentLocalData(
        val userId: Int,
        val dni: String,
        val fullName: String
    )


    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        rvStudents.visibility = if (show) View.GONE else View.VISIBLE
    }


    private fun createSampleNotifications() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("first_time_guardian", true)

        if (isFirstTime) {
            // Notificación de bienvenida
            notificationManager.addNotification(
                title = "¡Bienvenido!",
                message = "Desde aquí puedes monitorear el progreso académico de tus estudiantes.",
                type = com.evalua.classter.models.NotificationType.SYSTEM
            )

            // Notificación de ejemplo
            notificationManager.addNotification(
                title = "Tip: Agregar estudiantes",
                message = "Usa el botón + para agregar estudiantes a tu lista usando su DNI.",
                type = com.evalua.classter.models.NotificationType.SYSTEM
            )

            prefs.edit().putBoolean("first_time_guardian", false).apply()
        }
    }


    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPreferences.getInt("user_id", 0) > 0
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}