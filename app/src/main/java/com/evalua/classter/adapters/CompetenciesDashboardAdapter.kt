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
import com.evalua.classter.models.Competency
import com.evalua.classter.models.Criterion

class CompetenciesDashboardAdapter(
    private var competencies: List<Competency>,
    private var competencyAbilities: Map<Int, List<Ability>>,
    private var abilityCriteria: Map<Int, List<Criterion>>,
    private var expandedCompetencyId: Int? = null,
    private var expandedAbilityId: Int? = null,
    private val onCompetencyExpanded: (Int, Boolean) -> Unit,
    private val onCompetencyOptionsClick: (Competency, View) -> Unit,
    private val onAddAbility: (Int) -> Unit,
    private val onAbilityExpanded: (Int, Boolean) -> Unit,
    private val onAbilityOptionsClick: (Ability, View) -> Unit,
    private val onAddCriterion: (Int) -> Unit,
    private val onCriterionOptionsClick: (Criterion, View) -> Unit
) : RecyclerView.Adapter<CompetenciesDashboardAdapter.CompetencyViewHolder>() {

    inner class CompetencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutCompetencyHeader: View = view.findViewById(R.id.layoutCompetencyHeader)
        val ivExpandIcon: ImageView = view.findViewById(R.id.ivExpandIcon)
        val tvCompetencyName: TextView = view.findViewById(R.id.tvCompetencyName)
        val tvCompetencyDescription: TextView = view.findViewById(R.id.tvCompetencyDescription)
        val btnCompetencyOptions: ImageButton = view.findViewById(R.id.btnCompetencyOptions)
        val layoutExpandedContent: View = view.findViewById(R.id.layoutExpandedContent)
        val layoutAgregarCapacidad: View = view.findViewById(R.id.layoutAgregarCapacidad)
        val rvAbilities: RecyclerView = view.findViewById(R.id.rvAbilities)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompetencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_competency_expandable, parent, false)
        return CompetencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompetencyViewHolder, position: Int) {
        val competency = competencies[position]
        val isExpanded = expandedCompetencyId == competency.id

        holder.tvCompetencyName.text = competency.name
        holder.tvCompetencyDescription.text = competency.description

        // Estado de expansión
        holder.layoutExpandedContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpandIcon.rotation = if (isExpanded) 180f else 0f

        // Click para expandir/contraer
        holder.layoutCompetencyHeader.setOnClickListener {
            val newExpanded = !isExpanded
            expandedCompetencyId = if (newExpanded) competency.id else null
            expandedAbilityId = null // Reset ability expansion
            onCompetencyExpanded(competency.id, newExpanded)
            notifyDataSetChanged()
        }

        // Opciones de competencia
        holder.btnCompetencyOptions.setOnClickListener {
            onCompetencyOptionsClick(competency, it)
        }

        // Agregar capacidad
        holder.layoutAgregarCapacidad.setOnClickListener {
            onAddAbility(competency.id)
        }

        // Setup RecyclerView de capacidades
        if (isExpanded) {
            val abilities = competencyAbilities[competency.id] ?: emptyList()
            val abilitiesAdapter = AbilitiesAdapter(
                abilities = abilities,
                abilityCriteria = abilityCriteria,
                expandedAbilityId = expandedAbilityId,
                onAbilityExpanded = { abilityId, expanded ->
                    expandedAbilityId = if (expanded) abilityId else null
                    onAbilityExpanded(abilityId, expanded)
                    notifyDataSetChanged()
                },
                onAbilityOptionsClick = onAbilityOptionsClick,
                onAddCriterion = onAddCriterion,
                onCriterionOptionsClick = onCriterionOptionsClick
            )
            holder.rvAbilities.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.rvAbilities.adapter = abilitiesAdapter
        }
    }

    override fun getItemCount() = competencies.size

    fun updateData(
        newCompetencies: List<Competency>,
        newAbilities: Map<Int, List<Ability>>,
        newCriteria: Map<Int, List<Criterion>>,
        keepExpanded: Boolean = false
    ) {
        competencies = newCompetencies
        competencyAbilities = newAbilities
        abilityCriteria = newCriteria

        if (!keepExpanded) {
            expandedCompetencyId = null
            expandedAbilityId = null
        }

        notifyDataSetChanged()
    }

    fun updateExpandedStates(competencyId: Int?, abilityId: Int?) {
        expandedCompetencyId = competencyId
        expandedAbilityId = abilityId
        notifyDataSetChanged()
    }
}