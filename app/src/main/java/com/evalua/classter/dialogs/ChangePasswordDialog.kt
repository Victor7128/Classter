package com.evalua.classter.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import com.evalua.classter.R  // ✅ AGREGAR ESTA IMPORTACIÓN
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChangePasswordDialog(context: Context) : Dialog(context) {

    private lateinit var auth: FirebaseAuth
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_change_password)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        auth = Firebase.auth
        initViews()
        setupListeners()
    }

    private fun initViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupListeners() {
        btnChangePassword.setOnClickListener {
            changePassword()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (currentPassword.isEmpty()) {
            etCurrentPassword.error = "Ingresa tu contraseña actual"
            return
        }

        if (newPassword.isEmpty()) {
            etNewPassword.error = "Ingresa la nueva contraseña"
            return
        }

        if (newPassword.length < 6) {
            etNewPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Confirma la nueva contraseña"
            return
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Las contraseñas no coinciden"
            return
        }

        if (newPassword == currentPassword) {
            etNewPassword.error = "La nueva contraseña debe ser diferente a la actual"
            return
        }

        // Reautenticar al usuario antes de cambiar la contraseña
        val user = auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        // Cambiar la contraseña
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(context, "Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show()
                                    dismiss()
                                } else {
                                    Toast.makeText(context, "Error al cambiar contraseña: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        etCurrentPassword.error = "Contraseña actual incorrecta"
                    }
                }
        } else {
            Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }
}