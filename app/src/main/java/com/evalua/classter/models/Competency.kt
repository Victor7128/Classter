package com.evalua.classter.models

// ✅ UNIFICAR - Competency principal (compatible con evaluation context)
data class Competency(
    val id: Int,
    val session_id: Int? = null,
    val number: Int? = null,
    val name: String,
    val display_name: String? = null, // ✅ IMPORTANTE: Tu backend usa esto
    val description: String? = null
)

data class NewCompetencyRequest(
    val name: String,
    val description: String
)

data class UpdateCompetencyRequest(
    val name: String? = null,
    val description: String? = null
)