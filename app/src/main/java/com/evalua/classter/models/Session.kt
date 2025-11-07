package com.evalua.classter.models

data class Session(
    val id: Int,
    val section_id: Int,
    val number: Int,
    val title: String,
    val date: String,
    val created_at: String
)

data class NewSessionRequest(
    val title: String,
    val date: String = ""
)

data class UpdateSessionRequest(
    val title: String? = null,
    val date: String? = null
)