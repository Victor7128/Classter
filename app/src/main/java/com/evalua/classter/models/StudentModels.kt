package com.evalua.classter.models

import com.google.gson.annotations.SerializedName

// ============================================
// MODELOS PARA DASHBOARD DE ESTUDIANTE
// ============================================

/**
 * Calificación individual de un estudiante
 */
data class StudentGrade(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("section_letter")
    val sectionLetter: String,
    @SerializedName("grade_number")
    val gradeNumber: Int,
    @SerializedName("bimester_name")
    val bimesterName: String,
    @SerializedName("session_title")
    val sessionTitle: String?,
    @SerializedName("competency_name")
    val competencyName: String?,
    val value: String,
    val observation: String?,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Perfil completo del estudiante
 */
data class StudentProfileData(
    @SerializedName("user_id")
    val userId: Int,
    val dni: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    val gender: String?,
    val address: String?,
    @SerializedName("enrollment_code")
    val enrollmentCode: String?,
    @SerializedName("enrollment_date")
    val enrollmentDate: String?
)

/**
 * Matrícula del estudiante en una sección
 */
data class StudentEnrollment(
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("section_letter")
    val sectionLetter: String,
    @SerializedName("grade_number")
    val gradeNumber: Int,
    @SerializedName("bimester_name")
    val bimesterName: String,
    val year: Int
)

/**
 * Respuesta del perfil de estudiante con secciones
 */
data class StudentProfileResponse(
    val profile: StudentProfileData,
    val sections: List<StudentEnrollment>
)

/**
 * Agrupación de calificaciones por sesión
 */
data class SessionGrades(
    val sessionTitle: String,
    val bimesterName: String,
    val grades: List<StudentGrade>
)

/**
 * Agrupación de calificaciones por competencia
 */
data class CompetencyGrades(
    val competencyName: String,
    val grades: List<StudentGrade>,
    val average: String? // Promedio calculado
)

// ============================================
// MODELOS PARA DASHBOARD DE APODERADO
// ============================================

/**
 * Estudiante asignado a un apoderado
 */
data class GuardianStudent(
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("student_name")
    val studentName: String,
    @SerializedName("section_letter")
    val sectionLetter: String,
    @SerializedName("grade_number")
    val gradeNumber: Int,
    @SerializedName("bimester_name")
    val bimesterName: String,
    val year: Int
)

/**
 * Relación apoderado-estudiante
 */
data class GuardianRelationship(
    @SerializedName("guardian_user_id")
    val guardianUserId: Int,
    @SerializedName("student_user_id")
    val studentUserId: Int,
    @SerializedName("relationship_type")
    val relationshipType: String,
    @SerializedName("is_primary")
    val isPrimary: Boolean,
    @SerializedName("can_view_grades")
    val canViewGrades: Boolean,
    @SerializedName("can_make_claims")
    val canMakeClaims: Boolean
)

/**
 * Resumen de calificaciones de un estudiante para apoderado
 */
data class StudentSummaryForGuardian(
    val student: GuardianStudent,
    val totalGrades: Int,
    val averageGrade: String?,
    val lastUpdate: String?
)

/**
 * Request para crear relación apoderado-estudiante
 */
data class CreateGuardianRelationshipRequest(
    @SerializedName("guardian_user_id")
    val guardianUserId: Int,
    @SerializedName("student_user_id")
    val studentUserId: Int,
    @SerializedName("relationship_type")
    val relationshipType: String,
    @SerializedName("is_primary")
    val isPrimary: Boolean = false
)

/**
 * Response al crear relación
 */
data class CreateGuardianRelationshipResponse(
    val success: Boolean,
    val message: String
)

/**
 * Resultado de búsqueda de estudiante
 */
data class StudentSearchResult(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("section_id")
    val sectionId: Int,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("section_letter")
    val sectionLetter: String,
    @SerializedName("grade_number")
    val gradeNumber: Int,
    @SerializedName("bimester_name")
    val bimesterName: String,
    val year: Int
)

