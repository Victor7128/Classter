package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Criterion
import com.google.android.material.button.MaterialButton

class CriteriaAdapter(
    private var criteria: List<Criterion>,
    private val onEditCriterion: (Criterion) -> Unit,  // ✅ NUEVO
    private val onDeleteCriterion: (Criterion) -> Unit  // ✅ CAMBIO
) : RecyclerView.Adapter<CriteriaAdapter.CriterionViewHolder>() {

    inner class CriterionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCriterionName: TextView = view.findViewById(R.id.tvCriterionName)
        val tvCriterionDescription: TextView = view.findViewById(R.id.tvCriterionDescription)
        val btnEditCriterion: MaterialButton = view.findViewById(R.id.btnEditCriterion)  // ✅ NUEVO
        val btnDeleteCriterion: MaterialButton = view.findViewById(R.id.btnDeleteCriterion)  // ✅ NUEVO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CriterionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_criterion, parent, false)
        return CriterionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CriterionViewHolder, position: Int) {
        val criterion = criteria[position]

        holder.tvCriterionName.text = criterion.name

        if (criterion.description.isNullOrEmpty()) {
            holder.tvCriterionDescription.visibility = View.GONE
        } else {
            holder.tvCriterionDescription.visibility = View.VISIBLE
            holder.tvCriterionDescription.text = criterion.description
        }

        // ✅ NUEVO: Botón editar
        holder.btnEditCriterion.setOnClickListener {
            onEditCriterion(criterion)
        }

        // ✅ NUEVO: Botón eliminar
        holder.btnDeleteCriterion.setOnClickListener {
            onDeleteCriterion(criterion)
        }
    }

    override fun getItemCount() = criteria.size
}