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
import com.evalua.classter.LoginActivity
import com.evalua.classter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserProfileMenuDialog(context: Context) : Dialog(context) {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var llChangePassword: LinearLayout
    private lateinit var llLogout: LinearLayout
    private lateinit var dividerPassword: View

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
        llChangePassword = findViewById(R.id.llChangePassword)
        llLogout = findViewById(R.id.llLogout)
        dividerPassword = findViewById(R.id.dividerPassword)
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

        // ✅ CORREGIDO: Mostrar opción de cambiar contraseña para TODOS los roles
        // (alumnos, docentes y apoderados)
        if (role == "ALUMNO" || role == "DOCENTE" || role == "APODERADO") {
            llChangePassword.visibility = View.VISIBLE
            dividerPassword.visibility = View.VISIBLE
        } else {
            llChangePassword.visibility = View.GONE
            dividerPassword.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        llChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        llLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Cerrar el diálogo al hacer clic fuera de él
        setOnDismissListener {
            // Limpiar recursos si es necesario
        }
    }

    private fun showChangePasswordDialog() {
        val changePasswordDialog = ChangePasswordDialog(context)
        changePasswordDialog.show()
        dismiss()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(context)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Cerrar sesión en Firebase
        auth.signOut()

        // Limpiar SharedPreferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Redirigir al LoginActivity
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)

        dismiss()
    }
}