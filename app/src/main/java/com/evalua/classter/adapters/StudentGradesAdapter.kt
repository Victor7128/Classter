package com.evalua.classter.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.StudentGrade
import com.google.android.material.card.MaterialCardView

class StudentGradesAdapter(
    private var grades: List<StudentGrade>
) : RecyclerView.Adapter<StudentGradesAdapter.GradeViewHolder>() {

    class GradeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardGrade: MaterialCardView = view.findViewById(R.id.cardGrade)
        val tvSessionTitle: TextView = view.findViewById(R.id.tvSessionTitle)
        val tvCompetencyName: TextView = view.findViewById(R.id.tvCompetencyName)
        val tvGradeValue: TextView = view.findViewById(R.id.tvGradeValue)
        val tvObservation: TextView = view.findViewById(R.id.tvObservation)
        val tvBimester: TextView = view.findViewById(R.id.tvBimester)
        val tvUpdatedAt: TextView = view.findViewById(R.id.tvUpdatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_grade, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val grade = grades[position]

        holder.tvSessionTitle.text = grade.sessionTitle ?: "Sesión"
        holder.tvCompetencyName.text = grade.competencyName ?: "Competencia"
        holder.tvGradeValue.text = grade.value
        holder.tvBimester.text = "${grade.bimesterName} Bimestre - ${grade.gradeNumber}° ${grade.sectionLetter}"

        // Formatear fecha
        holder.tvUpdatedAt.text = "Actualizado: ${formatDate(grade.updatedAt)}"

        // Mostrar observación si existe
        if (!grade.observation.isNullOrEmpty()) {
            holder.tvObservation.visibility = View.VISIBLE
            holder.tvObservation.text = grade.observation
        } else {
            holder.tvObservation.visibility = View.GONE
        }

        // Colorear según calificación
        val (color, bgColor) = when (grade.value) {
            "AD" -> Pair(Color.parseColor("#1B5E20"), Color.parseColor("#E8F5E9"))
            "A" -> Pair(Color.parseColor("#2E7D32"), Color.parseColor("#F1F8E9"))
            "B" -> Pair(Color.parseColor("#F57C00"), Color.parseColor("#FFF3E0"))
            "C" -> Pair(Color.parseColor("#C62828"), Color.parseColor("#FFEBEE"))
            else -> Pair(Color.parseColor("#424242"), Color.parseColor("#F5F5F5"))
        }

        holder.tvGradeValue.setTextColor(color)
        holder.cardGrade.setCardBackgroundColor(bgColor)
    }

    override fun getItemCount() = grades.size

    fun updateGrades(newGrades: List<StudentGrade>) {
        grades = newGrades
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Asumiendo formato "yyyy-MM-dd HH:mm:ss"
            val parts = dateString.split(" ")
            if (parts.size >= 2) {
                val dateParts = parts[0].split("-")
                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${parts[1]}"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}

