package com.evalua.classter.models

// ✅ Contexto completo para reportes/consolidado
data class ReportsContext(
    val competency: Competency,
    val abilities: List<Ability>,
    val criteria: List<Criterion>,
    val students: List<Student>,
    val sessions: List<ReportSession>,
    val grades: List<ReportGrade>
)

// ✅ Sesión para reportes (con información adicional)
data class ReportSession(
    val id: Int,
    val competency_id: Int,
    val title: String,
    val description: String?,
    val session_date: String?
)

// ✅ Calificación para reportes
data class ReportGrade(
    val id: Int,
    val student_id: Int,
    val session_id: Int,
    val criterion_id: Int,
    val ability_id: Int,
    val value: String?, // AD, A, B, C
    val observation: String?,
    val created_at: String?
)

// ✅ Header de fila para consolidado (con número)
data class ReportRowHeaderModel(
    val id: String,
    val title: String,
    val studentId: Int,
    val number: Int
)

data class ReportColumnHeaderModel(
    val id: String,
    val title: String,
    val level: Int, // 1=Sesión, 2=Competencia, 3=Capacidad, 4=Criterio/Obs
    val parentId: String? = null,
    val colspan: Int = 1,
    val rowspan: Int = 1,

    // Identificadores específicos
    val sessionId: Int? = null,
    val competencyId: Int? = null,
    val abilityId: Int? = null,
    val criterionId: Int? = null,

    // Tipos de columna
    val isObservation: Boolean = false,
    val isPromedio: Boolean = false,
    val isPromedioFinal: Boolean = false,
    val isEditable: Boolean = false, // ✅ NUEVO: Para editar promedios

    // Colores y estilos
    val backgroundColor: String? = null,
    val textColor: String? = null
)

data class ReportCellModel(
    val id: String,
    val studentId: Int,
    val sessionId: Int? = null,
    val criterionId: Int? = null,
    val abilityId: Int? = null,
    val grade: String? = null,
    val observation: String? = null,

    // Tipos de celda
    val isPromedio: Boolean = false,
    val isPromedioFinal: Boolean = false,
    val isObservation: Boolean = false,
    val isEditable: Boolean = false, // ✅ Para permitir edición de promedios

    // Estado
    val isEmpty: Boolean = true, // ✅ Promedios siempre vacíos inicialmente
    val hasObservation: Boolean = false
)

data class SessionData(
    val session: ConsolidatedSession,
    val competencies: List<CompetencyData>
)

data class CompetencyData(
    val competency: ConsolidatedCompetency,
    val abilities: List<AbilityData>
)

data class AbilityData(
    val ability: ConsolidatedAbility,
    val criteria: List<ConsolidatedCriterion>
)