package com.evalua.classter.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.evalua.classter.R
import com.evalua.classter.RegisterDocenteActivity
import com.evalua.classter.models.ReniecData
import com.evalua.classter.models.ReniecRequest
import com.evalua.classter.network.ReniecClient
import com.evalua.classter.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleSignUpDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "GoogleSignUpDialog"
        private const val ARG_EMAIL = "email"
        private const val ARG_NAME = "name"

        fun newInstance(email: String, displayName: String): GoogleSignUpDialogFragment {
            return GoogleSignUpDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                    putString(ARG_NAME, displayName)
                }
            }
        }
    }

    interface GoogleSignUpListener {
        fun onGoogleSignUpComplete(dni: String, fullName: String, areaId: Int, areaName: String)
        fun onGoogleSignUpCancelled()
    }

    private lateinit var etDNI: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var actvArea: AutoCompleteTextView
    private lateinit var btnComplete: MaterialButton
    private lateinit var llValidating: LinearLayout
    private lateinit var llValidationStatus: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvReniecStatus: TextView

    private var reniecData: ReniecData? = null
    private var selectedAreaId: Int? = null
    private var areasMap: Map<String, Int> = emptyMap()

    private var listener: GoogleSignUpListener? = null

    private val userEmail: String by lazy { arguments?.getString(ARG_EMAIL) ?: "" }
    private val userName: String by lazy { arguments?.getString(ARG_NAME) ?: "" }

    fun setListener(listener: RegisterDocenteActivity) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_google_signup, null)

        initViews(view)
        loadAreas()
        setupListeners()

        // Pre-llenar nombre desde Google
        etFullName.setText(userName)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
    }

    private fun initViews(view: View) {
        etDNI = view.findViewById(R.id.etDNI)
        etFullName = view.findViewById(R.id.etFullName)
        actvArea = view.findViewById(R.id.actvArea)
        btnComplete = view.findViewById(R.id.btnComplete)
        llValidating = view.findViewById(R.id.llValidating)
        llValidationStatus = view.findViewById(R.id.llValidationStatus)
        ivStatusIcon = view.findViewById(R.id.ivStatusIcon)
        tvReniecStatus = view.findViewById(R.id.tvReniecStatus)

        // Botón cancelar
        view.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            listener?.onGoogleSignUpCancelled()
            dismiss()
        }
    }

    private fun loadAreas() {
        lifecycleScope.launch {
            try {
                val areas = withContext(Dispatchers.IO) {
                    RetrofitClient.curriculumApiService.getAreas()
                }

                withContext(Dispatchers.Main) {
                    if (areas.isNotEmpty()) {
                        val areaNames = areas.map { it.nombre }.toTypedArray()
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            areaNames
                        )
                        actvArea.setAdapter(adapter)

                        actvArea.setOnItemClickListener { _, _, position, _ ->
                            selectedAreaId = areas[position].id
                            Log.d(TAG, "✅ Área seleccionada: ${areas[position].nombre} (ID: ${areas[position].id})")
                            checkFormCompletion()
                        }
                    } else {
                        Log.e(TAG, "❌ No hay áreas disponibles")
                        Toast.makeText(context, "No hay áreas disponibles. Contacta al administrador.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando áreas", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error cargando áreas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        // ✅ VALIDACIÓN AUTOMÁTICA del DNI
        etDNI.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val dni = s.toString().trim()
                llValidationStatus.visibility = View.GONE
                etFullName.setText("")
                reniecData = null
                btnComplete.isEnabled = false

                if (dni.length == 8) {
                    validateDNI(dni)
                }
            }
        })

        btnComplete.setOnClickListener {
            completeSignUp()
        }
    }

    private fun validateDNI(dni: String) {
        llValidating.visibility = View.VISIBLE
        llValidationStatus.visibility = View.GONE
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
                        ivStatusIcon.setColorFilter(requireContext().getColor(android.R.color.holo_green_dark))
                        tvReniecStatus.text = "✅ DNI validado correctamente"
                        tvReniecStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))

                        checkFormCompletion()
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
                }
            }
        }
    }

    private fun showError(message: String) {
        ivStatusIcon.setImageResource(android.R.drawable.presence_busy)
        ivStatusIcon.setColorFilter(requireContext().getColor(android.R.color.holo_red_dark))
        tvReniecStatus.text = "❌ $message"
        tvReniecStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
        btnComplete.isEnabled = false
    }

    /**
     * ✅ Verifica si el formulario está completo para habilitar el botón
     */
    private fun checkFormCompletion() {
        val isDNIValid = reniecData != null
        val isAreaSelected = selectedAreaId != null

        btnComplete.isEnabled = isDNIValid && isAreaSelected

        if (btnComplete.isEnabled) {
            btnComplete.text = "✅ Completar Registro"
        } else {
            btnComplete.text = "Completar Registro"
        }
    }

    private fun completeSignUp() {
        val dni = etDNI.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val areaName = actvArea.text.toString().trim()

        if (dni.isEmpty() || reniecData == null) {
            Toast.makeText(context, "Valida tu DNI primero", Toast.LENGTH_SHORT).show()
            return
        }

        if (areaName.isEmpty() || selectedAreaId == null) {
            Toast.makeText(context, "Selecciona tu área", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "✅ Completando registro:")
        Log.d(TAG, "  - DNI: $dni")
        Log.d(TAG, "  - Nombre: $fullName")
        Log.d(TAG, "  - Área: $areaName (ID: $selectedAreaId)")

        listener?.onGoogleSignUpComplete(dni, fullName, selectedAreaId!!, areaName)
        dismiss()
    }
}