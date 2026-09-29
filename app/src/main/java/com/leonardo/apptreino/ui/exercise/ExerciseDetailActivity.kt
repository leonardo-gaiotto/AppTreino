package com.leonardo.apptreino.ui.exercise

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.databinding.ActivityExerciseDetailBinding
import com.leonardo.apptreino.databinding.ItemMistakeBinding
import com.leonardo.apptreino.databinding.ItemStepBinding
import com.leonardo.apptreino.databinding.ViewStatTileBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.loadVideoThumbnail
import com.leonardo.apptreino.util.YouTube
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Tela de detalhe do exercício (2ª tela).
 *
 * É aberta pela aba "Hoje" com uma INTENT EXPLÍCITA — ou seja, informando exatamente
 * qual classe abrir — criada por [newIntent]. Mostra miniatura grande do vídeo,
 * prescrição do coach, o REGISTRO das séries, passo a passo, erros comuns e a
 * observação do coach.
 */
class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseDetailBinding

    private val viewModel: ExerciseDetailViewModel by viewModels { ExerciseDetailViewModel.Factory }

    private val setAdapter = SetLogAdapter(
        onChangeReps = { index, direction -> viewModel.changeReps(index, direction) },
        onChangeLoad = { index, direction -> viewModel.changeLoad(index, direction) },
        onToggleSet = { index -> viewModel.toggleSet(index) },
    )

    /** O conteúdo fixo (vídeo, passos, erros) é desenhado uma única vez. */
    private var staticContentBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ícones claros na barra de status, pois ela fica sobre a miniatura escura.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        if (viewModel.uiState.value == null) {
            // Proteção: ID inválido na Intent.
            Toast.makeText(this, R.string.error_exercise_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        binding.recyclerSets.adapter = setAdapter
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonDone.setOnClickListener { button ->
            button.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            viewModel.toggleAll()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // O coach pode ter removido o exercício do treino enquanto a tela estava aberta.
                    if (state == null) finish() else render(state)
                }
            }
        }
    }

    /** A miniatura vai até o topo da tela; botão voltar e barra inferior respeitam as barras do sistema. */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            binding.buttonBack.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = bars.top + resources.getDimensionPixelSize(R.dimen.space_sm)
            }
            binding.layoutBottomBar.updatePadding(
                bottom = bars.bottom + resources.getDimensionPixelSize(R.dimen.space_md),
            )
            binding.root.updatePadding(left = bars.left, right = bars.right)
            insets
        }
    }

    private fun render(state: ExerciseDetailUiState) {
        if (!staticContentBound) {
            bindStaticContent(state.catalog)
            staticContentBound = true
        }
        val exercise = state.exercise

        // Posição no treino
        binding.textPosition.text = getString(
            R.string.detail_position,
            state.position,
            state.workout.exercises.size,
            Formatters.workoutTitle(resources, state.letter),
        )

        // Prescrição do coach (pode mudar ao vivo se o coach editar o treino)
        binding.statSets.bind(R.string.stat_sets, Formatters.integer(resources, exercise.sets))
        binding.statReps.bind(
            if (exercise.reps is Reps.Duration) R.string.stat_duration else R.string.stat_reps,
            Formatters.reps(resources, exercise.reps),
        )
        binding.statLoad.bind(R.string.stat_load, Formatters.load(resources, exercise.load))
        binding.statRest.bind(R.string.stat_rest, Formatters.duration(resources, exercise.restSeconds))

        // Observação do coach: só aparece quando existe.
        binding.layoutCoachNote.isVisible = exercise.coachNote != null
        binding.textCoachNote.text = exercise.coachNote?.resolve(resources)

        // Registro das séries
        binding.textSetsRegistered.text = resources.getQuantityString(
            R.plurals.sets_logged_short, exercise.sets, state.registeredSets, exercise.sets,
        )
        setAdapter.submitList(state.sets)
        renderDoneButton(state.isDone)
    }

    private fun bindStaticContent(catalog: CatalogExercise) {
        val name = getString(catalog.nameRes)

        binding.imageHero.loadVideoThumbnail(catalog.youtubeVideoId, YouTube.Thumbnail.HD)
        binding.imageHero.contentDescription = getString(R.string.cd_video_thumbnail, name)
        binding.buttonPlayHero.contentDescription = getString(R.string.cd_play_video, name)
        binding.buttonPlayHero.setOnClickListener { openVideo(catalog) }
        binding.buttonWatch.setOnClickListener { openVideo(catalog) }
        binding.textMuscle.setText(catalog.targetRes)
        binding.textName.text = name

        // Listas de passos e erros: poucos itens fixos, então inflamos direto num LinearLayout.
        resources.getStringArray(catalog.stepsRes).forEachIndexed { index, step ->
            ItemStepBinding.inflate(layoutInflater, binding.layoutSteps, true).apply {
                textStepNumber.text = Formatters.integer(resources, index + 1)
                textStep.text = step
            }
        }
        resources.getStringArray(catalog.commonMistakesRes).forEach { mistake ->
            ItemMistakeBinding.inflate(layoutInflater, binding.layoutMistakes, true).apply {
                textMistake.text = mistake
            }
        }
    }

    private fun openVideo(catalog: CatalogExercise) {
        if (!YouTube.openVideo(this, catalog.youtubeVideoId)) {
            Toast.makeText(this, R.string.error_open_video, Toast.LENGTH_LONG).show()
        }
    }

    /** Botão principal: "Concluir exercício" (verde-limão) ou "Feito" (discreto, toque desfaz). */
    private fun renderDoneButton(isDone: Boolean) {
        val button = binding.buttonDone
        val background = if (isDone) R.color.color_surface_variant else R.color.brand_lime
        val content = if (isDone) R.color.color_accent else R.color.color_on_accent

        button.setText(if (isDone) R.string.action_done else R.string.action_complete_exercise)
        button.backgroundTintList = ContextCompat.getColorStateList(this, background)
        button.setTextColor(ContextCompat.getColor(this, content))
        button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, content))
        button.contentDescription = getString(
            if (isDone) R.string.cd_mark_not_done else R.string.cd_mark_done,
            binding.textName.text,
        )
    }

    private fun ViewStatTileBinding.bind(labelRes: Int, value: String) {
        textLabel.setText(labelRes)
        textValue.text = value
    }

    companion object {
        const val EXTRA_PRESCRIBED_ID = "extra_prescribed_id"
        const val EXTRA_DATE = "extra_date"

        /**
         * Cria a Intent EXPLÍCITA para esta tela.
         * Centralizar aqui garante que quem abre a tela sempre envie os extras corretos.
         *
         * @param prescribedId exercício (do treino do aluno) a exibir.
         * @param date dia do treino (o registro das séries é feito nessa data).
         */
        fun newIntent(context: Context, prescribedId: String, date: LocalDate): Intent =
            Intent(context, ExerciseDetailActivity::class.java)
                .putExtra(EXTRA_PRESCRIBED_ID, prescribedId)
                .putExtra(EXTRA_DATE, date.toString())
    }
}
