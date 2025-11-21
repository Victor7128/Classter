package com.evalua.classter

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.adapters.NotificationsAdapter
import com.evalua.classter.models.Notification
import com.evalua.classter.utils.NotificationManager
import com.google.android.material.appbar.MaterialToolbar

class NotificationsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvMarkAllRead: TextView

    private lateinit var notificationManager: NotificationManager
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        notificationManager = NotificationManager(this)

        initViews()
        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvNotifications = findViewById(R.id.rvNotifications)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notificaciones"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            notifications = emptyList(),
            notificationManager = notificationManager,
            onNotificationClick = { notification ->
                handleNotificationClick(notification)
            }
        )

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter

        // Marcar todas como leídas
        tvMarkAllRead.setOnClickListener {
            notificationManager.markAllAsRead()
            loadNotifications()
        }
    }

    private fun loadNotifications() {
        val notifications = notificationManager.getNotifications()

        if (notifications.isEmpty()) {
            showEmptyState(true)
            tvMarkAllRead.visibility = View.GONE
        } else {
            showEmptyState(false)
            adapter.updateNotifications(notifications)

            // Mostrar "Marcar todas como leídas" solo si hay no leídas
            val hasUnread = notifications.any { !it.isRead }
            tvMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        // Marcar como leída
        if (!notification.isRead) {
            notificationManager.markAsRead(notification.id)
            loadNotifications()
        }

        // Aquí puedes agregar navegación según el tipo de notificación
        // Por ejemplo, si es NEW_GRADE, abrir el dashboard de calificaciones
    }

    private fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        rvNotifications.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
}

