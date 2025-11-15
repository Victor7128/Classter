package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Student

class StudentAdapter(
    private val students: MutableList<Student>,
    private val onEditStudent: (Student) -> Unit,
    private val onDeleteStudent: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvStudentInitial: TextView? = view.findViewById(R.id.tvStudentInitial)
        val tvStudentNumber: TextView? = view.findViewById(R.id.tvStudentNumber)
        val btnEditStudent: ImageButton = view.findViewById(R.id.btnEditStudent)
        val btnDeleteStudent: ImageButton = view.findViewById(R.id.btnDeleteStudent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.tvStudentName.text = student.full_name

        // ✅ Configurar inicial del avatar
        holder.tvStudentInitial?.text = student.full_name.firstOrNull()?.uppercase() ?: "?"

        // ✅ Configurar número de orden
        holder.tvStudentNumber?.text = "#${position + 1}"

        holder.btnEditStudent.setOnClickListener {
            onEditStudent(student)
        }

        holder.btnDeleteStudent.setOnClickListener {
            onDeleteStudent(student)
        }
    }

    override fun getItemCount() = students.size

    // Métodos para actualizar la lista
    fun removeStudent(student: Student) {
        val position = students.indexOfFirst { it.id == student.id }
        if (position != -1) {
            students.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, students.size - position)
        }
    }

    fun addStudent(student: Student) {
        students.add(student)
        students.sortBy { it.full_name }
        val position = students.indexOfFirst { it.id == student.id }
        notifyItemInserted(position)
        notifyItemRangeChanged(position, students.size - position)
    }

    fun updateStudent(student: Student) {
        val position = students.indexOfFirst { it.id == student.id }
        if (position != -1) {
            students[position] = student
            notifyItemChanged(position)
        }
    }

    fun updateStudents(newStudents: List<Student>) {
        students.clear()
        students.addAll(newStudents)
        notifyDataSetChanged()
    }
}