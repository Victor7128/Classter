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
import com.evalua.classter.adapters.StudentGradesAdapter
import com.evalua.classter.dialogs.UserProfileMenuDialog
import com.evalua.classter.models.StudentEnrollment
import com.evalua.classter.models.StudentGrade
import com.evalua.classter.network.RetrofitClient
import com.evalua.classter.utils.NotificationManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class DashboardAlumnosActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DashboardAlumnosActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvWelcome: TextView
    private lateinit var tvEnrollmentInfo: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvGrades: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var ibNotifications: ImageButton
    private lateinit var tvNotificationBadge: TextView

    // Notificaciones en el dashboard
    private lateinit var rvNotificationsDashboard: RecyclerView
    private lateinit var tvNoNotifications: TextView
    private lateinit var tvSeeAllNotifications: TextView
    private lateinit var btnTestNotification: com.google.android.material.button.MaterialButton
    private lateinit var notificationsAdapter: com.evalua.classter.adapters.NotificationsCompactAdapter

    private lateinit var adapter: StudentGradesAdapter
    private lateinit var notificationManager: NotificationManager
    private var allGrades: List<StudentGrade> = emptyList()
    private var enrollments: List<StudentEnrollment> = emptyList()
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_alumnos)

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
        loadStudentData()

        // Solicitar permiso de notificaciones
        com.evalua.classter.utils.NotificationPermissionHelper.requestNotificationPermission(this)

        // Crear notificaciones de prueba
        createWelcomeNotification()

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
        tvEnrollmentInfo = findViewById(R.id.tvEnrollmentInfo)
        chipGroup = findViewById(R.id.chipGroup)
        rvGrades = findViewById(R.id.rvGrades)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        ibNotifications = findViewById(R.id.ibNotifications)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)

        // Views de notificaciones en el dashboard
        rvNotificationsDashboard = findViewById(R.id.rvNotificationsDashboard)
        tvNoNotifications = findViewById(R.id.tvNoNotifications)
        tvSeeAllNotifications = findViewById(R.id.tvSeeAllNotifications)
        btnTestNotification = findViewById(R.id.btnTestNotification)

        Log.d(TAG, "✓ Views inicializadas - Notificaciones integradas en el dashboard")
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mis Calificaciones"
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
     * Cargar calificaciones guardadas previamente
     */
    private fun loadPreviousGrades(): List<StudentGrade> {
        return try {
            val prefs = getSharedPreferences("student_grades_$userId", MODE_PRIVATE)
            val gradesJson = prefs.getString("grades", "[]")
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<StudentGrade>>() {}.type
            if (gradesJson != "[]") {
                gson.fromJson(gradesJson, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando calificaciones guardadas: ${e.message}")
            emptyList()
        }
    }

    /**
     * Guardar calificaciones actuales en SharedPreferences
     * para poder detectar nuevas calificaciones en el futuro
     */
    private fun saveCurrentGrades(grades: List<StudentGrade>) {
        try {
            val prefs = getSharedPreferences("student_grades_$userId", MODE_PRIVATE)
            val gson = com.google.gson.Gson()
            val gradesJson = gson.toJson(grades)
            prefs.edit().putString("grades", gradesJson).apply()
            Log.d(TAG, "💾 Calificaciones guardadas para comparación futura")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando calificaciones: ${e.message}")
        }
    }

    /**
     * Detectar nuevas calificaciones comparando con las anteriores
     * y generar notificaciones push automáticamente
     */
    private fun detectAndNotifyNewGrades(
        previousGrades: List<StudentGrade>,
        currentGrades: List<StudentGrade>
    ) {
        // IDs de calificaciones anteriores (usando competencyName y sessionTitle)
        val previousIds = previousGrades.map { "${it.competencyName}_${it.sessionTitle}" }.toSet()

        // Encontrar calificaciones nuevas
        val newGrades = currentGrades.filter { grade ->
            val gradeId = "${grade.competencyName}_${grade.sessionTitle}"
            !previousIds.contains(gradeId)
        }

        // Generar notificación para cada calificación nueva
        newGrades.forEach { grade ->
            val titulo = "🎓 Nueva calificación registrada"
            val competencia = grade.competencyName ?: "Competencia"
            val sesion = grade.sessionTitle ?: "Sesión"
            val mensaje = "$competencia: ${grade.value} - $sesion"

            notificationManager.addNotification(
                title = titulo,
                message = mensaje,
                type = com.evalua.classter.models.NotificationType.NEW_GRADE,
                showPush = true,  // ← Notificación push automática
                extraData = "{\"competency\":\"$competencia\",\"value\":\"${grade.value}\"}"
            )

            Log.d(TAG, "📬 Nueva calificación detectada: $competencia - ${grade.value}")
        }

        // Actualizar dashboard si hay nuevas notificaciones
        if (newGrades.isNotEmpty()) {
            loadDashboardNotifications()
            updateNotificationBadge()
        }
    }

    /**
     * Generar notificación de prueba para demostración
     * Esto simula lo que pasaría cuando un docente registra una calificación
     */
    private fun generateTestNotification() {
        val materias = listOf("Matemática", "Comunicación", "Ciencias", "Historia", "Arte")
        val calificaciones = listOf("AD", "A", "B")
        val mensajes = listOf(
            "¡Excelente trabajo! Sigue así.",
            "Tu esfuerzo está dando resultados.",
            "Has mejorado significativamente.",
            "Buen desempeño en la evaluación."
        )

        val materia = materias.random()
        val calificacion = calificaciones.random()
        val mensaje = mensajes.random()

        // Crear notificación con PUSH habilitado
        notificationManager.addNotification(
            title = "🎓 Nueva calificación: $calificacion",
            message = "$materia - $mensaje",
            type = com.evalua.classter.models.NotificationType.NEW_GRADE,
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

        Log.d(TAG, "📬 Notificación push generada: $materia - $calificacion")
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
        adapter = StudentGradesAdapter(emptyList())
        rvGrades.layoutManager = LinearLayoutManager(this)
        rvGrades.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadStudentData()
        }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("user_full_name", "Estudiante")
        tvWelcome.text = "Bienvenido, $userName"
    }

    private fun loadStudentData() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Cargar matrículas
                enrollments = RetrofitClient.apiService.getStudentEnrollments(userId)
                updateEnrollmentInfo()

                // Cargar calificaciones guardadas previamente para comparar
                val previousGrades = loadPreviousGrades()

                // Cargar calificaciones del backend
                allGrades = RetrofitClient.apiService.getStudentGrades(userId)

                // Detectar nuevas calificaciones y generar notificaciones
                if (previousGrades.isNotEmpty()) {
                    detectAndNotifyNewGrades(previousGrades, allGrades)
                }

                if (allGrades.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    adapter.updateGrades(allGrades)
                    setupFilterChips()
                }

                // Guardar calificaciones actuales para la próxima comparación
                saveCurrentGrades(allGrades)

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos del estudiante", e)
                Toast.makeText(
                    this@DashboardAlumnosActivity,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyState(true)
            } finally {
                showLoading(false)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateEnrollmentInfo() {
        if (enrollments.isNotEmpty()) {
            val enrollment = enrollments.first()
            tvEnrollmentInfo.text = "${enrollment.gradeNumber}° ${enrollment.sectionLetter} - ${enrollment.bimesterName} Bimestre ${enrollment.year}"
        }
    }

    private fun setupFilterChips() {
        chipGroup.removeAllViews()

        // Chip "Todas"
        val chipAll = Chip(this).apply {
            text = "Todas"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                adapter.updateGrades(allGrades)
            }
        }
        chipGroup.addView(chipAll)

        // Chips por sesión
        val sessions = allGrades.map { it.sessionTitle }.distinct()
        sessions.forEach { sessionTitle ->
            val chip = Chip(this).apply {
                text = sessionTitle
                isCheckable = true
                setOnClickListener {
                    val filtered = allGrades.filter { it.sessionTitle == sessionTitle }
                    adapter.updateGrades(filtered)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvGrades.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        rvGrades.visibility = if (show) View.GONE else View.VISIBLE
    }


    private fun createWelcomeNotification() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("first_time_student", true)

        if (isFirstTime) {
            // Notificación de bienvenida
            notificationManager.addNotification(
                title = "¡Bienvenido!",
                message = "Aquí podrás ver todas tus calificaciones y seguir tu progreso académico.",
                type = com.evalua.classter.models.NotificationType.SYSTEM
            )

            // Notificación de ejemplo 1
            notificationManager.addNotification(
                title = "Nueva calificación registrada",
                message = "Tu docente ha registrado una calificación en Matemática. ¡Revísala!",
                type = com.evalua.classter.models.NotificationType.NEW_GRADE
            )

            // Notificación de ejemplo 2
            notificationManager.addNotification(
                title = "Recordatorio",
                message = "No olvides revisar tus calificaciones del bimestre actual.",
                type = com.evalua.classter.models.NotificationType.REMINDER
            )

            prefs.edit().putBoolean("first_time_student", false).apply()
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