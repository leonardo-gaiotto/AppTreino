package com.leonardo.apptreino.ui.exercise

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemSetLogBinding
import com.leonardo.apptreino.domain.Comparison
import com.leonardo.apptreino.domain.SetComparator
import com.leonardo.apptreino.ui.common.Formatters
import kotlin.math.abs

/**
 * Lista de séries do registro do aluno (ListAdapter + DiffUtil).
 * Cada linha mostra o que o coach pediu ao lado do que o aluno fez e, depois de
 * registrada, destaca se ficou acima, no alvo ou abaixo do previsto.
 */
class SetLogAdapter(
    private val onChangeReps: (index: Int, direction: Int) -> Unit,
    private val onChangeLoad: (index: Int, direction: Int) -> Unit,
    private val onToggleSet: (index: Int) -> Unit,
) : ListAdapter<SetRow, SetLogAdapter.SetViewHolder>(SetRowDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder =
        SetViewHolder(ItemSetLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SetViewHolder(private val binding: ItemSetLogBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.stepperReps.buttonMinus.setOnClickListener { withIndex { onChangeReps(it, -1) } }
            binding.stepperReps.buttonPlus.setOnClickListener { withIndex { onChangeReps(it, +1) } }
            binding.stepperLoad.buttonMinus.setOnClickListener { withIndex { onChangeLoad(it, -1) } }
            binding.stepperLoad.buttonPlus.setOnClickListener { withIndex { onChangeLoad(it, +1) } }
            binding.buttonRegisterSet.setOnClickListener { withIndex(onToggleSet) }
        }

        fun bind(row: SetRow) {
            val context = binding.root.context
            val res = context.resources
            val setNumber = row.index + 1

            binding.textSetNumber.text = res.getString(R.string.set_number, setNumber)
            binding.textCoachTarget.text = res.getString(R.string.set_coach_target, Formatters.setTarget(res, row.target))

            // Repetições (ou segundos)
            binding.stepperReps.textStepperValue.text =
                if (row.isIsometric) Formatters.duration(res, row.reps) else Formatters.integer(res, row.reps)
            binding.stepperReps.textStepperUnit.setText(if (row.isIsometric) R.string.unit_time else R.string.unit_reps)
            binding.stepperReps.buttonMinus.contentDescription = res.getString(R.string.cd_decrease_reps, setNumber)
            binding.stepperReps.buttonPlus.contentDescription = res.getString(R.string.cd_increase_reps, setNumber)

            // Carga (escondida em exercícios com o peso do corpo)
            binding.stepperLoad.root.isVisible = row.showsLoad
            binding.spaceSteppers.isVisible = row.showsLoad
            binding.stepperLoad.textStepperValue.text = Formatters.decimal(res, row.loadKg)
            binding.stepperLoad.textStepperUnit.setText(R.string.unit_kg)
            binding.stepperLoad.buttonMinus.contentDescription = res.getString(R.string.cd_decrease_load, setNumber)
            binding.stepperLoad.buttonPlus.contentDescription = res.getString(R.string.cd_increase_load, setNumber)

            // Registrar / desfazer
            binding.imageRegisterSet.isActivated = row.isRegistered
            binding.buttonRegisterSet.contentDescription = res.getString(
                if (row.isRegistered) R.string.cd_unregister_set else R.string.cd_register_set, setNumber,
            )
            binding.cardSet.strokeColor = ContextCompat.getColor(
                context, if (row.isRegistered) R.color.brand_lime else R.color.color_outline,
            )

            bindComparison(row)
        }

        /** Chips "acima / no alvo / abaixo" do previsto, visíveis só depois de registrar. */
        private fun bindComparison(row: SetRow) {
            val res = binding.root.resources
            binding.layoutComparison.isVisible = row.isRegistered
            if (!row.isRegistered) return

            val repsComparison = SetComparator.reps(row.reps, row.target.reps)
            val repsDelta = abs(SetComparator.repsDelta(row.reps, row.target.reps))
            binding.textRepsDelta.text = when {
                repsComparison == Comparison.ON_TARGET ->
                    res.getString(if (row.isIsometric) R.string.delta_time_on_target else R.string.delta_reps_on_target)
                row.isIsometric -> res.getString(
                    if (repsComparison == Comparison.ABOVE) R.string.delta_seconds_up else R.string.delta_seconds_down,
                    repsDelta,
                )
                else -> res.getQuantityString(
                    if (repsComparison == Comparison.ABOVE) R.plurals.delta_reps_up else R.plurals.delta_reps_down,
                    repsDelta, repsDelta,
                )
            }
            binding.textRepsDelta.applyComparisonColors(repsComparison)

            binding.textLoadDelta.isVisible = row.showsLoad
            if (row.showsLoad) {
                val loadComparison = SetComparator.load(row.loadKg, row.target.load)
                val loadDelta = Formatters.decimal(res, abs(SetComparator.loadDelta(row.loadKg, row.target.load)))
                binding.textLoadDelta.text = when (loadComparison) {
                    Comparison.ABOVE -> res.getString(R.string.delta_load_up, loadDelta)
                    Comparison.BELOW -> res.getString(R.string.delta_load_down, loadDelta)
                    Comparison.ON_TARGET -> res.getString(R.string.delta_load_on_target)
                }
                binding.textLoadDelta.applyComparisonColors(loadComparison)
            }
        }

        private fun TextView.applyComparisonColors(comparison: Comparison) {
            @ColorRes val text: Int
            @ColorRes val background: Int
            when (comparison) {
                Comparison.ABOVE -> { text = R.color.color_accent; background = R.color.color_accent_soft }
                Comparison.BELOW -> { text = R.color.color_warning; background = R.color.color_warning_soft }
                Comparison.ON_TARGET -> { text = R.color.color_text_secondary; background = R.color.color_surface_variant }
            }
            setTextColor(ContextCompat.getColor(context, text))
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, background))
        }

        private fun withIndex(action: (Int) -> Unit) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) action(getItem(position).index)
        }
    }
}

object SetRowDiffCallback : DiffUtil.ItemCallback<SetRow>() {
    override fun areItemsTheSame(oldItem: SetRow, newItem: SetRow): Boolean = oldItem.index == newItem.index
    override fun areContentsTheSame(oldItem: SetRow, newItem: SetRow): Boolean = oldItem == newItem
}
