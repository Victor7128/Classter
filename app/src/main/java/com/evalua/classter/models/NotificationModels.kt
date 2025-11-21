package com.evalua.classter.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de Notificación local
 */
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    var isRead: Boolean = false,
    val extraData: String? = null // JSON con datos adicionales si es necesario
)

/**
 * Tipos de notificación
 */
enum class NotificationType {
    NEW_GRADE,          // Nueva calificación
    GRADE_UPDATED,      // Calificación actualizada
    STUDENT_ADDED,      // Estudiante agregado (para apoderados)
    REMINDER,           // Recordatorio general
    SYSTEM              // Notificación del sistema
}

/**
 * Data class para agrupar notificaciones por fecha
 */
data class NotificationGroup(
    val date: String,
    val notifications: List<Notification>
)

