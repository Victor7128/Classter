package com.evalua.classter.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.evalua.classter.R
import com.evalua.classter.models.Student
import com.google.android.material.button.MaterialButton

class EditStudentDialogFragment(
    private val student: Student,
    private val onStudentUpdated: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_student, null)

        val etStudentName = view.findViewById<EditText>(R.id.etStudentName)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnAccept = view.findViewById<MaterialButton>(R.id.btnAccept)

        // Pre-llenar con el nombre actual
        etStudentName.setText(student.full_name)
        etStudentName.setSelection(student.full_name.length)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnAccept.setOnClickListener {
            val name = etStudentName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "Ingrese el nombre del estudiante", Toast.LENGTH_SHORT).show()
            } else {
                onStudentUpdated(name)
                dismiss()
            }
        }

        builder.setView(view)
            .setTitle("Editar Estudiante")

        return builder.create()
    }
}