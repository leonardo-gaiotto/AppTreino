package com.leonardo.apptreino.ui.today

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemCardioBinding
import com.leonardo.apptreino.databinding.ItemExerciseBinding
import com.leonardo.apptreino.databinding.ItemSectionHeaderBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.loadVideoThumbnail
import com.leonardo.apptreino.ui.main.CardioItem
import com.leonardo.apptreino.ui.main.ExerciseItem
import com.leonardo.apptreino.ui.main.TodayListItem
import com.leonardo.apptreino.util.YouTube

/**
 * Adapter da lista da aba "Hoje", com TRÊS tipos de item: exercício, título de seção e cardio.
 *
 * Por ser um [ListAdapter], basta chamar `submitList(novaLista)`: o [TodayDiffCallback]
 * compara a lista antiga com a nova em segundo plano e atualiza SOMENTE os cards que
 * mudaram, com animação — sem o custoso `notifyDataSetChanged()`.
 */
class TodayAdapter(
    private val onExerciseClick: (ExerciseItem) -> Unit,
    private val onToggleExercise: (ExerciseItem) -> Unit,
    private val onToggleCardio: (CardioItem) -> Unit,
) : ListAdapter<TodayListItem, RecyclerView.ViewHolder>(TodayDiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TodayListItem.ExerciseRow -> TYPE_EXERCISE
        TodayListItem.CardioHeader -> TYPE_HEADER
        is TodayListItem.CardioRow -> TYPE_CARDIO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_EXERCISE -> ExerciseViewHolder(ItemExerciseBinding.inflate(inflater, parent, false))
            TYPE_CARDIO -> CardioViewHolder(ItemCardioBinding.inflate(inflater, parent, false))
            else -> HeaderViewHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TodayListItem.ExerciseRow -> (holder as ExerciseViewHolder).bind(item.item)
            is TodayListItem.CardioRow -> (holder as CardioViewHolder).bind(item.item)
            TodayListItem.CardioHeader -> (holder as HeaderViewHolder).bind()
        }
    }

    /**
     * Atualização parcial: quando só o status mudou, o DiffUtil envia o payload
     * [TodayDiffCallback.PAYLOAD_STATUS] e redesenhamos apenas o check e a borda,
     * sem recarregar a miniatura (evita piscar a imagem).
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        val item = getItem(position)
        if (TodayDiffCallback.PAYLOAD_STATUS in payloads && item is TodayListItem.ExerciseRow) {
            (holder as ExerciseViewHolder).bindStatus(item.item)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // ------------------------------------------------------------------ Exercício

    inner class ExerciseViewHolder(
        private val binding: ItemExerciseBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Os cliques são configurados uma única vez por ViewHolder (e não a cada bind).
            binding.cardExercise.setOnClickListener { currentExercise()?.let(onExerciseClick) }
            binding.buttonDone.setOnClickListener {
                currentExercise()?.let { item ->
                    binding.imageDone.bounce()
                    onToggleExercise(item)
                }
            }
        }

        fun bind(item: ExerciseItem) {
            val res = binding.root.resources
            val name = res.getString(item.catalog.nameRes)

            binding.textNumber.text = Formatters.integer(res, item.number)
            binding.textMuscle.setText(item.catalog.targetRes)
            binding.textName.text = name
            binding.textPrescription.text = Formatters.prescription(res, item.exercise)
            val rest = Formatters.duration(res, item.exercise.restSeconds)
            binding.textRest.text = rest
            binding.textRest.contentDescription = res.getString(R.string.rest_label, rest)
            binding.imageThumbnail.contentDescription = res.getString(R.string.cd_video_thumbnail, name)
            binding.imageThumbnail.loadVideoThumbnail(item.catalog.youtubeVideoId, YouTube.Thumbnail.MEDIUM)

            bindStatus(item)
        }

        fun bindStatus(item: ExerciseItem) {
            val context = binding.root.context
            val name = context.getString(item.catalog.nameRes)

            binding.imageDone.isActivated = item.isDone
            binding.buttonDone.contentDescription = context.getString(
                if (item.isDone) R.string.cd_mark_not_done else R.string.cd_mark_done, name,
            )
            binding.textSetsProgress.isVisible = item.isPartial
            binding.textSetsProgress.text = context.resources.getQuantityString(
                R.plurals.sets_logged, item.exercise.sets, item.loggedSets, item.exercise.sets,
            )
            binding.cardExercise.highlight(item.isDone)
            binding.imageThumbnail.alpha = if (item.isDone) DONE_ALPHA else 1f
        }

        private fun currentExercise(): ExerciseItem? =
            (currentItem() as? TodayListItem.ExerciseRow)?.item
    }

    // ------------------------------------------------------------------ Cardio

    inner class CardioViewHolder(
        private val binding: ItemCardioBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.buttonCardioDone.setOnClickListener {
                (currentItem() as? TodayListItem.CardioRow)?.item?.let { item ->
                    binding.imageCardioDone.bounce()
                    onToggleCardio(item)
                }
            }
        }

        fun bind(item: CardioItem) {
            val context = binding.root.context
            val res = context.resources
            val name = res.getString(item.cardio.type.labelRes)

            binding.imageCardioType.setImageResource(item.cardio.type.iconRes)
            binding.textCardioName.text = name
            binding.textCardioSummary.text = Formatters.cardioSummary(res, item.cardio)
            binding.textCardioNote.isVisible = item.cardio.note != null
            binding.textCardioNote.text = item.cardio.note?.resolve(res)

            binding.imageCardioDone.isActivated = item.isDone
            binding.buttonCardioDone.contentDescription = context.getString(
                if (item.isDone) R.string.cd_mark_not_done else R.string.cd_mark_done, name,
            )
            binding.cardCardio.highlight(item.isDone)
        }
    }

    // ------------------------------------------------------------------ Título de seção

    class HeaderViewHolder(private val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.textSectionTitle.setText(R.string.today_cardio_header)
        }
    }

    // ------------------------------------------------------------------ Auxiliares

    private fun RecyclerView.ViewHolder.currentItem(): TodayListItem? =
        bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(::getItem)

    /** Borda verde-limão quando o item está concluído. */
    private fun MaterialCardView.highlight(done: Boolean) {
        strokeColor = ContextCompat.getColor(context, if (done) R.color.brand_lime else R.color.color_outline)
        strokeWidth = resources.getDimensionPixelSize(if (done) R.dimen.stroke_selected else R.dimen.stroke_thin)
    }

    /** Pequeno "pulo" no check para dar resposta visual ao toque. */
    private fun View.bounce() {
        animate().cancel()
        scaleX = CHECK_PRESSED_SCALE
        scaleY = CHECK_PRESSED_SCALE
        animate().scaleX(1f).scaleY(1f).setDuration(CHECK_ANIM_MS).start()
    }

    private companion object {
        const val TYPE_EXERCISE = 0
        const val TYPE_HEADER = 1
        const val TYPE_CARDIO = 2
        const val DONE_ALPHA = 0.5f
        const val CHECK_PRESSED_SCALE = 0.8f
        const val CHECK_ANIM_MS = 180L
    }
}

/**
 * Regras do DiffUtil para a lista da aba "Hoje":
 * - mesmo item → mesma chave (tipo + ID);
 * - mesmo conteúdo → data class igual;
 * - se num exercício só mudou o status do registro, envia um payload (atualização parcial).
 */
object TodayDiffCallback : DiffUtil.ItemCallback<TodayListItem>() {

    const val PAYLOAD_STATUS = "payload_status"

    override fun areItemsTheSame(oldItem: TodayListItem, newItem: TodayListItem): Boolean =
        oldItem.key == newItem.key

    override fun areContentsTheSame(oldItem: TodayListItem, newItem: TodayListItem): Boolean =
        oldItem == newItem

    override fun getChangePayload(oldItem: TodayListItem, newItem: TodayListItem): Any? {
        if (oldItem !is TodayListItem.ExerciseRow || newItem !is TodayListItem.ExerciseRow) return null
        val onlyStatusChanged = oldItem.item.copy(loggedSets = newItem.item.loggedSets) == newItem.item
        return if (onlyStatusChanged) PAYLOAD_STATUS else null
    }
}
