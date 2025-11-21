package com.evalua.classter.utils

import android.content.Context
import android.content.SharedPreferences
import com.evalua.classter.models.Notification
import com.evalua.classter.models.NotificationType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class NotificationManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications_list"
        private const val MAX_NOTIFICATIONS = 50 // Límite de notificaciones guardadas
    }

    /**
     * Agregar una nueva notificación
     */
    fun addNotification(
        title: String,
        message: String,
        type: NotificationType,
        extraData: String? = null,
        showPush: Boolean = true
    ) {
        val notifications = getNotifications().toMutableList()

        val newNotification = Notification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            extraData = extraData
        )

        notifications.add(0, newNotification) // Agregar al inicio

        // Limitar cantidad de notificaciones
        if (notifications.size > MAX_NOTIFICATIONS) {
            notifications.subList(MAX_NOTIFICATIONS, notifications.size).clear()
        }

        saveNotifications(notifications)

        // Mostrar notificación push del sistema si está habilitado
        if (showPush) {
            showSystemNotification(title, message, type)
        }
    }

    /**
     * Mostrar notificación push del sistema Android
     */
    private fun showSystemNotification(title: String, message: String, type: NotificationType) {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager

            val channelId = "classter_notifications"
            val notificationId = System.currentTimeMillis().toInt()

            // Crear canal de notificación (Android 8.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Notificaciones Classter",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones de calificaciones y actualizaciones"
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Crear intent para abrir la app
            val intent = android.content.Intent(context, Class.forName("com.evalua.classter.NotificationsActivity"))
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP

            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            // Icono según tipo de notificación
            val iconRes = when (type) {
                NotificationType.NEW_GRADE -> android.R.drawable.star_on
                NotificationType.GRADE_UPDATED -> android.R.drawable.ic_menu_edit
                NotificationType.STUDENT_ADDED -> android.R.drawable.ic_menu_add
                NotificationType.REMINDER -> android.R.drawable.ic_menu_today
                NotificationType.SYSTEM -> android.R.drawable.ic_dialog_info
            }

            // Construir notificación
            val builder = android.app.Notification.Builder(context).apply {
                setContentTitle(title)
                setContentText(message)
                setSmallIcon(iconRes)
                setContentIntent(pendingIntent)
                setAutoCancel(true)
                setPriority(android.app.Notification.PRIORITY_DEFAULT)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    setChannelId(channelId)
                }
            }

            // Mostrar notificación
            notificationManager.notify(notificationId, builder.build())

        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Error mostrando notificación push: ${e.message}")
        }
    }

    /**
     * Obtener todas las notificaciones
     */
    fun getNotifications(): List<Notification> {
        val json = prefs.getString(KEY_NOTIFICATIONS, "[]")
        val type = object : TypeToken<List<Notification>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * Obtener notificaciones no leídas
     */
    fun getUnreadNotifications(): List<Notification> {
        return getNotifications().filter { !it.isRead }
    }

    /**
     * Obtener cantidad de notificaciones no leídas
     */
    fun getUnreadCount(): Int {
        return getUnreadNotifications().size
    }

    /**
     * Marcar notificación como leída
     */
    fun markAsRead(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveNotifications(notifications)
        }
    }

    /**
     * Marcar todas como leídas
     */
    fun markAllAsRead() {
        val notifications = getNotifications().map { it.copy(isRead = true) }
        saveNotifications(notifications)
    }

    /**
     * Eliminar notificación
     */
    fun deleteNotification(notificationId: String) {
        val notifications = getNotifications().filter { it.id != notificationId }
        saveNotifications(notifications)
    }

    /**
     * Eliminar todas las notificaciones
     */
    fun deleteAllNotifications() {
        saveNotifications(emptyList())
    }

    /**
     * Guardar notificaciones
     */
    private fun saveNotifications(notifications: List<Notification>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    /**
     * Formatear fecha para mostrar
     */
    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Ahora"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            diff < 604800_000 -> "${diff / 86400_000}d"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    /**
     * Obtener fecha del día para agrupar
     */
    fun getDateLabel(timestamp: Long): String {
        val notificationDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(notificationDate, today) -> "Hoy"
            isSameDay(notificationDate, yesterday) -> "Ayer"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

