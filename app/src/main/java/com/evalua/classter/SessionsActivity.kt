package com.evalua.classter

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evalua.classter.adapters.SessionsAdapter
import com.evalua.classter.databinding.ActivitySessionsBinding
import com.evalua.classter.models.NewSessionRequest
import com.evalua.classter.models.Session
import com.evalua.classter.models.UpdateSessionRequest
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionsBinding
    private lateinit var adapter: SessionsAdapter
    private var sectionId: Int = -1
    private var sectionName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del Intent
        sectionId = intent.getIntExtra("SECTION_ID", -1)
        sectionName = intent.getStringExtra("SECTION_NAME") ?: "Sesiones"

        if (sectionId == -1) {
            Toast.makeText(this, "Error: Sección no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        loadSessions()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Sesiones - $sectionName"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SessionsAdapter(
            sessions = emptyList(),
            onSessionClick = { session ->
                // Navegar a SessionDetailActivity
                openSessionDetail(session)
            },
            onSessionOptionsClick = { session, view ->
                showSessionOptionsMenu(session, view)
            }
        )
        binding.rvSessions.layoutManager = LinearLayoutManager(this)
        binding.rvSessions.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadSessions()
        }
    }

    private fun setupFab() {
        binding.fabAddSession.setOnClickListener {
            showAddSessionDialog()
        }
    }

    private fun loadSessions() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val sessions = RetrofitClient.apiService.getSessions(sectionId)
                Log.d("SessionsActivity", "Sessions loaded: ${sessions.size}")
                adapter.updateSessions(sessions)

                if (sessions.isEmpty()) {
                    Toast.makeText(
                        this@SessionsActivity,
                        "No hay sesiones registradas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("SessionsActivity", "Error loading sessions", e)
                Toast.makeText(
                    this@SessionsActivity,
                    "Error al cargar sesiones: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAddSessionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_session, null)
        val etSessionTitle = dialogView.findViewById<TextInputEditText>(R.id.etSessionTitle)
        val etSessionDate = dialogView.findViewById<TextInputEditText>(R.id.etSessionDate)

        // Pre-llenar con fecha actual
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etSessionDate.setText(today)

        // También permitir click en el campo de texto
        etSessionDate.setOnClickListener {
            showDatePicker { selectedDate ->
                etSessionDate.setText(selectedDate)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Nueva Sesión")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val title = etSessionTitle.text.toString().trim()
                val date = etSessionDate.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createSession(title, date)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditSessionDialog(session: Session) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_session, null)
        val etSessionTitle = dialogView.findViewById<TextInputEditText>(R.id.etSessionTitle)
        val etSessionDate = dialogView.findViewById<TextInputEditText>(R.id.etSessionDate)

        // Pre-llenar con datos actuales
        etSessionTitle.setText(session.title)
        etSessionDate.setText(session.date)

        // También permitir click en el campo de texto
        etSessionDate.setOnClickListener {
            showDatePicker(session.date) { selectedDate ->
                etSessionDate.setText(selectedDate)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Sesión ${session.number}")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val title = etSessionTitle.text.toString().trim()
                val date = etSessionDate.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateSession(session.id, title, date)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createSession(title: String, date: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = NewSessionRequest(title = title, date = date)
                val newSession = RetrofitClient.apiService.createSession(sectionId, request)
                Log.d("SessionsActivity", "Session created: $newSession")

                Toast.makeText(
                    this@SessionsActivity,
                    "Sesión creada: ${newSession.title}",
                    Toast.LENGTH_SHORT
                ).show()

                loadSessions()
            } catch (e: Exception) {
                Log.e("SessionsActivity", "Error creating session", e)
                Toast.makeText(
                    this@SessionsActivity,
                    "Error al crear sesión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateSession(sessionId: Int, title: String, date: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = UpdateSessionRequest(title = title, date = date)
                val updatedSession = RetrofitClient.apiService.updateSession(sessionId, request)
                Log.d("SessionsActivity", "Session updated: $updatedSession")

                Toast.makeText(
                    this@SessionsActivity,
                    "Sesión actualizada",
                    Toast.LENGTH_SHORT
                ).show()

                loadSessions()
            } catch (e: Exception) {
                Log.e("SessionsActivity", "Error updating session", e)
                Toast.makeText(
                    this@SessionsActivity,
                    "Error al actualizar sesión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showSessionOptionsMenu(session: Session, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_session_options, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditSessionDialog(session)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog(session)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmationDialog(session: Session) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Sesión")
            .setMessage("¿Estás seguro de que deseas eliminar la sesión ${session.number}: ${session.title}?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteSession(session.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteSession(sessionId: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteSession(sessionId)
                Log.d("SessionsActivity", "Session deleted: $sessionId")

                Toast.makeText(
                    this@SessionsActivity,
                    "Sesión eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                loadSessions()
            } catch (e: Exception) {
                Log.e("SessionsActivity", "Error deleting session", e)
                Toast.makeText(
                    this@SessionsActivity,
                    "Error al eliminar sesión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDatePicker(initialDate: String? = null, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        // Si hay fecha inicial, parsearla
        if (!initialDate.isNullOrEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(initialDate)
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                Log.w("SessionsActivity", "Error parsing initial date: $initialDate", e)
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth
                )
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun openSessionDetail(session: Session) {
        val intent = Intent(this, SessionDetailActivity::class.java).apply {
            putExtra("SESSION_ID", session.id)
            putExtra("SESSION_NUMBER", session.number)
            putExtra("SESSION_TITLE", session.title)
            putExtra("SESSION_DATE", session.date)
            putExtra("SECTION_ID", sectionId)
            putExtra("SECTION_NAME", sectionName)
        }

        Log.d("SessionsActivity", "Abriendo SessionDetailActivity con:")
        Log.d("SessionsActivity", "  SESSION_ID: ${session.id}")
        Log.d("SessionsActivity", "  SESSION_TITLE: ${session.title}")

        startActivity(intent)
    }
}