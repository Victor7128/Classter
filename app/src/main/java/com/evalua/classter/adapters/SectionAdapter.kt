package com.evalua.classter.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.SectionDetailActivity
import com.evalua.classter.models.Section

class SectionAdapter(
    private val sections: List<Section>,
    private val gradeNumber: Int,
    private val bimesterName: String = "",
    private val onDeleteSection: (Int, String) -> Unit
) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

    companion object {
        private const val TAG = "SectionAdapter"
    }

    inner class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSection: TextView = view.findViewById(R.id.tvSectionLetter)
        val btnDeleteSection: ImageButton = view.findViewById(R.id.btnDeleteSection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.tvSection.text = "Sección ${section.letter}"

        Log.d(TAG, "Binding section: ${section.letter}, ID: ${section.id}")

        // Click en la sección para ver el detalle (SectionDetailActivity)
        holder.itemView.setOnClickListener {
            Log.d(TAG, "=== Click detectado en sección ===")
            Log.d(TAG, "Section ID: ${section.id}")
            Log.d(TAG, "Section Letter: ${section.letter}")
            Log.d(TAG, "Grade ID: ${section.grade_id}")
            Log.d(TAG, "Grade Number: $gradeNumber")
            Log.d(TAG, "Bimester Name: $bimesterName")

            val context = holder.itemView.context

            try {
                // Navegar a SectionDetailActivity (no a StudentsActivity)
                val intent = Intent(context, SectionDetailActivity::class.java).apply {
                    putExtra("section_id", section.id)
                    putExtra("section_letter", section.letter)
                    putExtra("grade_id", section.grade_id)
                    putExtra("grade_number", gradeNumber)
                    putExtra("bimester_name", bimesterName)
                }

                Log.d(TAG, "Abriendo SectionDetailActivity")
                context.startActivity(intent)
                Log.d(TAG, "startActivity llamado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al abrir SectionDetailActivity", e)
            }
        }

        // Click en el TextView también
        holder.tvSection.setOnClickListener {
            Log.d(TAG, "Click en TextView de sección ${section.letter}")
            holder.itemView.performClick()
        }

        // Botón eliminar con confirmación
        holder.btnDeleteSection.setOnClickListener {
            Log.d(TAG, "Click en eliminar sección ${section.letter}")

            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro que desea eliminar la sección ${section.letter}?\n\nSe eliminarán todos los estudiantes y calificaciones asociadas.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Eliminar") { _, _ ->
                    onDeleteSection(section.id, section.letter)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun getItemCount() = sections.size
}