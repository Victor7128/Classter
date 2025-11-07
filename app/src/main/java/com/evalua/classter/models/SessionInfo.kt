package com.evalua.classter.models

data class SessionInfo(
    val id: Int,
    val title: String,
    val grade: String,     // "5°"
    val section: String,   // "A"
    val bimester: String   // "II"
)