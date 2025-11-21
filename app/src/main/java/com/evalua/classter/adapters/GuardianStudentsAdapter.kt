package com.evalua.classter.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.GuardianStudent
import com.google.android.material.card.MaterialCardView

class GuardianStudentsAdapter(
    private var students: List<GuardianStudent>
) : RecyclerView.Adapter<GuardianStudentsAdapter.StudentViewHolder>() {

    class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardStudent: MaterialCardView = view.findViewById(R.id.cardStudent)
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvGradeSection: TextView = view.findViewById(R.id.tvGradeSection)
        val tvBimester: TextView = view.findViewById(R.id.tvBimester)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guardian_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvStudentName.text = student.studentName
        holder.tvGradeSection.text = "${student.gradeNumber}° ${student.sectionLetter}"
        holder.tvBimester.text = "${student.bimesterName} Bimestre ${student.year}"

        holder.cardStudent.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Class.forName("com.evalua.classter.StudentDetailForGuardianActivity")).apply {
                putExtra("student_user_id", student.studentId)
                putExtra("student_name", student.studentName)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = students.size

    fun updateStudents(newStudents: List<GuardianStudent>) {
        students = newStudents
        notifyDataSetChanged()
    }
}

