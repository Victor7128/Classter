package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.CompetenciaTemplate
import com.google.android.material.card.MaterialCardView

class CompetenciasTemplateAdapter(
    private var competencias: List<CompetenciaTemplate>,
    private val onSelectionChanged: (List<CompetenciaTemplate>) -> Unit
) : RecyclerView.Adapter<CompetenciasTemplateAdapter.CompetenciaViewHolder>() {

    private val selectedCompetencias = mutableSetOf<Int>()

    inner class CompetenciaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardCompetencia: MaterialCardView = view.findViewById(R.id.cardCompetencia)
        val checkboxCompetencia: CheckBox = view.findViewById(R.id.checkboxCompetencia)
        val tvCompetenciaNombre: TextView = view.findViewById(R.id.tvCompetenciaNombre)
        val tvCompetenciaArea: TextView = view.findViewById(R.id.tvCompetenciaArea)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompetenciaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_competencia_template, parent, false)
        return CompetenciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompetenciaViewHolder, position: Int) {
        val competencia = competencias[position]

        holder.tvCompetenciaNombre.text = competencia.nombre
        holder.tvCompetenciaArea.text = "Área: ${competencia.area_nombre}"

        // Estado del checkbox
        val isSelected = selectedCompetencias.contains(competencia.id)
        holder.checkboxCompetencia.isChecked = isSelected

        // Color del card según selección
        holder.cardCompetencia.strokeWidth = if (isSelected) 4 else 0
        holder.cardCompetencia.strokeColor = if (isSelected) {
            holder.itemView.context.getColor(R.color.purple_500)
        } else {
            holder.itemView.context.getColor(android.R.color.transparent)
        }

        // Click en el card completo
        holder.cardCompetencia.setOnClickListener {
            toggleSelection(competencia.id)
            notifyItemChanged(position)
            updateSelectionCallback()
        }

        // Click en el checkbox
        holder.checkboxCompetencia.setOnClickListener {
            toggleSelection(competencia.id)
            notifyItemChanged(position)
            updateSelectionCallback()
        }
    }

    override fun getItemCount() = competencias.size

    private fun toggleSelection(competenciaId: Int) {
        if (selectedCompetencias.contains(competenciaId)) {
            selectedCompetencias.remove(competenciaId)
        } else {
            selectedCompetencias.add(competenciaId)
        }
    }

    private fun updateSelectionCallback() {
        val selected = competencias.filter { selectedCompetencias.contains(it.id) }
        onSelectionChanged(selected)
    }

    fun updateCompetencias(newCompetencias: List<CompetenciaTemplate>) {
        competencias = newCompetencias
        selectedCompetencias.clear()
        notifyDataSetChanged()
        updateSelectionCallback()
    }

    fun getSelectedCompetencias(): List<CompetenciaTemplate> {
        return competencias.filter { selectedCompetencias.contains(it.id) }
    }

    fun clearSelection() {
        selectedCompetencias.clear()
        notifyDataSetChanged()
        updateSelectionCallback()
    }
}