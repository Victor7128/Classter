package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Session
import java.text.SimpleDateFormat
import java.util.*

class SessionsAdapter(
    private var sessions: List<Session>,
    private val onSessionClick: (Session) -> Unit,
    private val onSessionOptionsClick: (Session, View) -> Unit
) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSessionNumber: TextView = view.findViewById(R.id.tvSessionNumber)
        val tvSessionTitle: TextView = view.findViewById(R.id.tvSessionTitle)
        val tvSessionDate: TextView = view.findViewById(R.id.tvSessionDate)
        val btnSessionOptions: ImageButton = view.findViewById(R.id.btnSessionOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]

        holder.tvSessionNumber.text = session.number.toString()
        holder.tvSessionTitle.text = session.title
        holder.tvSessionDate.text = formatDate(session.date)

        holder.itemView.setOnClickListener {
            onSessionClick(session)
        }

        holder.btnSessionOptions.setOnClickListener {
            onSessionOptionsClick(session, it)
        }
    }

    override fun getItemCount() = sessions.size

    fun updateSessions(newSessions: List<Session>) {
        sessions = newSessions
        notifyDataSetChanged()
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }
}