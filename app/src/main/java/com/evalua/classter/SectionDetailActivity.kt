package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class SectionDetailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SectionDetailActivity"
    }

    private var sectionId: Int = -1
    private var gradeId: Int = -1
    private var gradeNumber: Int = -1
    private var sectionLetter: String = ""
    private var bimesterName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_detail)

        sectionId = intent.getIntExtra("section_id", -1)
        gradeId = intent.getIntExtra("grade_id", -1)
        gradeNumber = intent.getIntExtra("grade_number", -1)
        sectionLetter = intent.getStringExtra("section_letter") ?: ""
        bimesterName = intent.getStringExtra("bimester_name") ?: ""

        Log.d(TAG, "=== Datos recibidos en SectionDetailActivity ===")
        Log.d(TAG, "Section ID: $sectionId")
        Log.d(TAG, "Grade ID: $gradeId")
        Log.d(TAG, "Grade Number: $gradeNumber")
        Log.d(TAG, "Section Letter: $sectionLetter")
        Log.d(TAG, "Bimester Name: $bimesterName")

        if (sectionId == -1 || gradeNumber == -1 || sectionLetter.isEmpty()) {
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
        supportActionBar?.title = "Detalles de Sección"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        val tvTitle = findViewById<TextView>(R.id.tvSectionTitle)
        tvTitle.text = "$bimesterName Bimestre - $gradeNumber° - Sección $sectionLetter"
        Log.d(TAG, "Título configurado: ${tvTitle.text}")
    }

    private fun setupClickListeners() {
        // Botón de Alumnos
        val cardStudents = findViewById<MaterialCardView>(R.id.cardStudents)
        cardStudents.setOnClickListener {
            Log.d(TAG, "Click en botón Alumnos")
            openStudentsActivity()
        }

        // Botón de Sesiones
        val cardGrades = findViewById<MaterialCardView>(R.id.cardGrades)
        cardGrades.setOnClickListener {
            Log.d(TAG, "Click en botón Sesiones")
            openSessionsActivity()
        }

        // ✅ NUEVO: Botón de Reportes/Consolidado
        val cardReports = findViewById<MaterialCardView>(R.id.cardReports)
        cardReports.setOnClickListener {
            Log.d(TAG, "Click en botón Reportes")
            openReportsActivity()
        }
    }

    private fun openStudentsActivity() {
        val intent = Intent(this, StudentsActivity::class.java).apply {
            putExtra("section_id", sectionId)
            putExtra("section_letter", sectionLetter)
            putExtra("grade_id", gradeId)
            putExtra("grade_number", gradeNumber)
            putExtra("bimester_name", bimesterName)
        }

        Log.d(TAG, "Abriendo StudentsActivity con:")
        Log.d(TAG, "  section_id: $sectionId")
        Log.d(TAG, "  section_letter: $sectionLetter")
        Log.d(TAG, "  grade_number: $gradeNumber")
        Log.d(TAG, "  bimester_name: $bimesterName")

        startActivity(intent)
    }

    private fun openSessionsActivity() {
        val intent = Intent(this, SessionsActivity::class.java).apply {
            putExtra("SECTION_ID", sectionId)
            putExtra("SECTION_NAME", "$gradeNumber° $sectionLetter - $bimesterName Bimestre")
        }

        Log.d(TAG, "Abriendo SessionsActivity con:")
        Log.d(TAG, "  SECTION_ID: $sectionId")
        Log.d(TAG, "  SECTION_NAME: $gradeNumber° $sectionLetter - $bimesterName Bimestre")

        startActivity(intent)
    }

    // ✅ NUEVA FUNCIÓN: Abrir ReportsActivity
    private fun openReportsActivity() {
        val sectionName = "$gradeNumber° $sectionLetter"

        val intent = Intent(this, ReportsActivity::class.java).apply {
            putExtra("SECTION_ID", sectionId)
            putExtra("COMPETENCY_ID", -1)
            putExtra("SECTION_NAME", sectionName)
            putExtra("BIMESTER_NAME", bimesterName)
        }

        Log.d(TAG, "Abriendo ReportsActivity con:")
        Log.d(TAG, "  SECTION_ID: $sectionId")
        Log.d(TAG, "  SECTION_NAME: $sectionName")
        Log.d(TAG, "  BIMESTER_NAME: $bimesterName")

        startActivity(intent)
    }
}