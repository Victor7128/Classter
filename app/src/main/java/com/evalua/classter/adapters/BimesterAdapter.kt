package com.evalua.classter.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evalua.classter.R
import com.evalua.classter.dialogs.AddGradeDialogFragment
import com.evalua.classter.models.BimesterData

class BimesterAdapter(
    private val bimesters: List<BimesterData>,
    private var expandedBimesterId: Int?,
    private val expandedGradeId: Int?,
    private val onBimesterExpanded: (Int, Boolean) -> Unit,
    private val onGradeExpanded: (Int, Boolean) -> Unit,
    private val onAddGrade: (Int, Int) -> Unit,
    private val onDeleteGrade: (Int, Int) -> Unit,
    private val onAddSection: (Int, String?) -> Unit,
    private val onDeleteSection: (Int, String) -> Unit
) : RecyclerView.Adapter<BimesterAdapter.BimesterViewHolder>() {

    companion object {
        private const val TAG = "BimesterAdapter"
    }

    inner class BimesterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivExpandIcon: ImageView = view.findViewById(R.id.ivExpandIcon)
        val tvBimester: TextView = view.findViewById(R.id.tvBimesterName)
        val rvGrades: RecyclerView = view.findViewById(R.id.recyclerViewGrades)
        val layoutGrades: LinearLayout = view.findViewById(R.id.layoutGradesContainer)
        val btnAddGrade: Button = view.findViewById(R.id.btnAddGrade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BimesterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bimester, parent, false)
        return BimesterViewHolder(view)
    }

    override fun onBindViewHolder(holder: BimesterViewHolder, position: Int) {
        val bimester = bimesters[position]
        holder.tvBimester.text = "${bimester.name} Bimestre"

        Log.d(TAG, "Binding bimester ${bimester.id}, expandedBimesterId: $expandedBimesterId")

        val gradeAdapter = GradeAdapter(
            grades = bimester.grades,
            bimesterName = bimester.name,
            expandedGradeId = expandedGradeId,
            onGradeExpanded = onGradeExpanded,
            onDeleteGrade = { gradeId, gradeNumber ->
                onDeleteGrade(gradeId, gradeNumber)
            },
            onAddSection = { gradeId, letter ->
                onAddSection(gradeId, letter)
            },
            onDeleteSection = { sectionId, sectionLetter ->
                onDeleteSection(sectionId, sectionLetter)
            }
        )
        holder.rvGrades.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvGrades.adapter = gradeAdapter

        val isExpanded = bimester.id == expandedBimesterId
        Log.d(TAG, "Bimester ${bimester.id} isExpanded: $isExpanded")

        updateBimesterUI(holder, isExpanded)

        val headerLayout = holder.itemView.findViewById<View>(R.id.layoutBimesterHeader)
        headerLayout.setOnClickListener {
            val currentlyExpanded = bimester.id == expandedBimesterId
            val newState = !currentlyExpanded

            Log.d(TAG, "Bimester ${bimester.id} clicked, changing from $currentlyExpanded to $newState")

            val oldExpandedId = expandedBimesterId
            expandedBimesterId = if (newState) bimester.id else null

            updateBimesterUI(holder, newState)

            if (oldExpandedId != null && oldExpandedId != bimester.id) {
                val oldPosition = bimesters.indexOfFirst { it.id == oldExpandedId }
                if (oldPosition != -1) {
                    notifyItemChanged(oldPosition)
                }
            }
            onBimesterExpanded(bimester.id, newState)
        }

        holder.btnAddGrade.setOnClickListener {
            val activity = holder.itemView.context as? AppCompatActivity
            activity?.let {
                val dialog = AddGradeDialogFragment(
                    existingGrades = bimester.grades
                ) { gradeNumber ->
                    onAddGrade(bimester.id, gradeNumber)
                }
                dialog.show(it.supportFragmentManager, "AddGradeDialog")
            }
        }
    }

    private fun updateBimesterUI(holder: BimesterViewHolder, isExpanded: Boolean) {
        holder.layoutGrades.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpandIcon.rotation = if (isExpanded) 180f else 0f
    }

    override fun getItemCount() = bimesters.size
}