package com.evalua.classter.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.evrencoskun.tableview.adapter.AbstractTableAdapter
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder
import com.evalua.classter.R
import com.evalua.classter.models.*
import androidx.core.graphics.toColorInt

class ReportsTableAdapter(
    private val context: Context,
    private val onGradeChanged: (studentId: Int, abilityId: Int?, grade: String?) -> Unit,
    private val onObservationClicked: (studentId: Int, criterionId: Int) -> Unit
) : AbstractTableAdapter<ReportColumnHeaderModel, ReportRowHeaderModel, ReportCellModel>() {

    // ✅ ViewHolders limpios
    class ReportColumnHeaderViewHolder(itemView: View) : AbstractViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        // ❌ ELIMINADO: tvLevel (no lo usamos)
    }

    class ReportRowHeaderViewHolder(itemView: View) : AbstractViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }

    class ReportCellViewHolder(itemView: View) : AbstractViewHolder(itemView) {
        val tvGrade: TextView? = itemView.findViewById(R.id.tvGrade)
        val etGrade: EditText? = itemView.findViewById(R.id.etGrade)
        val ivObservation: ImageView? = itemView.findViewById(R.id.ivObservation)
    }

    // ✅ Create ViewHolders con layouts específicos
    override fun onCreateColumnHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layoutId = when (viewType) {
            1 -> R.layout.report_column_header_session    // Nivel 1: Sesión
            2 -> R.layout.report_column_header_competency // Nivel 2: Competencia
            3 -> R.layout.report_column_header_ability    // Nivel 3: Capacidad
            else -> R.layout.report_column_header_criterion // Nivel 4: Criterio/Obs
        }
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return ReportColumnHeaderViewHolder(view)
    }

    override fun onCreateRowHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.report_row_header, parent, false)
        return ReportRowHeaderViewHolder(view)
    }

    override fun onCreateCellViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layoutId = when (viewType) {
            1 -> R.layout.report_cell_editable    // Celda editable (promedios)
            2 -> R.layout.report_cell_observation // Celda observación
            else -> R.layout.report_cell_grade    // Celda nota normal
        }
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return ReportCellViewHolder(view)
    }

    // ✅ Bind Column Headers con multinivel
    override fun onBindColumnHeaderViewHolder(
        holder: AbstractViewHolder,
        columnHeaderItemModel: ReportColumnHeaderModel?,
        columnPosition: Int
    ) {
        val headerViewHolder = holder as ReportColumnHeaderViewHolder

        if (columnHeaderItemModel != null) {
            // ✅ Establecer título
            headerViewHolder.tvTitle.text = columnHeaderItemModel.title

            // ✅ Configurar colores según nivel y tipo
            val (backgroundColor, textColor) = getHeaderColors(columnHeaderItemModel)

            headerViewHolder.itemView.setBackgroundColor(
                backgroundColor.toColorInt()
            )
            headerViewHolder.tvTitle.setTextColor(
                textColor.toColorInt()
            )

            // ✅ Configurar tamaño de texto según nivel
            val textSize = when (columnHeaderItemModel.level) {
                1 -> 14f // Sesión
                2 -> 12f // Competencia
                3 -> 11f // Capacidad
                else -> 10f // Criterio
            }
            headerViewHolder.tvTitle.textSize = textSize

            // ✅ Configurar padding según nivel
            val padding = when (columnHeaderItemModel.level) {
                1 -> 16
                2 -> 12
                3 -> 10
                else -> 8
            }
            headerViewHolder.itemView.setPadding(padding, padding, padding, padding)
        }
    }

    override fun onBindRowHeaderViewHolder(
        holder: AbstractViewHolder,
        rowHeaderItemModel: ReportRowHeaderModel?,
        rowPosition: Int
    ) {
        val rowHeaderViewHolder = holder as ReportRowHeaderViewHolder

        if (rowHeaderItemModel != null) {
            rowHeaderViewHolder.tvNumber.text = rowHeaderItemModel.number.toString()
            rowHeaderViewHolder.tvName.text = rowHeaderItemModel.title
        }
    }

    // ✅ Bind Cells con edición y observaciones
    override fun onBindCellViewHolder(
        holder: AbstractViewHolder,
        cellItemModel: ReportCellModel?,
        columnPosition: Int,
        rowPosition: Int
    ) {
        val cellViewHolder = holder as ReportCellViewHolder

        if (cellItemModel != null) {
            when {
                // ✅ CELDA DE OBSERVACIÓN
                cellItemModel.isObservation -> {
                    cellViewHolder.ivObservation?.let { icon ->
                        // ✅ Usar drawables existentes o crear temporales
                        try {
                            icon.setImageResource(
                                if (cellItemModel.hasObservation) R.drawable.ic_note_filled
                                else R.drawable.ic_note_empty
                            )
                        } catch (e: Exception) {
                            // ✅ Fallback si no existen los drawables
                            icon.setImageResource(android.R.drawable.ic_menu_edit)
                        }

                        try {
                            icon.setColorFilter(
                                ContextCompat.getColor(context,
                                    if (cellItemModel.hasObservation) R.color.observation_active
                                    else R.color.observation_inactive
                                )
                            )
                        } catch (e: Exception) {
                            // ✅ Fallback colors
                            icon.setColorFilter(
                                if (cellItemModel.hasObservation)
                                    "#FF9800".toColorInt()
                                else
                                    "#CCCCCC".toColorInt()
                            )
                        }

                        icon.setOnClickListener {
                            onObservationClicked(cellItemModel.studentId, cellItemModel.criterionId ?: -1)
                        }
                    }

                    // Fondo para observaciones
                    cellViewHolder.itemView.setBackgroundColor(
                        "#FFF8E1".toColorInt()
                    )
                }

                // ✅ CELDA EDITABLE (Promedios)
                cellItemModel.isEditable -> {
                    cellViewHolder.etGrade?.let { editText ->
                        // ✅ Limpiar listeners anteriores para evitar callbacks múltiples
                        val oldWatcher = editText.tag as? TextWatcher
                        oldWatcher?.let { editText.removeTextChangedListener(it) }
                        editText.tag = null

                        // ✅ Establecer valor actual (vacío para promedios)
                        editText.setText(cellItemModel.grade ?: "")

                        // ✅ Configurar hint
                        editText.hint = when {
                            cellItemModel.isPromedioFinal -> "Prom."
                            cellItemModel.isPromedio -> "Prom."
                            else -> ""
                        }

                        // ✅ TextWatcher para cambios
                        val textWatcher = object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                            override fun afterTextChanged(s: Editable?) {
                                val newGrade = s?.toString()?.trim()?.uppercase()
                                if (isValidGrade(newGrade)) {
                                    onGradeChanged(
                                        cellItemModel.studentId,
                                        cellItemModel.abilityId,
                                        newGrade?.ifEmpty { null }
                                    )
                                }
                            }
                        }

                        editText.addTextChangedListener(textWatcher)
                        editText.tag = textWatcher

                        // ✅ Estilo para celdas editables
                        editText.setBackgroundColor(
                            android.graphics.Color.parseColor(
                                if (cellItemModel.isPromedioFinal) "#FFF3E0"
                                else "#E3F2FD"
                            )
                        )
                    }
                }

                // ✅ CELDA NORMAL (Notas)
                else -> {
                    cellViewHolder.tvGrade?.let { textView ->
                        textView.text = cellItemModel.grade ?: ""

                        // ✅ Colores según calificación
                        val (backgroundColor, textColor) = getGradeColors(cellItemModel.grade)
                        cellViewHolder.itemView.setBackgroundColor(backgroundColor)
                        textView.setTextColor(textColor)
                    }
                }
            }
        }
    }

    // ✅ Corner view
    override fun onCreateCornerView(parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.report_corner, parent, false)
        return view
    }

    // ✅ ViewTypes para diferentes layouts
    override fun getColumnHeaderItemViewType(position: Int): Int {
        val header = mColumnHeaderItems?.get(position)
        return header?.level ?: 0
    }

    override fun getRowHeaderItemViewType(position: Int): Int = 0

    override fun getCellItemViewType(columnPosition: Int): Int {
        val columnHeader = mColumnHeaderItems?.get(columnPosition)
        return when {
            columnHeader?.isObservation == true -> 2 // Observación
            columnHeader?.isEditable == true -> 1    // Editable
            else -> 0 // Normal
        }
    }

    // ✅ FUNCIONES AUXILIARES
    private fun getHeaderColors(header: ReportColumnHeaderModel): Pair<String, String> {
        return when {
            header.isPromedioFinal -> "#FF9800" to "#FFFFFF"
            header.isPromedio -> "#2196F3" to "#FFFFFF"
            header.isObservation -> "#FFC107" to "#333333"
            header.level == 1 -> "#E0E0E0" to "#333333" // Sesión
            header.level == 2 -> "#C8E6C9" to "#2E7D32" // Competencia
            header.level == 3 -> "#BBDEFB" to "#1565C0" // Capacidad
            else -> "#F5F5F5" to "#333333" // Criterio
        }
    }

    private fun getGradeColors(grade: String?): Pair<Int, Int> {
        return when (grade) {
            "AD" -> "#C8E6C9".toColorInt() to "#2E7D32".toColorInt()
            "A" -> "#BBDEFB".toColorInt() to "#1565C0".toColorInt()
            "B" -> "#FFE0B2".toColorInt() to "#EF6C00".toColorInt()
            "C" -> "#FFCDD2".toColorInt() to "#C62828".toColorInt()
            else -> android.graphics.Color.WHITE to "#666666".toColorInt()
        }
    }

    private fun isValidGrade(grade: String?): Boolean {
        return grade.isNullOrEmpty() || grade.uppercase() in listOf("AD", "A", "B", "C")
    }
}