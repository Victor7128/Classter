package com.evalua.classter.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.evalua.classter.R
import com.evalua.classter.models.Student
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class AddStudentDialogFragment(
    private val existingStudents: List<Student> = emptyList(),  // ✅ YA LO TIENES
    private val onStudentAdded: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_student, null)

        val etStudentName = view.findViewById<EditText>(R.id.etStudentName)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnAccept = view.findViewById<MaterialButton>(R.id.btnAccept)

        // ✅ NUEVO: Obtener TextInputLayout para mostrar errores
        val tilStudentName = view.findViewById<TextInputLayout>(R.id.tilStudentName)

        // ✅ NUEVO: Validación en tiempo real mientras escribe
        etStudentName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val name = s.toString().trim()

                if (name.isNotEmpty()) {
                    // Verificar si el nombre ya existe (sin distinguir mayúsculas/minúsculas)
                    val isDuplicate = existingStudents.any {
                        it.full_name.equals(name, ignoreCase = true)
                    }

                    if (isDuplicate) {
                        tilStudentName?.error = "⚠️ Este estudiante ya existe"
                        btnAccept.isEnabled = false  // ✅ Deshabilitar botón
                    } else {
                        tilStudentName?.error = null
                        btnAccept.isEnabled = true   // ✅ Habilitar botón
                    }
                } else {
                    tilStudentName?.error = null
                    btnAccept.isEnabled = true
                }
            }
        })

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnAccept.setOnClickListener {
            val name = etStudentName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(context, "Ingrese el nombre del estudiante", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ NUEVO: Validación final antes de enviar
            val isDuplicate = existingStudents.any {
                it.full_name.equals(name, ignoreCase = true)
            }

            if (isDuplicate) {
                Toast.makeText(
                    context,
                    "⚠️ El estudiante \"$name\" ya existe en la lista",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener  // ✅ NO cerrar el diálogo
            }

            // ✅ Si llegamos aquí, el nombre es válido
            onStudentAdded(name)
            dismiss()
        }

        builder.setView(view)
            .setTitle("Agregar Estudiante")

        return builder.create()
    }
}