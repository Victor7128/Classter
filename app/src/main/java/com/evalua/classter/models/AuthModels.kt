package com.evalua.classter.models

import com.google.gson.annotations.SerializedName

// ============================================
// REQUEST MODELS
// ============================================

data class RegisterAlumnoRequest(
    @SerializedName("dni")
    val dni: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("firebase_uid")
    val firebaseUid: String,

    @SerializedName("nombres")
    val nombres: String? = null,

    @SerializedName("apellido_paterno")
    val apellidoPaterno: String? = null,

    @SerializedName("apellido_materno")
    val apellidoMaterno: String? = null
)

data class RegisterApoderadoRequest(
    @SerializedName("dni")
    val dni: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("relationship_type")
    val relationshipType: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("firebase_uid")
    val firebaseUid: String,

    @SerializedName("occupation")
    val occupation: String? = null,

    @SerializedName("workplace")
    val workplace: String? = null
)

data class RegisterDocenteRequest(
    @SerializedName("dni")
    val dni: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("area_name")
    val areaName: String,

    @SerializedName("area_id")
    val areaId: Int? = null,

    @SerializedName("email")
    val email: String,

    @SerializedName("firebase_uid")
    val firebaseUid: String,

    @SerializedName("employee_code")
    val employeeCode: String? = null,

    @SerializedName("specialization")
    val specialization: String? = null
)

// ============================================
// RESPONSE MODELS
// ============================================

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T?
)

data class UserResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("profile_data")
    val profileData: Map<String, Any?>
)

data class ErrorResponse(
    @SerializedName("error")
    val error: String,

    @SerializedName("details")
    val details: String?
)