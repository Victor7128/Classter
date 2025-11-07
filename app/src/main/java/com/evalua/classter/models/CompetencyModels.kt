package com.evalua.classter.models

// ✅ Ability corregido (compatible con backend)
data class Ability(
    val id: Int,
    val competency_id: Int,
    val number: Int? = null,
    val name: String? = null,
    val display_name: String? = null, // ✅ Tu backend usa display_name
    val description: String? = null
)

// ✅ Criterion corregido (compatible con backend)
data class Criterion(
    val id: Int,
    val ability_id: Int,
    val number: Int? = null,
    val name: String? = null,
    val display_name: String? = null, // ✅ Tu backend usa display_name
    val description: String? = null
)

// Requests para el backend principal
data class NewAbilityRequest(
    val name: String,
    val description: String? = null
)

data class UpdateAbilityRequest(
    val name: String? = null,
    val description: String? = null
)

data class NewCriterionRequest(
    val name: String,
    val description: String? = null
)

data class UpdateCriterionRequest(
    val name: String? = null,
    val description: String? = null
)

// Modelos para el catálogo curricular
data class CapacidadTemplate(
    val id: Int,
    val nombre: String
)