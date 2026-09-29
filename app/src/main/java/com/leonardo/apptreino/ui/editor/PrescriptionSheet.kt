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
import com.google.android.material.textfield.TextInputLayout
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.databinding.SheetPrescriptionBinding
import com.leonardo.apptreino.domain.PrescriptionField
import com.leonardo.apptreino.domain.PrescriptionInput
import com.leonardo.apptreino.domain.PrescriptionValidator
import com.leonardo.apptreino.ui.common.Formatters

/**
 * Formulário da prescrição de um exercício: séries, repetições (ou tempo), carga,
 * descanso e observação opcional. Usa o MESMO ViewModel do editor (activityViewModels),
 * então ao salvar o exercício aparece na lista do editor na hora.
 */
class PrescriptionSheet : BottomSheetDialogFragment() {

    private var _binding: SheetPrescriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutEditorViewModel by activityViewModels { WorkoutEditorViewModel.Factory }

    private val catalogId: String get() = requireArguments().getString(ARG_CATALOG_ID).orEmpty()
    private val prescribedId: String get() = requireArguments().getString(ARG_PRESCRIBED_ID).orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetPrescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val catalog = ExerciseCatalog.find(catalogId) ?: return dismiss()
        binding.textTarget.setText(catalog.targetRes)
        binding.textName.setText(catalog.nameRes)

        // Isométricos (ex.: prancha) usam segundos no lugar de repetições.
        binding.inputLayoutRepsMin.isVisible = !catalog.isIsometric
        binding.inputLayoutRepsMax.isVisible = !catalog.isIsometric
        binding.spaceReps.isVisible = !catalog.isIsometric
        binding.inputLayoutSeconds.isVisible = catalog.isIsometric

        // Os campos restauram o próprio texto após rotação; só preenchemos na primeira vez.
        if (savedInstanceState == null) {
            fill(viewModel.findExercise(prescribedId) ?: viewModel.defaultPrescription(catalog))
        }
        binding.switchBodyWeight.setOnCheckedChangeListener { _, _ -> updateLoadFields() }
        updateLoadFields()

        binding.buttons.buttonCancel.setOnClickListener { dismiss() }
        binding.buttons.buttonConfirm.setOnClickListener { save(catalog.isIsometric) }
    }

    private fun fill(exercise: PrescribedExercise) {
        val res = resources
        binding.inputSets.setText(Formatters.integer(res, exercise.sets))
        when (val reps = exercise.reps) {
            is Reps.Range -> {
                binding.inputRepsMin.setText(Formatters.integer(res, reps.min))
                binding.inputRepsMax.setText(Formatters.integer(res, reps.max))
            }
            is Reps.Duration -> binding.inputSeconds.setText(Formatters.integer(res, reps.seconds))
        }
        when (val load = exercise.load) {
            is Load.Kilograms -> {
                binding.inputLoad.setText(Formatters.decimal(res, load.value))
                binding.checkPerSide.isChecked = load.perSide
            }
            Load.BodyWeight -> binding.switchBodyWeight.isChecked = true
        }
        binding.inputRest.setText(Formatters.integer(res, exercise.restSeconds))
        binding.inputNote.setText(exercise.coachNote?.resolve(res))
    }

    /** Com "peso corporal" ligado, o campo de carga não se aplica. */
    private fun updateLoadFields() {
        val bodyWeight = binding.switchBodyWeight.isChecked
        binding.inputLayoutLoad.isEnabled = !bodyWeight
        binding.checkPerSide.isEnabled = !bodyWeight
        if (bodyWeight) binding.inputLayoutLoad.error = null
    }

    private fun save(isIsometric: Boolean) {
        val input = PrescriptionInput(
            sets = binding.inputSets.text.toString(),
            repsMin = binding.inputRepsMin.text.toString(),
            repsMax = binding.inputRepsMax.text.toString(),
            seconds = binding.inputSeconds.text.toString(),
            load = binding.inputLoad.text.toString(),
            isBodyWeight = binding.switchBodyWeight.isChecked,
            isPerSide = binding.checkPerSide.isChecked,
            rest = binding.inputRest.text.toString(),
            note = binding.inputNote.text.toString(),
            isIsometric = isIsometric,
        )
        val errors = PrescriptionValidator.validate(input)
        binding.inputLayoutSets.showError(errors[PrescriptionField.SETS])
        binding.inputLayoutRepsMin.showError(errors[PrescriptionField.REPS_MIN])
        binding.inputLayoutRepsMax.showError(errors[PrescriptionField.REPS_MAX])
        binding.inputLayoutSeconds.showError(errors[PrescriptionField.SECONDS])
        binding.inputLayoutLoad.showError(errors[PrescriptionField.LOAD])
        binding.inputLayoutRest.showError(errors[PrescriptionField.REST])
        if (errors.isNotEmpty()) return

        viewModel.upsertExercise(PrescriptionValidator.toPrescription(input, prescribedId, catalogId))
        dismiss()
    }

    private fun TextInputLayout.showError(messageRes: Int?) {
        error = messageRes?.let(::getString)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "prescription_sheet"
        private const val ARG_CATALOG_ID = "catalog_id"
        private const val ARG_PRESCRIBED_ID = "prescribed_id"

        fun newInstance(catalogId: String, prescribedId: String) = PrescriptionSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_CATALOG_ID, catalogId)
                putString(ARG_PRESCRIBED_ID, prescribedId)
            }
        }
    }
}
