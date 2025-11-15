package com.evalua.classter.models

data class BimesterData(
    val id: Int,
    val name: String,
    val grades: List<Grade>
)

data class Grade(
    val id: Int,
    val bimester_id: Int,
    val number: Int,
    val sections: List<Section> = emptyList()
)

data class Section(
    val id: Int,
    val grade_id: Int,
    val letter: String
)

// ✅ UNIFICAR - Student principal (compatible con todos los endpoints)
data class Student(
    val id: Int,
    val section_id: Int? = null,
    val full_name: String,
    val email: String? = null,
    val user_id: Int? = null,
    val dni: String? = null
)