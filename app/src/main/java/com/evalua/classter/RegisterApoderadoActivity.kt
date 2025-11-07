package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evalua.classter.models.ErrorResponse
import com.evalua.classter.models.RegisterApoderadoRequest
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterApoderadoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RegisterApoderadoActivity"
    }

    private lateinit var auth: FirebaseAuth

    private lateinit var etDNI: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var actvRelationship: AutoCompleteTextView
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
        setContentView(R.layout.activity_register_apoderado)

        // Initialize Firebase Auth
        auth = Firebase.auth

        initViews()
        setupRelationshipDropdown()
        setupListeners()
    }

    private fun initViews() {
        etDNI = findViewById(R.id.etDNI)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        actvRelationship = findViewById(R.id.actvRelationship)
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

    private fun setupRelationshipDropdown() {
        val relationships = arrayOf(
            "Padre",
            "Madre",
            "Tutor Legal",
            "Abuelo/a",
            "Tío/a",
            "Hermano/a Mayor",
            "Otro"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, relationships)
        actvRelationship.setAdapter(adapter)
        actvRelationship.setText("Padre", false)
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
                val response = ReniecClient.apiService.validateDNI(
                    token = ReniecClient.getToken(),
                    request = request
                )

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
        val dni = etDNI.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val relationship = actvRelationship.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val passwordConfirm = etPasswordConfirm.text.toString().trim()

        if (!validateInput(dni, fullName, phone, relationship, email, password, passwordConfirm)) {
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Enviar verificación de correo
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d(TAG, "Correo de verificación enviado")
                        } else {
                            Log.w(TAG, "Error enviando verificación: ${verificationTask.exception}")
                        }
                    }

                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null) {
                            registerUserInBackend(token, email, fullName, dni, phone, relationship)
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
                    Toast.makeText(this, "Error en registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateInput(
        dni: String,
        fullName: String,
        phone: String,
        relationship: String,
        email: String,
        password: String,
        passwordConfirm: String
    ): Boolean {
        if (dni.length != 8) {
            Toast.makeText(this, "Ingresa un DNI válido de 8 dígitos", Toast.LENGTH_SHORT).show()
            return false
        }

        if (reniecData == null) {
            Toast.makeText(this, "Debes validar tu DNI con RENIEC primero", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fullName.isEmpty()) {
            etFullName.error = "El nombre completo es requerido"
            return false
        }

        if (phone.isEmpty() || phone.length < 9) {
            etPhone.error = "Ingresa un teléfono válido (mínimo 9 dígitos)"
            return false
        }

        if (relationship.isEmpty()) {
            Toast.makeText(this, "Selecciona tu parentesco", Toast.LENGTH_SHORT).show()
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

    private fun registerUserInBackend(
        token: String,
        email: String,
        fullName: String,
        dni: String,
        phone: String,
        relationship: String
    ) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    showLoading(false)
                    Toast.makeText(this@RegisterApoderadoActivity, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Preparar request
                val request = RegisterApoderadoRequest(
                    dni = dni,
                    fullName = fullName,
                    phone = phone,
                    relationshipType = relationship,
                    email = email,
                    firebaseUid = user.uid,
                    occupation = null,
                    workplace = null
                )

                // Llamar al backend
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.registerApoderado(request)
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success) {
                            Log.d(TAG, "Apoderado registrado exitosamente")

                            Toast.makeText(
                                this@RegisterApoderadoActivity,
                                "✅ Cuenta creada exitosamente",
                                Toast.LENGTH_LONG
                            ).show()

                            // Redirigir al Dashboard del Apoderado
                            val intent = Intent(this@RegisterApoderadoActivity, DashboardApoderadoActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@RegisterApoderadoActivity,
                                "Error: ${body?.message ?: "Respuesta vacía del servidor"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorResponse = try {
                            com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        val errorMessage = errorResponse?.error ?: "Error en el registro (${response.code()})"
                        Toast.makeText(this@RegisterApoderadoActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error response: $errorBody")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "Error registrando apoderado", e)
                    Toast.makeText(
                        this@RegisterApoderadoActivity,
                        "Error de conexión: ${e.message}",
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