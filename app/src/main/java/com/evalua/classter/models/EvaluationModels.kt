package com.evalua.classter.models

import com.google.gson.annotations.SerializedName

// ✅ Modelo para el contexto completo de evaluación
data class EvaluationContext(
    val locked: Boolean,
    val competency: Competency, // ✅ Usa el modelo unificado
    val product: Product,
    val abilities: List<Ability>, // ✅ Usa el modelo unificado
    val criteria: List<Criterion>, // ✅ Usa el modelo unificado
    val students: List<Student>, // ✅ Usa el modelo unificado
    val values: List<EvaluationValue>
)

// ✅ Modelo para una evaluación individual
data class EvaluationValue(
    @SerializedName("student_id")
    val student_id: Int,
    @SerializedName("ability_id")
    val ability_id: Int,
    @SerializedName("criterion_id")
    val criterion_id: Int,
    val value: String?,
    val observation: String?
)

// ✅ Modelo para crear/actualizar evaluación
data class EvalValueRequest(
    @SerializedName("session_id")
    val session_id: Int,
    @SerializedName("competency_id")
    val competency_id: Int,
    @SerializedName("ability_id")
    val ability_id: Int,
    @SerializedName("criterion_id")
    val criterion_id: Int,
    @SerializedName("product_id")
    val product_id: Int,
    @SerializedName("student_id")
    val student_id: Int,
    val value: String?,
    val observation: String?
)

// ✅ Respuesta al crear evaluación
data class EvaluationResponse(
    val id: Int
)

// ✅ Item de evaluación individual
data class EvaluationItem(
    val id: Int,
    @SerializedName("session_id")
    val session_id: Int,
    @SerializedName("competency_id")
    val competency_id: Int,
    @SerializedName("ability_id")
    val ability_id: Int,
    @SerializedName("criterion_id")
    val criterion_id: Int,
    @SerializedName("product_id")
    val product_id: Int,
    @SerializedName("student_id")
    val student_id: Int,
    val value: String?,
    val observation: String?,
    @SerializedName("updated_at")
    val updated_at: String?
)

// ✅ Respuesta de matriz de evaluación
data class MatrixResponse(
    val locked: Boolean,
    val competency: Map<String, Any>,
    val abilities: List<Map<String, Any>>,
    val criteria: List<Map<String, Any>>,
    val products: List<Map<String, Any>>,
    val students: List<Map<String, Any>>,
    val values: List<Map<String, Any>>
)

// ✅ Respuesta consolidada de sección
data class ConsolidatedSectionResponse(
    val students: List<Map<String, Any>>,
    val sessions: List<Map<String, Any>>,
    val competencies: List<Map<String, Any>>,
    val abilities: List<Map<String, Any>>,
    val criteria: List<Map<String, Any>>,
    val values: List<Map<String, Any>>,
    val observations: List<Map<String, Any>>
)

// ✅ Respuesta genérica de API
data class EvaluationApiResponse<T>(
    val data: T,
    val message: String? = null,
    val status: String? = null
)
