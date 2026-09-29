package com.leonardo.apptreino.ui.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.MuscleGroup
import com.leonardo.apptreino.databinding.ActivityExerciseCatalogBinding
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import kotlinx.coroutines.launch

/**
 * Catálogo de exercícios (modo Coach), aberto pelo editor de treino com Intent explícita.
 * Busca por nome e filtro por grupo muscular; ao tocar num exercício, devolve o ID dele
 * ao editor pela Activity Result API (contrato [PickExercise]).
 */
class ExerciseCatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseCatalogBinding

    private val viewModel: CatalogViewModel by viewModels { CatalogViewModel.Factory }

    private val adapter = CatalogAdapter(onPick = ::returnResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExerciseCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(top = true, bottom = true, includeKeyboard = true)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerCatalog.adapter = adapter
        binding.inputSearch.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        setupFilterChips()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.textEmpty.isVisible = state.exercises.isEmpty()
                    adapter.submitList(state.exercises)
                }
            }
        }
    }

    /** "Todos" + um chip por grupo muscular (textos do enum, em strings.xml). */
    private fun setupFilterChips() {
        val options = listOf<MuscleGroup?>(null) + MuscleGroup.entries
        options.forEachIndexed { index, group ->
            val chip = layoutInflater.inflate(R.layout.view_choice_chip, binding.chipGroupFilter, false) as Chip
            chip.id = CHIP_ID_OFFSET + index
            chip.setText(group?.labelRes ?: R.string.catalog_filter_all)
            binding.chipGroupFilter.addView(chip)
        }
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val index = (checkedIds.firstOrNull() ?: CHIP_ID_OFFSET) - CHIP_ID_OFFSET
            viewModel.filter(options.getOrNull(index))
        }
        val selectedIndex = options.indexOf(viewModel.uiState.value.group)
        binding.chipGroupFilter.check(CHIP_ID_OFFSET + selectedIndex)
    }

    private fun returnResult(exercise: CatalogExercise) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_CATALOG_ID, exercise.id))
        finish()
    }

    /**
     * Contrato da Activity Result API: abre o catálogo (Intent EXPLÍCITA) e devolve o ID
     * do exercício escolhido, ou `null` se o coach voltar sem escolher.
     */
    class PickExercise : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(context, ExerciseCatalogActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            if (resultCode == Activity.RESULT_OK) intent?.getStringExtra(EXTRA_CATALOG_ID) else null
    }

    companion object {
        private const val EXTRA_CATALOG_ID = "extra_catalog_id"
        private const val CHIP_ID_OFFSET = 5_000
    }
}
