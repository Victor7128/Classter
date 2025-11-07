package com.evalua.classter

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evalua.classter.dialogs.GoogleSignUpDialogFragment
import com.evalua.classter.models.RegisterDocenteRequest
import com.evalua.classter.models.ReniecData
import com.evalua.classter.models.ReniecRequest
import com.evalua.classter.network.ReniecClient
import com.evalua.classter.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterDocenteActivity : AppCompatActivity(), GoogleSignUpDialogFragment.GoogleSignUpListener {
    companion object {
        private const val TAG = "RegisterDocenteActivity"
        private const val RC_SIGN_IN = 9005
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var etDNI: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var actvArea: AutoCompleteTextView
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirm: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogleSignUp: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var llValidating: LinearLayout
    private lateinit var llValidationStatus: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvReniecStatus: TextView
    private var reniecData: ReniecData? = null
    private var selectedAreaId: Int? = null
    private var selectedAreaName: String? = null
    private var pendingGoogleAccount: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_docente)

        auth = Firebase.auth

        // ✅ Configuración mejorada de Google Sign-In
        configureGoogleSignIn()

        initViews()
        loadAreasFromAPI()
        setupListeners()
        diagnoseGoogleSignIn()
        diagnoseDeveloperError()
    }

    private fun configureGoogleSignIn() {
        try {
            // 1. Verificar que google-services.json esté configurado
            val googleAppId = getString(R.string.google_app_id)
            if (googleAppId.isEmpty() || googleAppId == "debug_check") {
                throw IllegalStateException("google-services.json no configurado correctamente")
            }

            // 2. Obtener Web Client ID de forma segura
            val webClientId = getString(R.string.default_web_client_id)
            if (webClientId.isEmpty()) {
                throw IllegalStateException("default_web_client_id no encontrado en strings.xml")
            }

            Log.d(TAG, "🎯 Configurando Google Sign-In:")
            Log.d(TAG, "   - Package: $packageName")
            Log.d(TAG, "   - Google App ID: ${googleAppId.take(10)}...")
            Log.d(TAG, "   - Web Client ID: ${webClientId.take(10)}...")

            // 3. Configurar Google Sign-In Options
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()

            // 4. Crear cliente
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            Log.d(TAG, "✅ Google Sign-In configurado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR CRÍTICO en configuración", e)
            showCriticalConfigError(e)
        }
    }

    private fun showCriticalConfigError(exception: Exception) {
        val solutionSteps = """
        🔧 SOLUCIÓN PARA ERROR DE CONFIGURACIÓN:
        
        1. Verificar google-services.json:
           - Descarga NUEVO desde Firebase Console
           - Coloca en app/google-services.json
        
        2. Verificar strings.xml:
           - Agrega: <string name="default_web_client_id">TU_CLIENT_ID</string>
        
        3. Verificar build.gradle:
           - Aplica plugin: 'com.google.gms.google-services'
           - Package name debe coincidir
        
        4. Sincronizar y limpiar proyecto:
           - Build → Clean Project
           - Build → Rebuild Project
        
        Error: ${exception.message}
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("❌ Error de Configuración Firebase")
            .setMessage(solutionSteps)
            .setPositiveButton("Ver Firebase Console") { _, _ ->
                // Abrir Firebase console en el navegador
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://console.firebase.google.com/")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cerrar") { _, _ -> }
            .setCancelable(false)
            .show()
    }

    private fun handleGoogleSignInError(e: ApiException) {
        val errorMessage = when (e.statusCode) {
            CommonStatusCodes.NETWORK_ERROR ->
                "Error de red. Verifica tu conexión a internet."

            CommonStatusCodes.INTERNAL_ERROR ->
                "Error interno. Reintenta más tarde."

            CommonStatusCodes.DEVELOPER_ERROR ->
                "Error de configuración del desarrollador. Verifica:\n" +
                        "1. SHA-1 en Firebase Console\n" +
                        "2. Package name correcto\n" +
                        "3. Google Sign-In habilitado"

            CommonStatusCodes.INVALID_ACCOUNT ->
                "Cuenta de Google inválida o no autorizada."

            CommonStatusCodes.SIGN_IN_REQUIRED ->
                "Debes iniciar sesión en el dispositivo con una cuenta Google."

            12501 -> // SIGN_IN_CANCELLED
                "Inicio de sesión cancelado por el usuario."

            12502 -> // SIGN_IN_CURRENTLY_IN_PROGRESS
                "Ya hay un inicio de sesión en progreso."

            12500 -> // INTERNAL_ERROR específico de Google Sign-In
                "Error interno de Google Sign-In. Reinstala Google Play Services."

            10 -> // DEVELOPER_ERROR
                "Configuración incorrecta:\n" +
                        "• Verifica google-services.json\n" +
                        "• Revisa SHA-1 en Firebase\n" +
                        "• Confirma package name"

            else ->
                "Error desconocido (${e.statusCode}): ${e.message ?: "Sin detalles"}"
        }

        Log.e(TAG, "Código error: ${e.statusCode}, Mensaje: $errorMessage")

        showConfigErrorDialog(errorMessage)
        googleSignInClient.signOut()
    }

    private fun showConfigErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("❌ Error de Configuración")
            .setMessage(message)
            .setPositiveButton("Reintentar") { _, _ ->
                signUpWithGoogle()
            }
            .setNegativeButton("Ver Configuración") { _, _ ->
                // Podrías abrir una actividad de configuración
                checkFirebaseConfiguration()
            }
            .show()
    }

    private fun checkFirebaseConfiguration() {
        Log.d(TAG, "🔍 Verificando configuración Firebase:")
        Log.d(TAG, "   - Package: $packageName")
        Log.d(TAG, "   - Firebase UID: ${auth.currentUser?.uid ?: "No logueado"}")
        Log.d(TAG, "   - Google Client: ${getString(R.string.default_web_client_id).take(10)}...")

        Toast.makeText(this,
            "Verifica:\n1. google-services.json\n2. SHA-1 en Firebase\n3. Google Sign-In habilitado",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun initViews() {
        etDNI = findViewById(R.id.etDNI)
        etFullName = findViewById(R.id.etFullName)
        actvArea = findViewById(R.id.actvArea)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        progressBar = findViewById(R.id.progressBar)
        llValidating = findViewById(R.id.llValidating)
        llValidationStatus = findViewById(R.id.llValidationStatus)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvReniecStatus = findViewById(R.id.tvReniecStatus)

        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadAreasFromAPI() {
        lifecycleScope.launch {
            try {
                val areas = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getAreas()
                }

                withContext(Dispatchers.Main) {
                    if (areas.isNotEmpty()) {
                        Log.d(TAG, "✅ ${areas.size} áreas cargadas")

                        val areaNames = areas.map { it.nombre }.toTypedArray()

                        val adapter = ArrayAdapter(
                            this@RegisterDocenteActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            areaNames
                        )
                        actvArea.setAdapter(adapter)

                        actvArea.setOnItemClickListener { _, _, position, _ ->
                            selectedAreaId = areas[position].id
                            selectedAreaName = areas[position].nombre
                            Log.d(TAG, "✅ Área seleccionada: $selectedAreaName (ID: $selectedAreaId)")
                        }
                    } else {
                        Log.e(TAG, "❌ No hay áreas disponibles")
                        Toast.makeText(
                            this@RegisterDocenteActivity,
                            "No hay áreas disponibles. Contacta al administrador.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar áreas", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error cargando áreas: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        // Auto-validar DNI (para registro con email)
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

        // Google Sign-Up directo (el modal valida DNI + área)
        btnGoogleSignUp.setOnClickListener {
            signUpWithGoogle()
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

                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()!!.data!!
                        reniecData = data

                        etFullName.setText(data.nombreCompleto)
                        etFullName.isEnabled = false

                        ivStatusIcon.setImageResource(android.R.drawable.presence_online)
                        ivStatusIcon.setColorFilter(getColor(android.R.color.holo_green_dark))

                        tvReniecStatus.text = "✅ DNI validado correctamente"
                        tvReniecStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                        llValidationStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))

                        Log.d(TAG, "✅ DNI validado: ${data.nombreCompleto}")
                    } else {
                        showError("DNI no encontrado en RENIEC")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llValidating.visibility = View.GONE
                    llValidationStatus.visibility = View.VISIBLE
                    etDNI.isEnabled = true
                    showError("Error de conexión: ${e.message}")
                    Log.e(TAG, "❌ Error validando DNI", e)
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
        val area = actvArea.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val passwordConfirm = etPasswordConfirm.text.toString().trim()

        if (!validateInput(dni, fullName, area, email, password, passwordConfirm)) {
            return
        }

        showLoading(true)

        // ✅ Crear cuenta en Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        // ✅ CORREGIDO: Pasar areaId y areaName explícitamente
                        if (selectedAreaId != null && selectedAreaName != null) {
                            registerUserInBackend(
                                token = token,
                                email = email,
                                fullName = fullName,
                                dni = dni,
                                areaName = selectedAreaName!!,
                                areaId = selectedAreaId!!
                            )
                        } else {
                            showLoading(false)
                            Log.e(TAG, "❌ ERROR: areaId o areaName son null")
                            Toast.makeText(
                                this,
                                "Error: Área no seleccionada correctamente. Por favor selecciona un área de la lista.",
                                Toast.LENGTH_LONG
                            ).show()
                            user.delete()
                        }
                    }?.addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error obteniendo token", e)
                        Toast.makeText(this, "Error de autenticación. Intenta nuevamente.", Toast.LENGTH_SHORT).show()
                        user?.delete()
                    }
                } else {
                    showLoading(false)
                    Log.e(TAG, "❌ Error creando cuenta Firebase", task.exception)

                    // ✅ MANEJO MEJORADO DE ERRORES FIREBASE
                    val errorMessage = when {
                        task.exception?.message?.contains("email already in use", ignoreCase = true) == true -> {
                            clearFormFields()
                            "Este correo electrónico ya está registrado. Por favor inicia sesión o usa otro correo."
                        }
                        task.exception?.message?.contains("invalid email", ignoreCase = true) == true -> {
                            etEmail.error = "Correo inválido"
                            "El formato del correo electrónico no es válido."
                        }
                        task.exception?.message?.contains("weak password", ignoreCase = true) == true -> {
                            etPassword.error = "Contraseña muy débil"
                            "La contraseña es muy débil. Usa una contraseña más segura."
                        }
                        task.exception?.message?.contains("network", ignoreCase = true) == true -> {
                            "Error de conexión. Verifica tu internet e intenta nuevamente."
                        }
                        else -> {
                            "Error al crear la cuenta: ${task.exception?.message ?: "Error desconocido"}"
                        }
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateInput(
        dni: String,
        fullName: String,
        area: String,
        email: String,
        password: String,
        passwordConfirm: String
    ): Boolean {
        if (dni.length != 8) {
            Toast.makeText(this, "Ingresa un DNI válido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (reniecData == null) {
            Toast.makeText(this, "Debes validar tu DNI con RENIEC primero", Toast.LENGTH_SHORT).show()
            return false
        }

        // ✅ MEJORADO: Validación más estricta del área
        if (area.isEmpty() || selectedAreaId == null || selectedAreaName == null) {
            Toast.makeText(this, "Selecciona tu área de la lista desplegable", Toast.LENGTH_SHORT).show()
            actvArea.requestFocus()
            actvArea.showDropDown() // Mostrar la lista automáticamente
            return false
        }

        // ✅ Verificar que el área seleccionada coincida con el texto
        if (area != selectedAreaName) {
            Toast.makeText(this, "Por favor selecciona un área de la lista", Toast.LENGTH_SHORT).show()
            actvArea.requestFocus()
            actvArea.showDropDown()
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo inválido"
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            etPassword.error = "Mínimo 6 caracteres"
            return false
        }

        if (password != passwordConfirm) {
            etPasswordConfirm.error = "Las contraseñas no coinciden"
            return false
        }

        return true
    }

    private fun signUpWithGoogle() {
        // ✅ Cerrar sesión primero para forzar selector
        googleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "✅ Google Sign-In exitoso: ${account.email}")

            // Verificaciones adicionales
            if (account.idToken == null) {
                Log.e(TAG, "❌ ID Token es null")
                showConfigErrorDialog("Token de Google no disponible. Revisa la configuración OAuth.")
                return
            }

            if (account.email.isNullOrEmpty()) {
                Log.e(TAG, "❌ Email de Google es null o vacío")
                showConfigErrorDialog("No se pudo obtener el email de Google. Verifica los permisos.")
                return
            }

            // Todo OK, proceder
            pendingGoogleAccount = account
            showGoogleSignUpDialog(account)

        } catch (e: ApiException) {
            Log.e(TAG, "❌ Google Sign-In falló", e)
            handleGoogleSignInError(e)
        }
    }

    private fun showGoogleSignUpDialog(account: GoogleSignInAccount) {
        val dialog = GoogleSignUpDialogFragment.newInstance(
            email = account.email ?: "",
            displayName = account.displayName ?: ""
        )
        dialog.setListener(this)
        dialog.show(supportFragmentManager, "GoogleSignUpDialog")
    }

    override fun onGoogleSignUpComplete(dni: String, fullName: String, areaId: Int, areaName: String) {
        val account = pendingGoogleAccount
        if (account == null) {
            Toast.makeText(this, "Error: Cuenta de Google no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "📋 Datos validados del modal:")
        Log.d(TAG, "  - DNI: $dni")
        Log.d(TAG, "  - Nombre: $fullName")
        Log.d(TAG, "  - Área: $areaName")
        Log.d(TAG, "  - Area ID: $areaId ✅")

        // ✅ GUARDAR EL NOMBRE DEL ÁREA PARA USARLO DESPUÉS
        selectedAreaName = areaName

        // ✅ Validación adicional
        if (areaId <= 0) {
            Log.e(TAG, "❌ Area ID inválido: $areaId")
            Toast.makeText(this, "Error: ID de área inválido. Por favor reintenta.", Toast.LENGTH_LONG).show()
            return
        }

        // ✅ VALIDAR que el área exista ANTES de autenticar Firebase
        validateAreaBeforeFirebaseAuth(account, dni, fullName, areaId, areaName)
    }

    private fun validateAreaBeforeFirebaseAuth(
        account: GoogleSignInAccount,
        dni: String,
        fullName: String,
        areaId: Int,
        areaName: String
    ) {
        showLoading(true)

        Log.d(TAG, "🔍 INICIANDO VALIDACIÓN DE ÁREA:")
        Log.d(TAG, "   - Google Account: ${account.email}")
        Log.d(TAG, "   - DNI: $dni")
        Log.d(TAG, "   - Nombre: $fullName")
        Log.d(TAG, "   - Área ID: $areaId")
        Log.d(TAG, "   - Token no null: ${account.idToken != null}")

        lifecycleScope.launch {
            try {
                // Verificar que el área exista en el backend
                Log.d(TAG, "🔍 Verificando área con ID: $areaId")

                val area = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getArea(areaId)
                }

                Log.d(TAG, "✅ Área verificada: ${area.nombre}")

                withContext(Dispatchers.Main) {
                    // ✅ Área OK, proceder con Firebase
                    Log.d(TAG, "🚀 INICIANDO AUTENTICACIÓN FIREBASE CON GOOGLE")

                    // ✅ Verificación final del token
                    if (account.idToken == null) {
                        showLoading(false)
                        Toast.makeText(this@RegisterDocenteActivity, "Error: Token de Google no disponible", Toast.LENGTH_LONG).show()
                        return@withContext
                    }

                    firebaseAuthWithGoogle(
                        idToken = account.idToken!!,
                        dni = dni,
                        fullName = fullName,
                        areaId = areaId,
                        areaName = area.nombre,
                        email = account.email ?: ""
                    )
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "❌ Error validando área", e)

                    val errorMsg = when {
                        e.message?.contains("404") == true ->
                            "El área seleccionada no existe en el sistema.\n\nPor favor contacta al administrador."
                        e.message?.contains("timeout") == true ||
                                e.message?.contains("Unable to resolve host") == true ->
                            "Error de conexión. Verifica tu internet."
                        else ->
                            "Error validando área: ${e.message}"
                    }

                    androidx.appcompat.app.AlertDialog.Builder(this@RegisterDocenteActivity)
                        .setTitle("❌ Error de Validación")
                        .setMessage(errorMsg)
                        .setPositiveButton("Reintentar") { _, _ ->
                            // Volver a mostrar el modal
                            showGoogleSignUpDialog(account)
                        }
                        .setNegativeButton("Cancelar") { _, _ ->
                            pendingGoogleAccount = null
                            googleSignInClient.signOut()
                        }
                        .setNeutralButton("Reintentar Google") { _, _ ->
                            // Reintentar Google Sign-In directamente
                            signUpWithGoogle()
                        }
                        .show()
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun diagnoseGoogleSignIn() {
        try {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(this)
            Log.d(TAG, "Última cuenta: ${lastAccount?.email ?: "Ninguna"}")

            val webClientId = getString(R.string.default_web_client_id)
            Log.d(TAG, "Web Client ID: ${if (webClientId.startsWith(" ")) "❌ VACÍO" else "✅ CONFIGURADO"}")

        } catch (e: Exception) {
            Log.e(TAG, "Error en diagnóstico", e)
        }
    }

    override fun onGoogleSignUpCancelled() {
        pendingGoogleAccount = null
        googleSignInClient.signOut()
        Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show()
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        dni: String,
        fullName: String,
        areaId: Int,
        areaName: String,
        email: String
    ) {
        showLoading(true)

        // ✅ VERIFICAR que el token no sea null o vacío
        if (idToken.isEmpty()) {
            showLoading(false)
            Log.e(TAG, "❌ ID Token de Google es vacío")
            Toast.makeText(this, "Error: Token de Google inválido", Toast.LENGTH_LONG).show()
            googleSignInClient.signOut()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        showLoading(false)
                        Log.e(TAG, "❌ Usuario Firebase null después de autenticación exitosa")
                        Toast.makeText(this, "Error: Usuario no disponible", Toast.LENGTH_SHORT).show()
                        googleSignInClient.signOut()
                        return@addOnCompleteListener
                    }

                    Log.d(TAG, "✅ Firebase auth exitoso: ${user.uid}, Email: ${user.email}")

                    // ✅ Obtener token actualizado
                    user.getIdToken(true).addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null) {
                            // ✅ Registrar en backend
                            registerUserInBackend(token, email, fullName, dni, areaName, areaId)
                        } else {
                            showLoading(false)
                            Log.e(TAG, "❌ Token Firebase es null")
                            Toast.makeText(this, "Error de autenticación. Intenta nuevamente.", Toast.LENGTH_SHORT).show()
                            user.delete()
                            googleSignInClient.signOut()
                        }
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Error obteniendo token", e)
                        Toast.makeText(this, "Error de autenticación. Intenta nuevamente.", Toast.LENGTH_SHORT).show()
                        user.delete()
                        googleSignInClient.signOut()
                    }
                } else {
                    showLoading(false)
                    val exception = task.exception
                    Log.e(TAG, "❌ Firebase auth fallida", exception)

                    // ✅ MANEJO MEJORADO DE ERRORES GOOGLE AUTH
                    val errorMessage = when {
                        exception?.message?.contains("invalid credential", ignoreCase = true) == true ->
                            "Credenciales de Google inválidas. Intenta nuevamente."
                        exception?.message?.contains("already exists", ignoreCase = true) == true ||
                                exception?.message?.contains("already in use", ignoreCase = true) == true -> {
                            // Limpiar datos pendientes de Google
                            pendingGoogleAccount = null
                            googleSignInClient.signOut()
                            "Esta cuenta de Google ya está registrada. Por favor inicia sesión."
                        }
                        exception?.message?.contains("network", ignoreCase = true) == true ->
                            "Error de conexión. Verifica tu internet e intenta nuevamente."
                        else ->
                            "Error de autenticación con Google. Intenta nuevamente."
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    googleSignInClient.signOut()
                }
            }
    }

    private fun registerUserInBackend(
        token: String?,
        email: String,
        fullName: String,
        dni: String,
        areaName: String,
        areaId: Int? = selectedAreaId
    ) {
        showLoading(true)
        Log.d(TAG, "🔍 VERIFICANDO DATOS DE ÁREA:")
        Log.d(TAG, "   - areaName recibido: $areaName")
        Log.d(TAG, "   - areaId recibido: $areaId")
        Log.d(TAG, "   - selectedAreaName: $selectedAreaName")
        Log.d(TAG, "   - selectedAreaId: $selectedAreaId")

        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    showLoading(false)
                    Toast.makeText(this@RegisterDocenteActivity, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ VALIDACIÓN MEJORADA: Asegurar que areaId NO sea null
                if (areaId == null) {
                    showLoading(false)
                    Log.e(TAG, "❌ ERROR CRÍTICO: areaId es null")
                    Log.e(TAG, "   - Email: $email")
                    Log.e(TAG, "   - DNI: $dni")
                    Log.e(TAG, "   - Área seleccionada: $areaName")
                    Log.e(TAG, "   - selectedAreaId: $selectedAreaId")

                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error: Área no seleccionada correctamente. Por favor selecciona un área de la lista.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Eliminar cuenta de Firebase
                    user.delete()
                    return@launch
                }

                val request = RegisterDocenteRequest(
                    dni = dni,
                    fullName = fullName,
                    areaName = areaName,
                    areaId = areaId,
                    email = email,
                    firebaseUid = user.uid,
                    employeeCode = null,
                    specialization = null
                )

                Log.d(TAG, "📤 Enviando registro al backend:")
                Log.d(TAG, "  - DNI: $dni")
                Log.d(TAG, "  - Email: $email")
                Log.d(TAG, "  - Full Name: $fullName")
                Log.d(TAG, "  - Área: $areaName")
                Log.d(TAG, "  - Area ID: $areaId ✅")
                Log.d(TAG, "  - Firebase UID: ${user.uid}")
                Log.d(TAG, "  - Token no null: ${token != null}")

                // ✅ Log del JSON que se enviará
                val gson = com.google.gson.Gson()
                Log.d(TAG, "📦 JSON Request:\n${gson.toJson(request)}")

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.registerDocente(request)
                }

                Log.d(TAG, "📥 Respuesta del servidor: Code ${response.code()}")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d(TAG, "✅ Registro exitoso en backend")
                        val userResponse = response.body()!!.data!!

                        // ✅ GUARDAR DATOS DEL USUARIO Y REDIRIGIR DIRECTAMENTE
                        saveUserData(userResponse)

                        val message = "✅ Tu cuenta de docente ha sido creada exitosamente.\n\nSerás redirigido automáticamente..."

                        // ✅ MOSTRAR DIÁLOGO DE ÉXITO
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this@RegisterDocenteActivity)
                            .setTitle("✅ Cuenta Creada")
                            .setMessage(message)
                            .setPositiveButton("Continuar") { _, _ ->
                                redirectToMainActivity(userResponse)
                            }
                            .setCancelable(false)
                            .create()

                        dialog.show()

                        // ✅ REDIRECCIÓN AUTOMÁTICA después de 3 segundos
                        lifecycleScope.launch {
                            delay(3000)
                            withContext(Dispatchers.Main) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                redirectToMainActivity(userResponse)
                            }
                        }

                    } else {
                        // ... (el resto del manejo de errores permanece igual)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "❌ Error de conexión/excepción", e)

                    // ❌ SI HAY EXCEPCIÓN, ELIMINAR CUENTA DE FIREBASE
                    val user = auth.currentUser
                    user?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d(TAG, "🗑️ Cuenta de Firebase eliminada tras excepción")
                        }
                    }

                    Toast.makeText(
                        this@RegisterDocenteActivity,
                        "Error de conexión: ${e.message}\n\nIntenta nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun clearFormFields() {
        runOnUiThread {
            // Limpiar campos de email y contraseña
            etEmail.text?.clear()
            etPassword.text?.clear()
            etPasswordConfirm.text?.clear()

            // Limpiar errores visuales
            etEmail.error = null
            etPassword.error = null
            etPasswordConfirm.error = null

            // Enfocar en el campo de email para facilitar corrección
            etEmail.requestFocus()

            Log.d(TAG, "🧹 Campos del formulario limpiados")
        }
    }

    private fun redirectToMainActivity(userResponse: com.evalua.classter.models.UserResponse) {
        Log.d(TAG, "🚀 Redirigiendo a MainActivity...")

        val intent = Intent(this@RegisterDocenteActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToLogin() {
        val intent = Intent(this@RegisterDocenteActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun saveUserData(userResponse: com.evalua.classter.models.UserResponse) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("user_id", userResponse.id)
        editor.putString("user_email", userResponse.email)
        editor.putString("user_role", userResponse.role)
        editor.putString("user_status", userResponse.status)

        // Guardar datos del perfil
        val profileData = userResponse.profileData
        editor.putString("user_full_name", profileData["full_name"] as? String)
        editor.putInt("user_area_id", (profileData["area_id"] as? Double)?.toInt() ?: 0)
        editor.putString("user_employee_code", profileData["employee_code"] as? String)
        editor.putString("user_dni", profileData["dni"] as? String)

        // ✅ CORREGIDO: Guardar también el nombre del área
        // Si viene en el profileData, usarlo, sino usar el que tenemos localmente
        val areaName = profileData["area_nombre"] as? String ?: selectedAreaName
        editor.putString("user_area_name", areaName ?: "")

        editor.apply()

        Log.d(TAG, "💾 Datos del usuario guardados en SharedPreferences - ID: ${userResponse.id}, Área: $areaName")
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        btnGoogleSignUp.isEnabled = !show
    }

    private fun diagnoseDeveloperError() {
        Log.d(TAG, "🔍 DIAGNÓSTICO ERROR DEVELOPER (10):")

        // 1. Verificar google-services.json
        try {
            val resources = resources
            val resourceId = resources.getIdentifier("google_app_id", "string", packageName)
            val appId = getString(resourceId)
            Log.d(TAG, "✅ Google App ID: ${appId.take(10)}...")
        } catch (e: Exception) {
            Log.e(TAG, "❌ google-services.json NO configurado correctamente")
        }

        // 2. Verificar Web Client ID
        val webClientId = getString(R.string.default_web_client_id)
        Log.d(TAG, "🔑 Web Client ID: ${if (webClientId.isNotEmpty()) "✅ Presente" else "❌ FALTANTE"}")
        if (webClientId.isNotEmpty()) {
            Log.d(TAG, "   - Client ID empieza con: ${webClientId.take(20)}...")
        }

        // 3. Verificar package name
        Log.d(TAG, "📦 Package Name: $packageName")

        // 4. Verificar Google Play Services
        try {
            val version = packageManager.getPackageInfo("com.google.android.gms", 0).versionName
            Log.d(TAG, "🎮 Google Play Services: ✅ v$version")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Play Services no instalado")
        }
    }
}