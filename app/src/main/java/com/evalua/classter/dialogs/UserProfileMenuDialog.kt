package com.evalua.classter.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.evalua.classter.LoginActivity
import com.evalua.classter.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserProfileMenuDialog(context: Context) : Dialog(context) {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    // ✅ NUEVAS VISTAS DEL DISEÑO MEJORADO
    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvCurrentTheme: TextView

    // ✅ CARDS DEL NUEVO DISEÑO
    private lateinit var cardChangeTheme: MaterialCardView
    private lateinit var cardChangePassword: MaterialCardView
    private lateinit var llChangeTheme: LinearLayout
    private lateinit var llChangePassword: LinearLayout
    private lateinit var llLogout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.user_profile_menu)

        // Hacer el fondo transparente
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        auth = Firebase.auth
        sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        initViews()
        setupUserInfo()
        setupListeners()
    }

    private fun initViews() {
        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme)

        // ✅ INICIALIZAR NUEVOS ELEMENTOS
        cardChangeTheme = findViewById(R.id.cardChangeTheme)
        cardChangePassword = findViewById(R.id.cardChangePassword)
        llChangeTheme = findViewById(R.id.llChangeTheme)
        llChangePassword = findViewById(R.id.llChangePassword)
        llLogout = findViewById(R.id.llLogout)
    }

    private fun setupUserInfo() {
        val fullName = sharedPreferences.getString("user_full_name", "Usuario")
        val email = sharedPreferences.getString("user_email", "email@ejemplo.com")
        val role = sharedPreferences.getString("user_role", "USUARIO")

        tvUserName.text = fullName ?: "Usuario"
        tvUserEmail.text = email ?: "email@ejemplo.com"

        // Mostrar el rol de forma amigable
        tvUserRole.text = when (role) {
            "DOCENTE" -> "DOCENTE"
            "ALUMNO" -> "ALUMNO"
            "APODERADO" -> "APODERADO"
            else -> "USUARIO"
        }

        // ✅ MOSTRAR TEMA ACTUAL
        updateCurrentThemeText()

        // ✅ MOSTRAR/OCULTAR OPCIÓN DE CAMBIAR CONTRASEÑA
        if (role == "ALUMNO" || role == "DOCENTE" || role == "APODERADO") {
            cardChangePassword.visibility = View.VISIBLE
        } else {
            cardChangePassword.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        // ✅ NUEVO: Listener para cambiar tema
        llChangeTheme.setOnClickListener {
            showThemeDialog()
        }

        // Listener para cambiar contraseña
        llChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Listener para cerrar sesión
        llLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    // ✅ NUEVA FUNCIÓN: Actualizar texto del tema actual
    private fun updateCurrentThemeText() {
        val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val themeMode = appPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        tvCurrentTheme.text = when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Modo claro"
            AppCompatDelegate.MODE_NIGHT_YES -> "Modo oscuro"
            else -> "Seguir sistema"
        }
    }

    // ✅ NUEVA FUNCIÓN: Diálogo para cambiar tema
    private fun showThemeDialog() {
        val themes = arrayOf("Modo claro", "Modo oscuro", "Seguir sistema")
        val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentTheme = when (appPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Seleccionar tema")
            .setIcon(R.drawable.ic_theme)
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                val mode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                // Guardar preferencia
                appPrefs.edit().putInt("theme_mode", mode).apply()

                // Aplicar tema inmediatamente
                AppCompatDelegate.setDefaultNightMode(mode)

                // Actualizar el texto mostrado
                updateCurrentThemeText()

                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val changePasswordDialog = ChangePasswordDialog(context)
        changePasswordDialog.show()
        dismiss()
    }

    private fun showLogoutConfirmation() {
        // ✅ USAR MaterialAlertDialogBuilder para consistencia
        MaterialAlertDialogBuilder(context)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Cerrar Sesión") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Cerrar sesión en Firebase
        auth.signOut()

        // Limpiar SharedPreferences de usuario
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // ✅ NO limpiar las preferencias del tema
        // Las preferencias de "app_prefs" se mantienen para conservar el tema seleccionado

        // Redirigir al LoginActivity
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)

        dismiss()
    }
}