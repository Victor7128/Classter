package com.evalua.classter.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout  // ✅ AGREGAR ESTE IMPORT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.models.*

class ReportStudentAdapter(
    private val students: List<ConsolidatedStudent>,
    private val sessions: List<ConsolidatedSession>,
    private val competencies: List<ConsolidatedCompetency>,
    private val abilitiesByCompetency: Map<Int, List<ConsolidatedAbility>>,
    private val criteriaByAbility: Map<Int, List<ConsolidatedCriterion>>,
    private val values: List<ConsolidatedValue>,
    private val observations: List<ConsolidatedObservation>,
    private val onEditAverage: ((studentId: Int, abilityId: Int?, value: String?) -> Unit)? = null,
    private val onEditFinalAverage: ((studentId: Int, value: String?) -> Unit)? = null,
    private val onObservationClick: ((studentId: Int, abilityId: Int, criterionId: Int) -> Unit)? = null
) : RecyclerView.Adapter<ReportStudentAdapter.StudentViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardStudent)
        val tvNumber: TextView = itemView.findViewById(R.id.tvStudentNumber)
        val tvName: TextView = itemView.findViewById(R.id.tvStudentName)

        // ✅ CAMBIO 1: Cambiar de ImageView a FrameLayout
        val ivExpand: FrameLayout = itemView.findViewById(R.id.ivExpandCollapse)
        val ivGlobalObservation: FrameLayout = itemView.findViewById(R.id.ivGlobalObservation)

        val llExpandableContent: LinearLayout = itemView.findViewById(R.id.llExpandableContent)
        val llSessions: LinearLayout = itemView.findViewById(R.id.llSessions)
        val etFinalAverage: EditText = itemView.findViewById(R.id.etFinalAverage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        val isExpanded = expandedPositions.contains(position)

        // Configurar header de la card
        holder.tvNumber.text = "${position + 1}"
        holder.tvName.text = student.full_name

        // ✅ NUEVO: Acceder al ImageView interno y configurar rotación
        val expandIconImageView = holder.ivExpand.findViewById<ImageView>(R.id.ivExpandCollapseIcon)
        if (expandIconImageView != null) {
            expandIconImageView.rotation = if (isExpanded) 180f else 0f
        } else {
            // Fallback: Si no existe el ID, intentar con el hijo directo
            val childImageView = if (holder.ivExpand.childCount > 0) {
                holder.ivExpand.getChildAt(0) as? ImageView
            } else null
            childImageView?.rotation = if (isExpanded) 180f else 0f
        }

        // Verificar si el estudiante tiene alguna observación
        val hasAnyObservation = observations.any { it.student_id == student.id }
        holder.ivGlobalObservation.visibility = if (hasAnyObservation) View.VISIBLE else View.GONE

        // Mostrar/ocultar contenido expandible
        holder.llExpandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // ✅ CAMBIO IMPORTANTE: Click en toda la card para expandir/colapsar
        val clickListener = View.OnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                if (expandedPositions.contains(currentPosition)) {
                    expandedPositions.remove(currentPosition)
                    Log.d("ReportAdapter", "Colapsando estudiante en posición $currentPosition")
                } else {
                    expandedPositions.add(currentPosition)
                    Log.d("ReportAdapter", "Expandiendo estudiante en posición $currentPosition")
                }
                notifyItemChanged(currentPosition)
            }
        }

        // Aplicar el click listener a múltiples elementos
        holder.cardView.setOnClickListener(clickListener)
        holder.itemView.setOnClickListener(clickListener)
        holder.ivExpand.setOnClickListener(clickListener)

        // Solo renderizar el contenido si está expandido (optimización)
        if (isExpanded) {
            Log.d("ReportAdapter", "Renderizando detalles del estudiante: ${student.full_name}")
            renderStudentDetails(holder, student)
        } else {
            holder.llSessions.removeAllViews()
        }
    }

    private fun renderStudentDetails(holder: StudentViewHolder, student: ConsolidatedStudent) {
        holder.llSessions.removeAllViews()
        val sortedSessions = sessions.sortedBy { it.number }

        sortedSessions.forEach { session ->
            val sessionView = createSessionView(holder, student, session)
            holder.llSessions.addView(sessionView)
        }
        setupFinalAverageEditor(holder, student)
    }

    private fun createSessionView(
        holder: StudentViewHolder,
        student: ConsolidatedStudent,
        session: ConsolidatedSession
    ): View {
        val context = holder.itemView.context
        val sessionView = LayoutInflater.from(context)
            .inflate(R.layout.item_report_session, holder.llSessions, false)

        val tvSessionName = sessionView.findViewById<TextView>(R.id.tvSessionName)
        val llAbilities = sessionView.findViewById<LinearLayout>(R.id.llAbilities)

        tvSessionName.text = session.title ?: "Sesión ${session.number}"

        Log.d("ReportAdapter", "Sesión ${session.id}: ${session.title}")

        // Filtrar competencias por esta sesión
        val competenciesInSession = competencies.filter { it.session_id == session.id }

        if (competenciesInSession.isEmpty()) {
            // Mostrar mensaje si no hay competencias en esta sesión
            val emptyView = TextView(context).apply {
                text = "No hay competencias evaluadas en esta sesión"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
                setPadding(16, 16, 16, 16)
            }
            llAbilities.addView(emptyView)
        } else {
            // Solo mostrar capacidades de las competencias de esta sesión
            competenciesInSession.forEach { competency ->
                val abilitiesInCompetency = abilitiesByCompetency[competency.id] ?: emptyList()

                abilitiesInCompetency.forEach { ability ->
                    // Verificar si esta capacidad tiene criterios evaluados para este estudiante
                    val criteriaForAbility = criteriaByAbility[ability.id] ?: emptyList()
                    val criteriaInSession = criteriaForAbility.filter { criterion ->
                        values.any { value ->
                            value.student_id == student.id &&
                                    value.criterion_id == criterion.id
                        }
                    }

                    if (criteriaInSession.isNotEmpty()) {
                        val abilityView = createAbilityView(holder, student, ability, criteriaInSession)
                        llAbilities.addView(abilityView)
                    }
                }
            }
        }

        return sessionView
    }

    private fun createAbilityView(
        holder: StudentViewHolder,
        student: ConsolidatedStudent,
        ability: ConsolidatedAbility,
        criteria: List<ConsolidatedCriterion>
    ): View {
        val context = holder.itemView.context
        val abilityView = LayoutInflater.from(context)
            .inflate(R.layout.item_report_ability, holder.llSessions, false)

        val tvAbilityName = abilityView.findViewById<TextView>(R.id.tvAbilityName)
        val llCriteria = abilityView.findViewById<LinearLayout>(R.id.llCriteria)
        val etAbilityAverage = abilityView.findViewById<EditText>(R.id.etAbilityAverage)

        tvAbilityName.text = ability.display_name

        // Renderizar criterios de esta capacidad
        llCriteria.removeAllViews()

        criteria.forEach { criterion ->
            val criterionView = createCriterionView(holder, student, ability, criterion)
            llCriteria.addView(criterionView)
        }

        // Calcular promedio automático de la capacidad
        val abilityGrades = criteria.mapNotNull { criterion ->
            values.find { it.student_id == student.id && it.criterion_id == criterion.id }?.value
        }
        val calculatedAverage = calculateAverage(abilityGrades)

        // Configurar editor de promedio de capacidad
        setupAverageEditor(etAbilityAverage, student.id, ability.id, calculatedAverage)

        return abilityView
    }

    private fun createCriterionView(
        holder: StudentViewHolder,
        student: ConsolidatedStudent,
        ability: ConsolidatedAbility,
        criterion: ConsolidatedCriterion
    ): View {
        val context = holder.itemView.context
        val criterionView = LayoutInflater.from(context)
            .inflate(R.layout.item_report_criterion, null, false)

        val tvCriterionName = criterionView.findViewById<TextView>(R.id.tvCriterionName)
        val tvCriterionGrade = criterionView.findViewById<TextView>(R.id.tvCriterionGrade)
        val ivObservation = criterionView.findViewById<ImageView>(R.id.ivObservation)

        tvCriterionName.text = "• ${criterion.display_name}"

        // Buscar la nota del criterio
        val gradeValue = values.find {
            it.student_id == student.id && it.criterion_id == criterion.id
        }?.value

        tvCriterionGrade.text = gradeValue ?: "—"

        // Aplicar color según la calificación
        applyGradeColor(tvCriterionGrade, gradeValue)

        // Verificar si hay observación para esta capacidad
        val observation = observations.find {
            it.student_id == student.id && it.ability_id == ability.id
        }

        val hasObservation = !observation?.observation.isNullOrEmpty()
        ivObservation.setImageResource(
            if (hasObservation) R.drawable.ic_note_filled else R.drawable.ic_note_empty
        )

        try {
            ivObservation.setColorFilter(
                ContextCompat.getColor(
                    context,
                    if (hasObservation) R.color.observation_active else R.color.observation_inactive
                )
            )
        } catch (e: Exception) {
            ivObservation.setColorFilter(
                android.graphics.Color.parseColor(if (hasObservation) "#FF9800" else "#CCCCCC")
            )
        }

        ivObservation.setOnClickListener {
            onObservationClick?.invoke(student.id, ability.id, criterion.id)
        }

        return criterionView
    }

    private fun setupAverageEditor(
        editText: EditText,
        studentId: Int,
        abilityId: Int?,
        calculatedAverage: String?
    ) {
        // Remover listener anterior
        val oldWatcher = editText.tag as? TextWatcher
        oldWatcher?.let { editText.removeTextChangedListener(it) }

        editText.setText(calculatedAverage ?: "")
        editText.hint = "Prom."

        // Crear nuevo TextWatcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim()?.uppercase()
                if (isValidGrade(text)) {
                    onEditAverage?.invoke(studentId, abilityId, text?.ifEmpty { null })
                }
            }
        }

        editText.addTextChangedListener(textWatcher)
        editText.tag = textWatcher
    }

    private fun setupFinalAverageEditor(holder: StudentViewHolder, student: ConsolidatedStudent) {
        val oldWatcher = holder.etFinalAverage.tag as? TextWatcher
        oldWatcher?.let { holder.etFinalAverage.removeTextChangedListener(it) }

        // Calcular promedio final automático
        val allGrades = mutableListOf<String>()

        // Solo considerar valores del estudiante actual
        values.filter { it.student_id == student.id }
            .forEach { value ->
                if (value.value in listOf("AD", "A", "B", "C")) {
                    allGrades.add(value.value)
                }
            }

        val calculatedFinal = calculateAverage(allGrades)

        holder.etFinalAverage.setText(calculatedFinal ?: "")
        holder.etFinalAverage.hint = "Promedio Final"

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim()?.uppercase()
                if (isValidGrade(text)) {
                    onEditFinalAverage?.invoke(student.id, text?.ifEmpty { null })
                }
            }
        }

        holder.etFinalAverage.addTextChangedListener(textWatcher)
        holder.etFinalAverage.tag = textWatcher
    }

    private fun calculateAverage(grades: List<String>): String? {
        if (grades.isEmpty()) return null

        val gradeValues = mapOf("AD" to 4, "A" to 3, "B" to 2, "C" to 1)
        val sum = grades.mapNotNull { gradeValues[it] }.sum()
        if (sum == 0) return null

        val average = sum.toDouble() / grades.size
        return when {
            average >= 3.5 -> "AD"
            average >= 2.5 -> "A"
            average >= 1.5 -> "B"
            else -> "C"
        }
    }

    private fun applyGradeColor(textView: TextView, grade: String?) {
        val context = textView.context
        val (bgColor, textColor) = when (grade) {
            "AD" -> R.color.grade_ad_bg to R.color.grade_ad_text
            "A" -> R.color.grade_a_bg to R.color.grade_a_text
            "B" -> R.color.grade_b_bg to R.color.grade_b_text
            "C" -> R.color.grade_c_bg to R.color.grade_c_text
            else -> android.R.color.transparent to R.color.text_secondary
        }

        try {
            textView.setBackgroundColor(ContextCompat.getColor(context, bgColor))
            textView.setTextColor(ContextCompat.getColor(context, textColor))
        } catch (e: Exception) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
    }

    private fun isValidGrade(grade: String?): Boolean {
        return grade.isNullOrEmpty() || grade in listOf("AD", "A", "B", "C")
    }

    override fun getItemCount(): Int = students.size
}