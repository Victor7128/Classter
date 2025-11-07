package com.evalua.classter.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.evalua.classter.R
import com.evalua.classter.models.Section
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class AddSectionDialogFragment(
    private val existingSections: List<Section>,
    private val onSectionAdded: (String?) -> Unit
) : DialogFragment() {

    private var selectedLetter: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_section, null)
        val tilSectionLetter = view.findViewById<TextInputLayout>(R.id.tilSectionLetter)
        val actvSectionLetter = view.findViewById<AutoCompleteTextView>(R.id.actvSectionLetter)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnAccept = view.findViewById<MaterialButton>(R.id.btnAccept)
        val btnAuto = view.findViewById<MaterialButton>(R.id.btnAuto)

        val existingLetters = existingSections.map { it.letter }

        val allLetters = ('A'..'Z').map { it.toString() }
        val availableLetters = allLetters.filter { it !in existingLetters }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            availableLetters
        )
        actvSectionLetter.setAdapter(adapter)

        actvSectionLetter.setOnItemClickListener { _, _, position, _ ->
            selectedLetter = availableLetters[position]
            btnAccept.isEnabled = true
        }

        actvSectionLetter.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = actvSectionLetter.text.toString().trim().uppercase()
                if (text.isNotEmpty()) {
                    if (text in existingLetters) {
                        tilSectionLetter.error = "Esta letra ya existe"
                        btnAccept.isEnabled = false
                    } else if (text.length == 1 && text[0] in 'A'..'Z') {
                        selectedLetter = text
                        tilSectionLetter.error = null
                        btnAccept.isEnabled = true
                    } else {
                        tilSectionLetter.error = "Ingrese una letra válida (A-Z)"
                        btnAccept.isEnabled = false
                    }
                }
            }
        }

        btnAuto.setOnClickListener {
            if (availableLetters.isEmpty()) {
                Toast.makeText(context, "No hay letras disponibles", Toast.LENGTH_SHORT).show()
            } else {
                onSectionAdded(null)
                dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnAccept.setOnClickListener {
            selectedLetter?.let { letter ->
                onSectionAdded(letter)
                dismiss()
            } ?: run {
                Toast.makeText(
                    context,
                    "Por favor seleccione o ingrese una letra",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.setView(view)
            .setTitle("Agregar Sección")

        return builder.create()
    }
}