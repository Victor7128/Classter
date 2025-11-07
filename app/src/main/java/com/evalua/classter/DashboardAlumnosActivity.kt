package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.evalua.classter.dialogs.UserProfileMenuDialog

class DashboardAlumnosActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DashboardAlumnosActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_alumnos)

        // Verificar que el usuario esté logueado
        if (!isUserLoggedIn()) {
            redirectToLogin()
            return
        }

        // Cargar datos del usuario
        loadUserData()
        setupUserMenu()
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

        Log.d(TAG, "👤 Usuario cargado: $userName ($userEmail) - Rol: $userRole")
    }
}