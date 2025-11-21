package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Notification
import com.evalua.classter.models.NotificationType
import com.evalua.classter.utils.NotificationManager
import com.google.android.material.card.MaterialCardView

class NotificationsCompactAdapter(
    private var notifications: List<Notification>,
    private val notificationManager: NotificationManager,
    private val onNotificationClick: (Notification) -> Unit,
    private val onNotificationDismiss: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsCompactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val ivIcon: android.widget.ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val btnDismiss: android.widget.ImageButton = view.findViewById(R.id.btnDismiss)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_compact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        holder.tvTime.text = notificationManager.formatTimestamp(notification.timestamp)

        // Icono según tipo
        val iconRes = when (notification.type) {
            NotificationType.NEW_GRADE -> R.drawable.ic_grade
            NotificationType.GRADE_UPDATED -> R.drawable.ic_edit
            NotificationType.STUDENT_ADDED -> R.drawable.ic_user_add
            NotificationType.REMINDER -> R.drawable.ic_bell
            NotificationType.SYSTEM -> R.drawable.ic_info
        }
        holder.ivIcon.setImageResource(iconRes)

        // Click listener en la tarjeta
        holder.card.setOnClickListener {
            onNotificationClick(notification)
        }

        // Click listener en el botón de eliminar
        holder.btnDismiss.setOnClickListener {
            onNotificationDismiss(notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}

