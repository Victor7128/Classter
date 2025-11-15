package com.evalua.classter

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evalua.classter.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                signInWithEmail(email, password)
            }
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, SelectRegisterTypeActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo inválido"
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            return false
        }

        if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        return true
    }

    private fun signInWithEmail(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser

                    // Obtener token para backend
                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        Log.d(TAG, "Firebase token: $token")

                        // Verificar rol en backend y redirigir
                        verifyUserAndRedirect(token)
                    }?.addOnFailureListener { e ->
                        Log.e(TAG, "Error obteniendo token", e)
                        Toast.makeText(
                            this,
                            "Error de autenticación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    val errorMessage = when {
                        task.exception?.message?.contains("invalid credential") == true ->
                            "Credenciales incorrectas"
                        task.exception?.message?.contains("user not found") == true ->
                            "Usuario no encontrado"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "Formato de correo inválido"
                        else -> "Error de autenticación: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        // FORZAR SELECCIÓN DE CUENTA cada vez
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Una vez cerrada sesión, iniciar el flujo de sign-in
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Error en inicio de sesión con Google: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        Log.d(TAG, "✅ Google Sign-In exitoso, verificando con backend...")
                        verifyUserAndRedirect(token)
                    }?.addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error obteniendo token", e)
                        Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showLoading(false)
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    // Mensaje más específico para Google Sign-In
                    val errorMessage = when {
                        task.exception?.message?.contains("network error") == true ->
                            "Error de red. Verifica tu conexión."
                        task.exception?.message?.contains("invalid credential") == true ->
                            "Credenciales inválidas. Intenta nuevamente."
                        else -> "Error en inicio de sesión con Google: ${task.exception?.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verifyUserAndRedirect(token: String?) {
        Log.d(TAG, "🔄 INICIANDO verifyUserAndRedirect - Token: ${token?.take(10)}...")

        if (token == null) {
            Log.e(TAG, "❌ Token es null")
            Toast.makeText(this, "Error: No se pudo obtener el token", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                Log.d(TAG, "👤 Usuario Firebase: ${user?.uid}")

                if (user == null) {
                    Log.e(TAG, "❌ Usuario Firebase es null")
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(
                            this@LoginActivity,
                            "Error: Usuario no autenticado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                Log.d(TAG, "🌐 Llamando a /api/auth/me con UID: ${user.uid}")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getCurrentUser(user.uid)
                }

                Log.d(TAG, "📡 Respuesta del servidor - Código: ${response.code()}")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d(TAG, "✅ Respuesta exitosa - Body: $body")

                        if (body != null && body.success && body.data != null) {
                            val userResponse = body.data
                            Log.d(TAG, "🎯 Usuario verificado - ID: ${userResponse.id}, Rol: ${userResponse.role}")

                            // Verificar que la cuenta esté activa
                            if (userResponse.status == "ACTIVE") {
                                Log.d(TAG, "✅ Cuenta ACTIVA - Guardando datos...")

                                // ✅ ESPERAR a que termine de guardar los datos antes de redirigir
                                lifecycleScope.launch {
                                    val guardadoCompleto = saveUserDataAndWait(userResponse)
                                    if (guardadoCompleto) {
                                        Log.d(TAG, "✅ Todos los datos guardados - Redirigiendo...")
                                        redirectUser(userResponse.role)
                                    } else {
                                        Log.e(TAG, "❌ Error guardando datos completos")
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Error guardando datos del usuario",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Log.w(TAG, "⚠️ Cuenta NO activa - Status: ${userResponse.status}")
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Tu cuenta está ${userResponse.status?.lowercase()}. Contacta al administrador.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                                googleSignInClient.signOut()
                            }
                        } else {
                            Log.e(TAG, "❌ Body inválido - Success: ${body?.success}, Data: ${body?.data}")
                            Toast.makeText(
                                this@LoginActivity,
                                "Error: ${body?.message ?: "Respuesta inválida del servidor"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.e(TAG, "❌ Error del servidor - Código: ${response.code()}")
                        when (response.code()) {
                            401 -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "No autorizado. Por favor inicia sesión nuevamente.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                                googleSignInClient.signOut()
                            }
                            404 -> {
                                // Usuario no encontrado en el backend pero sí en Firebase
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Usuario no encontrado en el sistema. Intenta registrarte nuevamente.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                                googleSignInClient.signOut()
                            }
                            else -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "Error del servidor: ${response.code()} - $errorBody")
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Error del servidor (${response.code()}). Intenta más tarde.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Excepción en verifyUserAndRedirect", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ✅ NUEVO MÉTODO: Guardar datos y esperar a que termine
    private suspend fun saveUserDataAndWait(userResponse: com.evalua.classter.models.UserResponse): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                editor.putInt("user_id", userResponse.id)
                editor.putString("user_email", userResponse.email)
                editor.putString("user_role", userResponse.role)
                editor.putString("user_status", userResponse.status)
                editor.putString("user_firebase_uid", auth.currentUser?.uid)

                // Guardar datos del perfil de manera genérica
                val profileData = userResponse.profileData
                Log.d(TAG, "📊 ProfileData recibido: $profileData")

                editor.putString("user_full_name", profileData["full_name"] as? String)
                editor.putString("user_dni", profileData["dni"] as? String)

                // Para docentes - verificar qué datos llegan
                val areaId = profileData["area_id"] as? Double
                var areaNombre = profileData["area_nombre"] as? String
                val areaIdInt = areaId?.toInt() ?: 0

                Log.d(TAG, "🏫 Datos del área - ID: $areaIdInt, Nombre: $areaNombre")

                // ✅ MEJORADO: Si no viene el nombre del área, obtenerlo del servidor
                if (areaIdInt > 0 && areaNombre.isNullOrEmpty()) {
                    Log.d(TAG, "🔄 Obteniendo nombre del área desde el servidor...")
                    areaNombre = obtenerNombreAreaYEsperar(areaIdInt)
                }

                editor.putInt("user_area_id", areaIdInt)
                editor.putString("user_area_name", areaNombre ?: "")
                Log.d(TAG, "💾 Guardando área: ID $areaIdInt, Nombre: ${areaNombre ?: "NO_OBTENIDO"}")

                // Guardar el resto de datos
                editor.putString("user_employee_code", profileData["employee_code"] as? String)
                editor.putString("user_nombres", profileData["nombres"] as? String)
                editor.putString("user_apellido_paterno", profileData["apellido_paterno"] as? String)
                editor.putString("user_apellido_materno", profileData["apellido_materno"] as? String)
                editor.putString("user_phone", profileData["phone"] as? String)
                editor.putString("user_relationship_type", profileData["relationship_type"] as? String)

                // ✅ APLICAR los cambios y esperar a que termine
                editor.apply()

                // Verificar que se guardó correctamente
                val savedAreaId = sharedPreferences.getInt("user_area_id", -1)
                val savedAreaName = sharedPreferences.getString("user_area_name", "NO_GUARDADO")
                Log.d(TAG, "💾 Verificación - Área ID: $savedAreaId, Área Nombre: $savedAreaName")

                true // Indicar que se guardó correctamente

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en saveUserDataAndWait", e)
                false // Indicar error
            }
        }
    }

    // ✅ NUEVO MÉTODO: Obtener nombre del área y esperar
    private suspend fun obtenerNombreAreaYEsperar(areaId: Int): String? {
        return try {
            Log.d(TAG, "🌐 Solicitando área con ID: $areaId")

            // ✅ TIPO EXPLÍCITO
            val area: com.evalua.classter.models.Area = withContext(Dispatchers.IO) {
                RetrofitClient.curriculumApiService.getArea(areaId)
            }

            val areaNombre: String = area.nombre
            Log.d(TAG, "✅ Área obtenida: $areaNombre")
            areaNombre

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo el área", e)
            null
        }
    }

    // ✅ MÉTODO SEPARADO para redirección
    private fun redirectUser(role: String) {
        Log.d(TAG, "🔄 Redirigiendo según rol: $role")

        when (role) {
            "DOCENTE" -> {
                Log.d(TAG, "🎯 Redirigiendo a MainActivity (Docente)")
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            "ALUMNO" -> {
                Log.d(TAG, "🎯 Redirigiendo a DashboardAlumnosActivity (Alumno)")
                val intent = Intent(this@LoginActivity, DashboardAlumnosActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            "APODERADO" -> {
                Log.d(TAG, "🎯 Redirigiendo a DashboardApoderadoActivity (Apoderado)")
                val intent = Intent(this@LoginActivity, DashboardApoderadoActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            else -> {
                Log.e(TAG, "❌ Rol no reconocido: $role")
                Toast.makeText(
                    this@LoginActivity,
                    "Rol no reconocido: $role. Contacta al administrador.",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
                googleSignInClient.signOut()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnGoogleSignIn.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()

        // Verificar usuario con el backend antes de redirigir
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado, verificando con backend...")

            currentUser.getIdToken(true).addOnSuccessListener { result ->
                val token = result.token
                verifyUserAndRedirect(token)
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo token en onStart", e)
                // Si hay error obteniendo el token, cerrar sesión
                auth.signOut()
                googleSignInClient.signOut()
            }
        }
    }
}