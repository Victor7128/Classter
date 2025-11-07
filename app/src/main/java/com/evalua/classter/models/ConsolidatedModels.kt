package com.evalua.classter.models

import com.google.gson.annotations.SerializedName
data class ConsolidatedResponse(
    val students: List<ConsolidatedStudent>,
    val sessions: List<ConsolidatedSession>,
    val competencies: List<ConsolidatedCompetency>,
    val abilities: List<ConsolidatedAbility>,
    val criteria: List<ConsolidatedCriterion>,
    val values: List<ConsolidatedValue>,
    val observations: List<ConsolidatedObservation>
)
data class ConsolidatedStudent(
    val id: Int,

    @SerializedName("full_name")
    val full_name: String
)

data class ConsolidatedSession(
    val id: Int,
    @SerializedName("title")
    val title: String?,
    @SerializedName("number")
    val number: Int
) {
    val display_name: String
        get() = title?.let { "Sesión $number - $it" } ?: "Sesión $number"
    val session_order: Int
        get() = number
}

data class ConsolidatedCompetency(
    val id: Int,
    @SerializedName("session_id")
    val session_id: Int,
    @SerializedName("display_name")
    val display_name: String
)
data class ConsolidatedAbility(
    val id: Int,

    @SerializedName("competency_id")
    val competency_id: Int,

    @SerializedName("display_name")
    val display_name: String
)

data class ConsolidatedCriterion(
    val id: Int,

    @SerializedName("ability_id")
    val ability_id: Int,

    @SerializedName("display_name")
    val display_name: String
)

data class ConsolidatedValue(
    @SerializedName("student_id")
    val student_id: Int,

    @SerializedName("criterion_id")
    val criterion_id: Int,

    val value: String
)

data class ConsolidatedObservation(
    @SerializedName("student_id")
    val student_id: Int,

    @SerializedName("ability_id")
    val ability_id: Int,

    val observation: String
)