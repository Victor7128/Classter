package com.evalua.classter.models

import com.google.gson.annotations.SerializedName

data class ReniecResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: ReniecData?
)

data class ReniecData(
    @SerializedName("numero")
    val numero: String,

    @SerializedName("nombre_completo")
    val nombreCompleto: String,

    @SerializedName("nombres")
    val nombres: String,

    @SerializedName("apellido_paterno")
    val apellidoPaterno: String,

    @SerializedName("apellido_materno")
    val apellidoMaterno: String,

    @SerializedName("codigo_verificacion")
    val codigoVerificacion: String
)