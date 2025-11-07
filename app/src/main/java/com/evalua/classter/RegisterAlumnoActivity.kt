package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evalua.classter.models.ErrorResponse
import com.evalua.classter.models.RegisterAlumnoRequest
import com.evalua.classter.models.ReniecData
import com.evalua.classter.models.ReniecRequest
import com.evalua.classter.network.ReniecClient
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RegisterAlumnoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RegisterAlumnoActivity"
        private const val REQUEST_TIMEOUT = 30000L // 30 segundos
    }

    private lateinit var auth: FirebaseAuth

    private lateinit var etDNI: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirm: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var llValidating: LinearLayout
    private lateinit var llValidationStatus: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvReniecStatus: TextView

    private var reniecData: ReniecData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_alumno)

        // Initialize Firebase Auth
        auth = Firebase.auth

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etDNI = findViewById(R.id.etDNI)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        llValidating = findViewById(R.id.llValidating)
        llValidationStatus = findViewById(R.id.llValidationStatus)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvReniecStatus = findViewById(R.id.tvReniecStatus)
    }

    private fun setupListeners() {
        // Auto-validar DNI
        etDNI.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val dni = s.toString().trim()
                llValidationStatus.visibility = View.GONE
                etFullName.setText("")
                reniecData = null

                if (dni.length == 8) {
                    validateDNIWithReniec(dni)
                }
            }
        })

        btnRegister.setOnClickListener {
            registerWithEmail()
        }
    }

    private fun validateDNIWithReniec(dni: String) {
        llValidating.visibility = View.VISIBLE
        llValidationStatus.visibility = View.GONE
        etFullName.setText("")
        etDNI.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = ReniecRequest(dni = dni)
                val response = withContext(Dispatchers.IO) {
                    ReniecClient.apiService.validateDNI(
                        token = ReniecClient.getToken(),
                        request = request
                    )
                }

                withContext(Dispatchers.Main) {
                    llValidating.visibility = View.GONE
                    llValidationStatus.visibility = View.VISIBLE
                    etDNI.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val reniecResponse = response.body()!!

                        if (reniecResponse.success && reniecResponse.data != null) {
                            val data = reniecResponse.data
                            reniecData = data

                            etFullName.setText(data.nombreCompleto)
                            etFullName.isEnabled = false

                            ivStatusIcon.setImageResource(android.R.drawable.presence_online)
                            ivStatusIcon.setColorFilter(getColor(android.R.color.holo_green_dark))

                            tvReniecStatus.text = "✅ DNI validado correctamente"
                            tvReniecStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                            llValidationStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))

                            Log.d(TAG, "RENIEC Success: ${data.nombreCompleto}")
                        } else {
                            showError("DNI no encontrado en RENIEC")
                        }
                    } else {
                        val errorMessage = when (response.code()) {
                            401 -> "Token de API inválido"
                            404 -> "DNI no encontrado"
                            429 -> "Límite de consultas excedido"
                            else -> "Error ${response.code()}"
                        }
                        showError(errorMessage)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llValidating.visibility = View.GONE
                    llValidationStatus.visibility = View.VISIBLE
                    etDNI.isEnabled = true
                    showError("Error de conexión: ${e.message}")
                    Log.e(TAG, "RENIEC Exception", e)
                }
            }
        }
    }

    private fun showError(message: String) {
        ivStatusIcon.setImageResource(android.R.drawable.presence_busy)
        ivStatusIcon.setColorFilter(getColor(android.R.color.holo_red_dark))

        tvReniecStatus.text = "❌ $message"
        tvReniecStatus.setTextColor(getColor(android.R.color.holo_red_dark))

        llValidationStatus.setBackgroundColor(getColor(android.R.color.holo_red_light))
        etFullName.setText("")
    }

    private fun registerWithEmail() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val passwordConfirm = etPasswordConfirm.text.toString().trim()
        val dni = etDNI.text.toString().trim()

        if (!validateInput(email, password, passwordConfirm, dni)) {
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Enviar verificación de correo (pero no esperar a que termine)
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d(TAG, "Correo de verificación enviado")
                        } else {
                            Log.w(TAG, "Error enviando verificación: ${verificationTask.exception}")
                        }
                    }

                    // Obtener token y registrar en backend
                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null) {
                            registerUserInBackend(email, dni)
                        } else {
                            showLoading(false)
                            Toast.makeText(this, "Error: No se pudo obtener el token de autenticación", Toast.LENGTH_SHORT).show()
                        }
                    }?.addOnFailureListener { exception ->
                        showLoading(false)
                        Toast.makeText(this, "Error obteniendo token: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showLoading(false)
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already") == true ->
                            "El correo electrónico ya está en uso"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "Formato de correo electrónico inválido"
                        else -> "Error en registro: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateInput(email: String, password: String, passwordConfirm: String, dni: String): Boolean {
        if (dni.length != 8) {
            Toast.makeText(this, "Ingresa un DNI válido de 8 dígitos", Toast.LENGTH_SHORT).show()
            return false
        }

        if (reniecData == null) {
            Toast.makeText(this, "Debes validar tu DNI con RENIEC primero", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo electrónico inválido"
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            etPassword.error = "La contraseña debe tener mínimo 6 caracteres"
            return false
        }

        if (password != passwordConfirm) {
            etPasswordConfirm.error = "Las contraseñas no coinciden"
            return false
        }

        return true
    }

    private fun registerUserInBackend(email: String, dni: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Agregar timeout para evitar carga infinita
                withTimeout(REQUEST_TIMEOUT) {
                    val user = auth.currentUser
                    if (user == null) {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(this@RegisterAlumnoActivity, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
                        }
                        return@withTimeout
                    }

                    // Preparar request
                    val request = RegisterAlumnoRequest(
                        dni = dni,
                        fullName = reniecData?.nombreCompleto ?: etFullName.text.toString(),
                        email = email,
                        firebaseUid = user.uid,
                        nombres = reniecData?.nombres,
                        apellidoPaterno = reniecData?.apellidoPaterno,
                        apellidoMaterno = reniecData?.apellidoMaterno
                    )

                    Log.d(TAG, "Enviando registro al backend: $request")

                    // Llamar al backend
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.registerAlumno(request)
                    }

                    withContext(Dispatchers.Main) {
                        showLoading(false)

                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null && body.success) {
                                val userData = body.data

                                Log.d(TAG, "Alumno registrado en backend: ${userData?.email}, Rol: ${userData?.role}")

                                Toast.makeText(
                                    this@RegisterAlumnoActivity,
                                    "✅ Cuenta creada exitosamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Redirigir a DashboardAlumnosActivity
                                val intent = Intent(this@RegisterAlumnoActivity, DashboardAlumnosActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@RegisterAlumnoActivity,
                                    "Error en el servidor: ${body?.message ?: "Respuesta vacía"}",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e(TAG, "Respuesta del servidor sin éxito: $body")
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Error response: $errorBody, Code: ${response.code()}")

                            val errorMessage = when (response.code()) {
                                400 -> "Datos inválidos en la solicitud"
                                409 -> "El usuario ya existe en el sistema"
                                500 -> "Error interno del servidor"
                                else -> "Error en el registro (${response.code()})"
                            }

                            Toast.makeText(this@RegisterAlumnoActivity, errorMessage, Toast.LENGTH_LONG).show()

                            // Intentar eliminar el usuario de Firebase si falla el registro en backend
                            try {
                                user.delete()
                                Log.d(TAG, "Usuario de Firebase eliminado por fallo en backend")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error eliminando usuario de Firebase", e)
                            }
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@RegisterAlumnoActivity,
                        "Error: Tiempo de espera agotado. Verifica tu conexión.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "Timeout en registro de backend", e)
                }
            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@RegisterAlumnoActivity,
                        "Error: El servidor no responde. Intenta más tarde.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "Socket timeout en registro de backend", e)
                }
            } catch (e: UnknownHostException) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@RegisterAlumnoActivity,
                        "Error: No hay conexión a internet.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "No hay conexión a internet", e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "Error registrando alumno en backend", e)
                    Toast.makeText(
                        this@RegisterAlumnoActivity,
                        "Error de conexión con el servidor: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
    }
}