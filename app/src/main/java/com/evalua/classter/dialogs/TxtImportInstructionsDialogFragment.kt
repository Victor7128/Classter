package com.evalua.classter.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.evalua.classter.R
import com.google.android.material.button.MaterialButton

class TxtImportInstructionsDialogFragment(
    private val onProceed: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_txt_instructions, null)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnProceed = view.findViewById<MaterialButton>(R.id.btnProceed)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnProceed.setOnClickListener {
            dismiss()
            onProceed()
        }

        builder.setView(view)

        return builder.create()
    }
}