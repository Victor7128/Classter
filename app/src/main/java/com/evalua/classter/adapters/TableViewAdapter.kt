package com.evalua.classter.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.evrencoskun.tableview.adapter.AbstractTableAdapter
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder
import com.evalua.classter.R
import com.evalua.classter.models.*
import androidx.core.graphics.toColorInt

class TableViewAdapter(
    private val context: Context,
    private val onGradeChanged: (studentId: Int, criterionId: Int, grade: String?) -> Unit,
    private val onObservationClicked: (studentId: Int, criterionId: Int) -> Unit
) : AbstractTableAdapter<ColumnHeaderModel, RowHeaderModel, CellModel>() {
    private val gradeOptions = listOf("Sin eval", "AD", "A", "B", "C")

    // ✅ ViewHolders
    class ColumnHeaderViewHolder(itemView: View) : AbstractViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.column_header_textview)
    }

    class RowHeaderViewHolder(itemView: View) : AbstractViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.row_header_textview)
    }

    class CellViewHolder(itemView: View) : AbstractViewHolder(itemView) {
        val spinnerGrade: Spinner = itemView.findViewById(R.id.spinnerGrade)
        val btnObservation: TextView = itemView.findViewById(R.id.btnObservation)
    }

    // ✅ Create ViewHolders (SIN modificar layoutParams)
    override fun onCreateColumnHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.table_column_header, parent, false)
        return ColumnHeaderViewHolder(view)
    }

    override fun onCreateRowHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.table_row_header, parent, false)
        return RowHeaderViewHolder(view)
    }

    override fun onCreateCellViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.table_cell, parent, false)
        return CellViewHolder(view)
    }

    override fun onBindColumnHeaderViewHolder(
        holder: AbstractViewHolder,
        columnHeaderItemModel: ColumnHeaderModel?,
        columnPosition: Int
    ) {
        val columnHeaderViewHolder = holder as ColumnHeaderViewHolder
        val title = columnHeaderItemModel?.title ?: ""
        columnHeaderViewHolder.textView.text = title

        // ✅ Solo establecer el texto, NO modificar layoutParams
    }

    override fun onBindRowHeaderViewHolder(
        holder: AbstractViewHolder,
        rowHeaderItemModel: RowHeaderModel?,
        rowPosition: Int
    ) {
        val rowHeaderViewHolder = holder as RowHeaderViewHolder
        val title = rowHeaderItemModel?.title ?: ""
        rowHeaderViewHolder.textView.text = title

        // ✅ Solo establecer el texto, NO modificar layoutParams
    }

    override fun onBindCellViewHolder(
        holder: AbstractViewHolder,
        cellItemModel: CellModel?,
        columnPosition: Int,
        rowPosition: Int
    ) {
        val cellViewHolder = holder as CellViewHolder

        if (cellItemModel != null) {
            // ✅ Configurar Spinner con layout personalizado
            val adapter = ArrayAdapter(context, R.layout.spinner_evaluation_item, gradeOptions)
            adapter.setDropDownViewResource(R.layout.spinner_evaluation_item)
            cellViewHolder.spinnerGrade.adapter = adapter

            // ✅ Establecer valor actual SIN disparar eventos
            val selectedIndex = cellItemModel.currentValue?.let {
                gradeOptions.indexOf(it)
            } ?: 0
            cellViewHolder.spinnerGrade.setSelection(selectedIndex, false)

            // ✅ Configurar listener DESPUÉS
            cellViewHolder.spinnerGrade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedGrade = if (position > 0) gradeOptions[position] else null

                    if (selectedGrade != cellItemModel.currentValue) {
                        onGradeChanged(cellItemModel.studentId, cellItemModel.criterionId, selectedGrade)
                        updateCellColor(cellViewHolder.itemView, selectedGrade)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // ✅ Configurar observación
            cellViewHolder.btnObservation.text = if (cellItemModel.hasObservation) "📝" else "💭"
            cellViewHolder.btnObservation.setOnClickListener {
                onObservationClicked(cellItemModel.studentId, cellItemModel.criterionId)
            }

            // ✅ Color según calificación
            updateCellColor(cellViewHolder.itemView, cellItemModel.currentValue)
        }
    }

    private fun updateCellColor(view: View, value: String?) {
        val backgroundColor = when (value) {
            "AD" -> "#E8F5E8".toColorInt() // Verde claro
            "A" -> "#E3F2FD".toColorInt() // Azul claro
            "B" -> "#FFF3E0".toColorInt() // Naranja claro
            "C" -> "#FFEBEE".toColorInt() // Rojo claro
            else -> android.graphics.Color.WHITE
        }
        view.setBackgroundColor(backgroundColor)
    }

    override fun onCreateCornerView(parent: ViewGroup): View {
        val view = TextView(context).apply {
            text = "ESTUDIANTES"
            layoutParams = ViewGroup.LayoutParams(450, 80) // ✅ Altura aumentada a 80
            setBackgroundColor("#E8EAF6".toColorInt())
            gravity = android.view.Gravity.CENTER
            setPadding(12, 12, 12, 12)
            textSize = 12f
            setTextColor("#1976D2".toColorInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
        }
        return view
    }

    override fun getColumnHeaderItemViewType(position: Int): Int = 0
    override fun getRowHeaderItemViewType(position: Int): Int = 0
    override fun getCellItemViewType(columnPosition: Int): Int = 0
}