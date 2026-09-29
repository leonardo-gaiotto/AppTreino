package com.leonardo.apptreino.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.databinding.ActivityWorkoutEditorBinding
import com.leonardo.apptreino.domain.WorkoutError
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/**
 * Editor de treino (modo Coach), aberto pelo painel do aluno com Intent explícita.
 *
 * - Nome e dias da semana (chips de segunda a domingo; dias de outro treino ficam bloqueados).
 * - Exercícios escolhidos do CATÁLOGO (outra Activity, que devolve o resultado pela
 *   Activity Result API); cada um tem séries, repetições, carga, descanso e observação.
 *   Dá para reordenar arrastando e remover.
 * - Cardio: nenhum ou vários por treino.
 * - Validação ao salvar: nome, pelo menos um dia e pelo menos um exercício ou cardio.
 */
class WorkoutEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutEditorBinding

    private val viewModel: WorkoutEditorViewModel by viewModels { WorkoutEditorViewModel.Factory }

    // Tipos explícitos: o adapter e o ItemTouchHelper se referenciam um ao outro.
    private val exerciseAdapter: EditorExerciseAdapter = EditorExerciseAdapter(
        onEdit = { item -> openPrescriptionSheet(item.catalog.id, item.exercise.id) },
        onRemove = { item -> viewModel.removeExercise(item.exercise.id) },
        onStartDrag = { holder -> itemTouchHelper.startDrag(holder) },
    )

    private val cardioAdapter = EditorCardioAdapter(
        onEdit = { cardio -> CardioSheet.newInstance(cardio.id).show(supportFragmentManager, CardioSheet.TAG) },
        onRemove = { cardio -> viewModel.removeCardio(cardio.id) },
    )

    /** Arrastar e soltar para reordenar os exercícios (só pela alça). */
    private val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                // Durante o arraste só a lista da tela muda (resposta imediata ao dedo)...
                exerciseAdapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // ...e ao soltar a nova ordem é gravada no rascunho do ViewModel.
                viewModel.reorderExercises(exerciseAdapter.currentOrder())
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        },
    )

    /** Abre o catálogo e recebe o exercício escolhido (Activity Result API). */
    private val pickExercise = registerForActivityResult(ExerciseCatalogActivity.PickExercise()) { catalogId ->
        if (catalogId != null) openPrescriptionSheet(catalogId, prescribedId = GymRepository.newId())
    }

    /** Pergunta antes de sair se há alterações não salvas. */
    private val discardChangesCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = confirmDiscard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!viewModel.isValid) {
            finish()
            return
        }

        binding = ActivityWorkoutEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(top = true, bottom = true, includeKeyboard = true)
        onBackPressedDispatcher.addCallback(this, discardChangesCallback)

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.textStudent.text = getString(R.string.editor_student, viewModel.studentName?.resolve(resources).orEmpty())

        // O EditText restaura o próprio texto após rotação; só preenchemos na primeira vez.
        if (savedInstanceState == null) binding.inputName.setText(viewModel.uiState.value.draft.name)
        binding.inputName.doAfterTextChanged { viewModel.updateName(it?.toString().orEmpty()) }

        setupDayChips()
        binding.recyclerExercises.adapter = exerciseAdapter
        itemTouchHelper.attachToRecyclerView(binding.recyclerExercises)
        binding.recyclerCardio.adapter = cardioAdapter

        binding.buttonAddExercise.setOnClickListener { pickExercise.launch(Unit) }
        binding.buttonAddCardio.setOnClickListener {
            CardioSheet.newInstance(GymRepository.newId()).show(supportFragmentManager, CardioSheet.TAG)
        }
        binding.buttonSave.setOnClickListener { save() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    /** Um chip por dia da semana (seg a dom). */
    private fun setupDayChips() {
        DayOfWeek.entries.forEach { day ->
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.view_choice_chip, binding.chipGroupDays, false) as Chip
            chip.id = CHIP_ID_OFFSET + day.value
            chip.setOnClickListener { viewModel.toggleDay(day) }
            binding.chipGroupDays.addView(chip)
        }
    }

    private fun render(state: WorkoutEditorUiState) {
        val draft = state.draft
        binding.toolbar.setTitle(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit)
        discardChangesCallback.isEnabled = state.hasChanges

        // Dias: o chip mostra o dia e, se ocupado, a letra do outro treino (ex.: "Ter · B").
        val dayNames = resources.getStringArray(R.array.week_days_abbr)
        DayOfWeek.entries.forEach { day ->
            val chip = binding.chipGroupDays.findViewById<Chip>(CHIP_ID_OFFSET + day.value)
            val takenBy = state.takenDays[day]
            chip.text = if (takenBy != null) {
                getString(R.string.editor_day_taken, dayNames[day.value - 1], takenBy.toString())
            } else {
                dayNames[day.value - 1]
            }
            chip.isEnabled = takenBy == null
            chip.isChecked = day in draft.days
        }

        // Listas (ListAdapter + DiffUtil)
        exerciseAdapter.submitList(
            draft.exercises.mapNotNull { exercise ->
                viewModel.catalogOf(exercise)?.let { EditorExerciseItem(exercise, it) }
            },
        )
        cardioAdapter.submitList(draft.cardio)
        binding.textExercisesEmpty.isVisible = draft.exercises.isEmpty()
        binding.textCardioEmpty.isVisible = draft.cardio.isEmpty()

        // Erros de validação
        binding.inputLayoutName.error =
            if (WorkoutError.NAME_REQUIRED in state.errors) getString(WorkoutError.NAME_REQUIRED.messageRes) else null
        val dayError = listOf(WorkoutError.DAYS_REQUIRED, WorkoutError.DAY_CONFLICT).firstOrNull { it in state.errors }
        binding.textDaysError.isVisible = dayError != null
        dayError?.let { binding.textDaysError.setText(it.messageRes) }
        binding.textContentError.isVisible = WorkoutError.EMPTY in state.errors
    }

    private fun save() {
        if (viewModel.save()) {
            Toast.makeText(this, R.string.message_workout_saved, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            binding.scrollEditor.smoothScrollTo(0, 0)
        }
    }

    private fun openPrescriptionSheet(catalogId: String, prescribedId: String) {
        if (ExerciseCatalog.find(catalogId) == null) return
        PrescriptionSheet.newInstance(catalogId, prescribedId).show(supportFragmentManager, PrescriptionSheet.TAG)
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_discard_title)
            .setMessage(R.string.dialog_discard_message)
            .setNegativeButton(R.string.action_keep_editing, null)
            .setPositiveButton(R.string.action_discard) { _, _ -> finish() }
            .show()
    }

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"
        const val EXTRA_WORKOUT_ID = "extra_workout_id"

        private const val CHIP_ID_OFFSET = 2_000

        /**
         * Intent EXPLÍCITA para o editor.
         * @param workoutId treino a editar, ou `null` para criar um novo.
         */
        fun newIntent(context: Context, studentId: String, workoutId: String?): Intent =
            Intent(context, WorkoutEditorActivity::class.java)
                .putExtra(EXTRA_STUDENT_ID, studentId)
                .putExtra(EXTRA_WORKOUT_ID, workoutId)
    }
}
