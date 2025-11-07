package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Criterion

class CriteriaAdapter(
    private var criteria: List<Criterion>,
    private val onCriterionOptionsClick: (Criterion, View) -> Unit
) : RecyclerView.Adapter<CriteriaAdapter.CriterionViewHolder>() {

    inner class CriterionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCriterionName: TextView = view.findViewById(R.id.tvCriterionName)
        val tvCriterionDescription: TextView = view.findViewById(R.id.tvCriterionDescription)
        val btnCriterionOptions: ImageButton = view.findViewById(R.id.btnCriterionOptions)
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

        holder.btnCriterionOptions.setOnClickListener {
            onCriterionOptionsClick(criterion, it)
        }
    }

    override fun getItemCount() = criteria.size
}