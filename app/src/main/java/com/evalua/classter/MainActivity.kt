package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.adapters.BimesterAdapter
import com.evalua.classter.dialogs.UserProfileMenuDialog
import com.evalua.classter.models.BimesterData
import com.evalua.classter.models.Grade
import com.evalua.classter.models.Section
import com.evalua.classter.network.CreateGradeRequest
import com.evalua.classter.network.CreateSectionRequest
import com.evalua.classter.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var rvBimesters: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: BimesterAdapter

    private var expandedBimesterId: Int? = null
    private var expandedGradeId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar que el usuario esté logueado
        if (!isUserLoggedIn()) {
            redirectToLogin()
            return
        }

        // Cargar datos del usuario
        loadUserData()

        initViews()
        setupUserMenu()
        loadBimesters()
    }

    private fun initViews() {
        rvBimesters = findViewById(R.id.recyclerViewBimesters)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvBimesters.layoutManager = LinearLayoutManager(this)
    }

    private fun setupUserMenu() {
        try {
            val ibUserMenu = findViewById<ImageButton>(R.id.ibUserMenu)
            ibUserMenu.setOnClickListener {
                Log.d(TAG, "🔄 Abriendo menú de usuario...")
                val userMenuDialog = UserProfileMenuDialog(this)
                userMenuDialog.show()
            }
            Log.d(TAG, "✅ Menú de usuario configurado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error configurando menú de usuario", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("user_full_name", "")
        val userEmail = sharedPreferences.getString("user_email", "")
        val userRole = sharedPreferences.getString("user_role", "")

        Log.d("MainActivity", "👤 Usuario cargado: $userName ($userEmail) - Rol: $userRole")
    }

    // El resto de tus métodos (loadBimesters, addGrade, deleteGrade, etc.) permanecen igual
    private fun loadBimesters(keepExpanded: Boolean = false) {
        Log.d(TAG, "loadBimesters() - keepExpanded: $keepExpanded, expandedBimesterId: $expandedBimesterId, expandedGradeId: $expandedGradeId")

        progressBar.visibility = ProgressBar.VISIBLE
        tvEmptyState.visibility = TextView.GONE

        RetrofitClient.instance.getBimesters().enqueue(object : Callback<List<BimesterData>> {
            override fun onResponse(
                call: Call<List<BimesterData>>,
                response: Response<List<BimesterData>>
            ) {
                progressBar.visibility = ProgressBar.GONE
                if (response.isSuccessful) {
                    val bimesters = response.body() ?: emptyList()
                    if (bimesters.isEmpty()) {
                        tvEmptyState.visibility = TextView.VISIBLE
                    } else {
                        Log.d(TAG, "Creating adapter - expandedBimesterId: $expandedBimesterId, expandedGradeId: $expandedGradeId")

                        adapter = BimesterAdapter(
                            bimesters = bimesters,
                            expandedBimesterId = if (keepExpanded) expandedBimesterId else null,
                            expandedGradeId = if (keepExpanded) expandedGradeId else null,
                            onBimesterExpanded = { bimesterId, isExpanded ->
                                Log.d(TAG, "onBimesterExpanded - bimesterId: $bimesterId, isExpanded: $isExpanded")
                                expandedBimesterId = if (isExpanded) bimesterId else null
                                if (!isExpanded) {
                                    expandedGradeId = null
                                    Log.d(TAG, "Bimestre cerrado, reseteando expandedGradeId")
                                }
                            },
                            onGradeExpanded = { gradeId, isExpanded ->
                                Log.d(TAG, "onGradeExpanded - gradeId: $gradeId, isExpanded: $isExpanded")
                                expandedGradeId = if (isExpanded) gradeId else null
                            },
                            onAddGrade = { bimesterId, gradeNumber ->
                                addGrade(bimesterId, gradeNumber)
                            },
                            onDeleteGrade = { gradeId, gradeNumber ->
                                deleteGrade(gradeId, gradeNumber)
                            },
                            onAddSection = { gradeId, letter ->
                                addSection(gradeId, letter)
                            },
                            onDeleteSection = { sectionId, sectionLetter ->
                                deleteSection(sectionId, sectionLetter)
                            }
                        )
                        rvBimesters.adapter = adapter
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<BimesterData>>, t: Throwable) {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addGrade(bimesterId: Int, gradeNumber: Int) {
        progressBar.visibility = ProgressBar.VISIBLE

        val request = CreateGradeRequest(number = gradeNumber)
        RetrofitClient.instance.createGrade(bimesterId, request)
            .enqueue(object : Callback<Grade> {
                override fun onResponse(call: Call<Grade>, response: Response<Grade>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MainActivity,
                            "Grado $gradeNumber° agregado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        expandedBimesterId = bimesterId
                        loadBimesters(keepExpanded = true)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@MainActivity,
                            errorBody ?: "Error al agregar grado $gradeNumber°",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Grade>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Error al agregar grado $gradeNumber°: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun deleteGrade(gradeId: Int, gradeNumber: Int) {
        progressBar.visibility = ProgressBar.VISIBLE

        RetrofitClient.instance.deleteGrade(gradeId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MainActivity,
                            "Grado $gradeNumber° eliminado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadBimesters(keepExpanded = true)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al eliminar grado $gradeNumber°",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Error al eliminar grado $gradeNumber°: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun addSection(gradeId: Int, letter: String?) {
        progressBar.visibility = ProgressBar.VISIBLE

        val request = CreateSectionRequest(letter = letter)
        RetrofitClient.instance.createSection(gradeId, request)
            .enqueue(object : Callback<Section> {
                override fun onResponse(call: Call<Section>, response: Response<Section>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        val section = response.body()
                        Toast.makeText(
                            this@MainActivity,
                            "Sección ${section?.letter ?: ""} agregada exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        expandedGradeId = gradeId
                        Log.d(TAG, "Sección agregada, manteniendo expandedGradeId: $expandedGradeId")
                        loadBimesters(keepExpanded = true)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@MainActivity,
                            errorBody ?: "Error al agregar sección",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Section>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Error al agregar sección: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun deleteSection(sectionId: Int, sectionLetter: String) {
        progressBar.visibility = ProgressBar.VISIBLE

        RetrofitClient.instance.deleteSection(sectionId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    progressBar.visibility = ProgressBar.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MainActivity,
                            "Sección $sectionLetter eliminada exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadBimesters(keepExpanded = true)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al eliminar sección $sectionLetter",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Error al eliminar sección $sectionLetter: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}