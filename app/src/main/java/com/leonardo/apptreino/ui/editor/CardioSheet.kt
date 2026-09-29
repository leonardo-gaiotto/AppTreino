package com.leonardo.apptreino.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.CardioType
import com.leonardo.apptreino.data.model.Intensity
import com.leonardo.apptreino.databinding.SheetCardioBinding
import com.leonardo.apptreino.domain.CardioField
import com.leonardo.apptreino.domain.CardioInput
import com.leonardo.apptreino.domain.CardioValidator
import com.leonardo.apptreino.ui.common.Formatters

/**
 * Formulário de cardio: tipo (esteira, bicicleta...), duração, intensidade e observação.
 * Compartilha o ViewModel do editor de treino (activityViewModels).
 */
class CardioSheet : BottomSheetDialogFragment() {

    private var _binding: SheetCardioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutEditorViewModel by activityViewModels { WorkoutEditorViewModel.Factory }

    private val cardioId: String get() = requireArguments().getString(ARG_CARDIO_ID).orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetCardioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        addChips(binding.chipGroupType, CardioType.entries.map { it.labelRes }, TYPE_CHIP_OFFSET)
        addChips(binding.chipGroupIntensity, Intensity.entries.map { it.labelRes }, INTENSITY_CHIP_OFFSET)
        binding.chipGroupType.setOnCheckedStateChangeListener { _, _ -> binding.textTypeError.isVisible = false }
        binding.chipGroupIntensity.setOnCheckedStateChangeListener { _, _ -> binding.textIntensityError.isVisible = false }

        // Os campos restauram o próprio estado após rotação; só preenchemos na primeira vez.
        if (savedInstanceState == null) {
            val existing = viewModel.findCardio(cardioId)
            binding.chipGroupType.check(TYPE_CHIP_OFFSET + (existing?.type ?: CardioType.TREADMILL).ordinal)
            binding.chipGroupIntensity.check(INTENSITY_CHIP_OFFSET + (existing?.intensity ?: Intensity.MODERATE).ordinal)
            binding.inputDuration.setText(Formatters.integer(resources, existing?.durationMinutes ?: DEFAULT_MINUTES))
            binding.inputNote.setText(existing?.note?.resolve(resources))
        }

        binding.buttons.buttonCancel.setOnClickListener { dismiss() }
        binding.buttons.buttonConfirm.setOnClickListener { save() }
    }

    private fun addChips(group: ChipGroup, labels: List<Int>, idOffset: Int) {
        labels.forEachIndexed { index, labelRes ->
            val chip = layoutInflater.inflate(R.layout.view_choice_chip, group, false) as Chip
            chip.id = idOffset + index
            chip.setText(labelRes)
            group.addView(chip)
        }
    }

    private fun save() {
        val input = CardioInput(
            type = CardioType.entries.getOrNull(binding.chipGroupType.checkedChipId - TYPE_CHIP_OFFSET),
            duration = binding.inputDuration.text.toString(),
            intensity = Intensity.entries.getOrNull(binding.chipGroupIntensity.checkedChipId - INTENSITY_CHIP_OFFSET),
            note = binding.inputNote.text.toString(),
        )
        val errors = CardioValidator.validate(input)
        binding.textTypeError.isVisible = CardioField.TYPE in errors
        binding.textIntensityError.isVisible = CardioField.INTENSITY in errors
        binding.inputLayoutDuration.error = errors[CardioField.DURATION]?.let(::getString)
        if (errors.isNotEmpty()) return

        viewModel.upsertCardio(CardioValidator.toCardio(input, cardioId))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "cardio_sheet"
        private const val ARG_CARDIO_ID = "cardio_id"
        private const val TYPE_CHIP_OFFSET = 3_000
        private const val INTENSITY_CHIP_OFFSET = 4_000
        private const val DEFAULT_MINUTES = 15

        fun newInstance(cardioId: String) = CardioSheet().apply {
            arguments = Bundle().apply { putString(ARG_CARDIO_ID, cardioId) }
        }
    }
}
