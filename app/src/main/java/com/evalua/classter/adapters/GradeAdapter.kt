package com.evalua.classter.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.dialogs.AddSectionDialogFragment
import com.evalua.classter.models.Grade

class GradeAdapter(
    private val grades: List<Grade>,
    private val bimesterName: String = "",
    private var expandedGradeId: Int?,
    private val onGradeExpanded: (Int, Boolean) -> Unit,
    private val onDeleteGrade: (Int, Int) -> Unit,
    private val onAddSection: (Int, String?) -> Unit,
    private val onDeleteSection: (Int, String) -> Unit
) : RecyclerView.Adapter<GradeAdapter.GradeViewHolder>() {

    companion object {
        private const val TAG = "GradeAdapter"
    }

    inner class GradeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivExpandIcon: ImageView = view.findViewById(R.id.ivGradeExpandIcon)
        val tvGrade: TextView = view.findViewById(R.id.tvGradeNumber)
        val btnDeleteGrade: ImageButton = view.findViewById(R.id.btnDeleteGrade)
        val tvGradeLabel: TextView = view.findViewById(R.id.tvGradeLabel)
        val rvSections: RecyclerView = view.findViewById(R.id.recyclerViewSections)
        val layoutSections: LinearLayout = view.findViewById(R.id.layoutSectionsContainer)
        val btnAddSection: Button = view.findViewById(R.id.btnAddSection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val grade = grades[position]

        // ✅ Actualizar el badge del número
        holder.tvGrade.text = "${grade.number}°"

        // ✅ Actualizar el nombre del grado dinámicamente
        holder.tvGradeLabel.text = when (grade.number) {
            1 -> "Primer Grado"
            2 -> "Segundo Grado"
            3 -> "Tercer Grado"
            4 -> "Cuarto Grado"
            5 -> "Quinto Grado"
            6 -> "Sexto Grado"
            else -> "${grade.number}° Grado"
        }

        Log.d(TAG, "Binding grade ${grade.id}, expandedGradeId: $expandedGradeId")

        val sectionAdapter = SectionAdapter(
            sections = grade.sections,
            gradeNumber = grade.number,
            bimesterName = bimesterName,
            onDeleteSection = { sectionId, sectionLetter ->
                onDeleteSection(sectionId, sectionLetter)
            }
        )
        holder.rvSections.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvSections.adapter = sectionAdapter

        val isExpanded = grade.id == expandedGradeId
        Log.d(TAG, "Grade ${grade.id} isExpanded: $isExpanded")

        updateGradeUI(holder, isExpanded)

        val headerLayout = holder.itemView.findViewById<View>(R.id.layoutGradeHeader)
        headerLayout.setOnClickListener {
            val currentlyExpanded = grade.id == expandedGradeId
            val newState = !currentlyExpanded

            Log.d(TAG, "Grade ${grade.id} clicked, changing from $currentlyExpanded to $newState")

            val oldExpandedId = expandedGradeId
            expandedGradeId = if (newState) grade.id else null

            updateGradeUI(holder, newState)

            if (oldExpandedId != null && oldExpandedId != grade.id) {
                val oldPosition = grades.indexOfFirst { it.id == oldExpandedId }
                if (oldPosition != -1) {
                    notifyItemChanged(oldPosition)
                }
            }
            onGradeExpanded(grade.id, newState)
        }

        holder.btnDeleteGrade.setOnClickListener {
            val sectionsCount = grade.sections.size
            val message = if (sectionsCount > 0) {
                "¿Está seguro que desea eliminar el ${grade.number}°?\n\n" +
                        "Se eliminarán también ${sectionsCount} sección(es) con todos sus estudiantes y calificaciones."
            } else {
                "¿Está seguro que desea eliminar el ${grade.number}°?"
            }

            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Confirmar eliminación")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Eliminar") { _, _ ->
                    onDeleteGrade(grade.id, grade.number)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        holder.btnAddSection.setOnClickListener {
            val activity = holder.itemView.context as? AppCompatActivity
            activity?.let {
                val dialog = AddSectionDialogFragment(
                    existingSections = grade.sections
                ) { letter ->
                    onAddSection(grade.id, letter)
                }
                dialog.show(it.supportFragmentManager, "AddSectionDialog")
            }
        }
    }

    private fun updateGradeUI(holder: GradeViewHolder, isExpanded: Boolean) {
        holder.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpandIcon.rotation = if (isExpanded) 180f else 0f
    }

    override fun getItemCount() = grades.size
}