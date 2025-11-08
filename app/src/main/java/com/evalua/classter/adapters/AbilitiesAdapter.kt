package com.evalua.classter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.Ability
import com.evalua.classter.models.Criterion
import com.google.android.material.button.MaterialButton

class AbilitiesAdapter(
    private var abilities: List<Ability>,
    private var abilityCriteria: Map<Int, List<Criterion>>,
    private var expandedAbilityId: Int? = null,
    private val onAbilityExpanded: (Int, Boolean) -> Unit,
    private val onDeleteAbility: (Ability) -> Unit,  // ✅ CAMBIO
    private val onAddCriterion: (Int) -> Unit,
    private val onEditCriterion: (Criterion) -> Unit,  // ✅ NUEVO
    private val onDeleteCriterion: (Criterion) -> Unit  // ✅ CAMBIO
) : RecyclerView.Adapter<AbilitiesAdapter.AbilityViewHolder>() {

    inner class AbilityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutAbilityHeader: View = view.findViewById(R.id.layoutAbilityHeader)
        val ivAbilityExpandIcon: ImageView = view.findViewById(R.id.ivAbilityExpandIcon)
        val tvAbilityName: TextView = view.findViewById(R.id.tvAbilityName)
        val tvCriteriaCount: TextView = view.findViewById(R.id.tvCriteriaCount)
        val btnDeleteAbility: MaterialButton = view.findViewById(R.id.btnDeleteAbility)  // ✅ NUEVO
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

        // Mostrar contador de criterios
        val criteria = abilityCriteria[ability.id] ?: emptyList()
        if (criteria.isNotEmpty()) {
            holder.tvCriteriaCount.visibility = View.VISIBLE
            val count = criteria.size
            holder.tvCriteriaCount.text = "✓ $count criterio${if (count > 1) "s" else ""}"
        } else {
            holder.tvCriteriaCount.visibility = View.GONE
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

        // ✅ NUEVO: Botón eliminar
        holder.btnDeleteAbility.setOnClickListener {
            onDeleteAbility(ability)
        }

        // Agregar criterio
        holder.layoutAgregarCriterio.setOnClickListener {
            onAddCriterion(ability.id)
        }

        // Setup RecyclerView de criterios
        if (isExpanded) {
            val criteriaAdapter = CriteriaAdapter(
                criteria = criteria,
                onEditCriterion = onEditCriterion,  // ✅ NUEVO
                onDeleteCriterion = onDeleteCriterion  // ✅ CAMBIO
            )
            holder.rvCriteria.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.rvCriteria.adapter = criteriaAdapter
        }
    }

    override fun getItemCount() = abilities.size

    fun updateData(
        newAbilities: List<Ability>,
        newAbilityCriteria: Map<Int, List<Criterion>>
    ) {
        abilities = newAbilities
        abilityCriteria = newAbilityCriteria
        notifyDataSetChanged()
    }

    fun updateExpandedState(newExpandedAbilityId: Int?) {
        expandedAbilityId = newExpandedAbilityId
        notifyDataSetChanged()
    }
}