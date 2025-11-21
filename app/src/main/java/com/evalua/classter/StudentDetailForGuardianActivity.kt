package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.evalua.classter.adapters.StudentGradesAdapter
import com.evalua.classter.models.StudentGrade
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class StudentDetailForGuardianActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "StudentDetailGuardian"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvStudentName: TextView
    private lateinit var tvEnrollmentInfo: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvGrades: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    private lateinit var adapter: StudentGradesAdapter
    private var allGrades: List<StudentGrade> = emptyList()
    private var studentUserId: Int = 0
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail_for_guardian)

        studentUserId = intent.getIntExtra("student_user_id", 0)
        studentName = intent.getStringExtra("student_name") ?: "Estudiante"

        if (studentUserId == 0) {
            Toast.makeText(this, "Error: ID de estudiante inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        loadStudentGrades()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvEnrollmentInfo = findViewById(R.id.tvEnrollmentInfo)
        chipGroup = findViewById(R.id.chipGroup)
        rvGrades = findViewById(R.id.rvGrades)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Calificaciones"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        tvStudentName.text = studentName
    }

    private fun setupRecyclerView() {
        adapter = StudentGradesAdapter(emptyList())
        rvGrades.layoutManager = LinearLayoutManager(this)
        rvGrades.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadStudentGrades()
        }
    }

    private fun loadStudentGrades() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Cargar matrículas
                val enrollments = RetrofitClient.apiService.getStudentEnrollments(studentUserId)
                if (enrollments.isNotEmpty()) {
                    val enrollment = enrollments.first()
                    tvEnrollmentInfo.text = "${enrollment.gradeNumber}° ${enrollment.sectionLetter} - ${enrollment.bimesterName} Bimestre ${enrollment.year}"
                }

                // Cargar calificaciones
                allGrades = RetrofitClient.apiService.getStudentGrades(studentUserId)

                if (allGrades.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    adapter.updateGrades(allGrades)
                    setupFilterChips()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando calificaciones", e)
                Toast.makeText(
                    this@StudentDetailForGuardianActivity,
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
}

