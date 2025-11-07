package com.evalua.classter.models

import com.google.gson.annotations.SerializedName

data class Area(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,  // ✅ ASEGÚRATE DE QUE ESTE CAMPO EXISTE

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("color")
    val color: String? = null,

    @SerializedName("competencias")
    val competencias: List<CompetenciaDetallada>? = null
)

// ✅ Modelo para competencias que vienen de la API (con descripción y capacidades)
data class CompetenciaDetallada(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("capacidades")
    val capacidades: List<CapacidadDetallada>? = null
)

// ✅ Modelo para capacidades anidadas en competencias
data class CapacidadDetallada(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String? = null
)


data class CompetenciaTemplate(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("area_id")
    val area_id: Int,

    @SerializedName("area_nombre")
    val area_nombre: String
)

data class Capacidad(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("competencia_id")
    val competenciaId: Int? = null,

    @SerializedName("description")
    val description: String? = null
)