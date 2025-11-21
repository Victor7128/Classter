package com.evalua.classter.adapters

import android.graphics.Typeface
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

class NotificationsAdapter(
    private var notifications: List<Notification>,
    private val notificationManager: NotificationManager,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardNotification: MaterialCardView = view.findViewById(R.id.cardNotification)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val viewUnreadIndicator: View = view.findViewById(R.id.viewUnreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
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

        // Estilo para no leídas
        if (!notification.isRead) {
            holder.viewUnreadIndicator.visibility = View.VISIBLE
            holder.tvTitle.setTypeface(null, Typeface.BOLD)
            holder.tvMessage.setTypeface(null, Typeface.BOLD)
            holder.cardNotification.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.primaryLight)
            )
        } else {
            holder.viewUnreadIndicator.visibility = View.GONE
            holder.tvTitle.setTypeface(null, Typeface.NORMAL)
            holder.tvMessage.setTypeface(null, Typeface.NORMAL)
            holder.cardNotification.setCardBackgroundColor(
                holder.itemView.context.getColor(android.R.color.white)
            )
        }

        // Click listener
        holder.cardNotification.setOnClickListener {
            onNotificationClick(notification)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}

