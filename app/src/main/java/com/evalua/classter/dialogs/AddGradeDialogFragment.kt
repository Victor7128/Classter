package com.evalua.classter.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.evalua.classter.R
import com.evalua.classter.models.Grade
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class AddGradeDialogFragment(
    private val existingGrades: List<Grade>,
    private val onGradeAdded: (Int) -> Unit
) : DialogFragment() {

    private var selectedGradeNumber: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_grade, null)

        val tilGradeNumber = view.findViewById<TextInputLayout>(R.id.tilGradeNumber)
        val actvGradeNumber = view.findViewById<AutoCompleteTextView>(R.id.actvGradeNumber)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnAccept = view.findViewById<MaterialButton>(R.id.btnAccept)

        val existingGradeNumbers = existingGrades.map { it.number }

        val allGrades = (1..6).toList()
        val availableGrades = allGrades.filter { it !in existingGradeNumbers }

        if (availableGrades.isEmpty()) {
            Toast.makeText(
                context,
                "Todos los grados ya están creados para este bimestre",
                Toast.LENGTH_LONG
            ).show()
            dismiss()
            return builder.create()
        }

        val availableGradesFormatted = availableGrades.map { "$it°" }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            availableGradesFormatted
        )
        actvGradeNumber.setAdapter(adapter)

        actvGradeNumber.setOnItemClickListener { _, _, position, _ ->
            selectedGradeNumber = availableGrades[position]
            btnAccept.isEnabled = true
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnAccept.setOnClickListener {
            selectedGradeNumber?.let { gradeNumber ->
                onGradeAdded(gradeNumber)
                dismiss()
            } ?: run {
                Toast.makeText(
                    context,
                    "Por favor seleccione un grado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.setView(view)
            .setTitle("Agregar Grado")

        return builder.create()
    }
}