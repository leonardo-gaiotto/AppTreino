package com.leonardo.apptreino.ui.students

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.databinding.ActivityStudentFormBinding
import com.leonardo.apptreino.domain.StudentField
import com.leonardo.apptreino.domain.StudentInput
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import kotlinx.coroutines.launch

/**
 * Cadastro de aluno (modo Coach), aberto pela aba "Alunos" com Intent explícita.
 * Campos: nome, objetivo, idade, peso, altura e observações (opcional), com validação.
 */
class StudentFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentFormBinding

    private val viewModel: StudentFormViewModel by viewModels { StudentFormViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStudentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(top = true, bottom = true, includeKeyboard = true)

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupGoalChips()
        clearErrorWhenEditing(binding.inputName, StudentField.NAME)
        clearErrorWhenEditing(binding.inputAge, StudentField.AGE)
        clearErrorWhenEditing(binding.inputWeight, StudentField.WEIGHT)
        clearErrorWhenEditing(binding.inputHeight, StudentField.HEIGHT)

        binding.buttonSave.setOnClickListener { save() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errors.collect(::renderErrors)
            }
        }
    }

    /** Um chip por objetivo, criados a partir do enum (o texto vem de strings.xml). */
    private fun setupGoalChips() {
        Goal.entries.forEach { goal ->
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.view_choice_chip, binding.chipGroupGoal, false) as Chip
            chip.id = goal.chipId()
            chip.setText(goal.labelRes)
            binding.chipGroupGoal.addView(chip)
        }
        binding.chipGroupGoal.setOnCheckedStateChangeListener { _, _ -> viewModel.clearError(StudentField.GOAL) }
    }

    private fun save() {
        val input = StudentInput(
            name = binding.inputName.text.toString(),
            goal = Goal.entries.firstOrNull { it.chipId() == binding.chipGroupGoal.checkedChipId },
            age = binding.inputAge.text.toString(),
            weight = binding.inputWeight.text.toString(),
            height = binding.inputHeight.text.toString(),
            notes = binding.inputNotes.text.toString(),
        )
        if (viewModel.save(input)) {
            Toast.makeText(this, getString(R.string.message_student_saved, input.name.trim()), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun renderErrors(errors: Map<StudentField, Int>) {
        binding.inputLayoutName.showError(errors[StudentField.NAME])
        binding.inputLayoutAge.showError(errors[StudentField.AGE])
        binding.inputLayoutWeight.showError(errors[StudentField.WEIGHT])
        binding.inputLayoutHeight.showError(errors[StudentField.HEIGHT])
        binding.textGoalError.isVisible = StudentField.GOAL in errors
    }

    private fun TextInputLayout.showError(messageRes: Int?) {
        error = messageRes?.let(::getString)
    }

    private fun clearErrorWhenEditing(input: TextInputEditText, field: StudentField) {
        input.doAfterTextChanged { viewModel.clearError(field) }
    }

    /** IDs estáveis para os chips (a ordem do enum não muda). */
    private fun Goal.chipId(): Int = CHIP_ID_OFFSET + ordinal

    companion object {
        private const val CHIP_ID_OFFSET = 1_000

        /** Intent EXPLÍCITA para o cadastro. */
        fun newIntent(context: Context): Intent = Intent(context, StudentFormActivity::class.java)
    }
}
