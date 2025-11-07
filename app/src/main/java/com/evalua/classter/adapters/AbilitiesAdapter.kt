package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Ability
import com.evalua.classter.models.Criterion

class AbilitiesAdapter(
    private var abilities: List<Ability>,
    private var abilityCriteria: Map<Int, List<Criterion>>,
    private var expandedAbilityId: Int? = null,
    private val onAbilityExpanded: (Int, Boolean) -> Unit,
    private val onAbilityOptionsClick: (Ability, View) -> Unit,
    private val onAddCriterion: (Int) -> Unit,
    private val onCriterionOptionsClick: (Criterion, View) -> Unit
) : RecyclerView.Adapter<AbilitiesAdapter.AbilityViewHolder>() {

    inner class AbilityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutAbilityHeader: View = view.findViewById(R.id.layoutAbilityHeader)
        val ivAbilityExpandIcon: ImageView = view.findViewById(R.id.ivAbilityExpandIcon)
        val tvAbilityName: TextView = view.findViewById(R.id.tvAbilityName)
        val tvAbilityDescription: TextView = view.findViewById(R.id.tvAbilityDescription)
        val btnAbilityOptions: ImageButton = view.findViewById(R.id.btnAbilityOptions)
        val layoutAbilityExpandedContent: View = view.findViewById(R.id.layoutAbilityExpandedContent)
        val layoutAgregarCriterio: View = view.findViewById(R.id.layoutAgregarCriterio)
        val rvCriteria: RecyclerView = view.findViewById(R.id.rvCriteria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbilityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ability_expandable, parent, false)
        return AbilityViewHolder(view)
    }

    override fun onBindViewHolder(holder: AbilityViewHolder, position: Int) {
        val ability = abilities[position]
        val isExpanded = expandedAbilityId == ability.id

        holder.tvAbilityName.text = ability.name

        if (ability.description.isNullOrEmpty()) {
            holder.tvAbilityDescription.visibility = View.GONE
        } else {
            holder.tvAbilityDescription.visibility = View.VISIBLE
            holder.tvAbilityDescription.text = ability.description
        }

        // Estado de expansión
        holder.layoutAbilityExpandedContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivAbilityExpandIcon.rotation = if (isExpanded) 180f else 0f

        // Click para expandir/contraer
        holder.layoutAbilityHeader.setOnClickListener {
            val newExpanded = !isExpanded
            expandedAbilityId = if (newExpanded) ability.id else null
            onAbilityExpanded(ability.id, newExpanded)
            notifyDataSetChanged()
        }

        // Opciones de capacidad
        holder.btnAbilityOptions.setOnClickListener {
            onAbilityOptionsClick(ability, it)
        }

        // Agregar criterio
        holder.layoutAgregarCriterio.setOnClickListener {
            onAddCriterion(ability.id)
        }

        // Setup RecyclerView de criterios
        if (isExpanded) {
            val criteria = abilityCriteria[ability.id] ?: emptyList()
            val criteriaAdapter = CriteriaAdapter(
                criteria = criteria,
                onCriterionOptionsClick = onCriterionOptionsClick
            )
            holder.rvCriteria.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.rvCriteria.adapter = criteriaAdapter
        }
    }

    override fun getItemCount() = abilities.size
}