package com.evalua.classter.models

data class Product(
    val id: Int,
    val session_id: Int,
    val number: Int? = null,
    val name: String,
    val description: String? = null
)

data class NewProductRequest(
    val name: String,
    val description: String? = null
)

data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null
)