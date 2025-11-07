package com.evalua.classter.models

// ✅ Header de columna (Criterios)
data class ColumnHeaderModel(
    val id: String,
    val title: String
)

// ✅ Header de fila (Estudiantes)
data class RowHeaderModel(
    val id: String,
    val title: String,
    val studentId: Int
)

// ✅ Celda de datos (Evaluación)
data class CellModel(
    val id: String,
    val studentId: Int,
    val criterionId: Int,
    val currentValue: String?,
    val hasObservation: Boolean = false
)