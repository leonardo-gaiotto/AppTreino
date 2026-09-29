package com.leonardo.apptreino.ui.editor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.databinding.ItemEditorCardioBinding
import com.leonardo.apptreino.databinding.ItemEditorExerciseBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.loadVideoThumbnail
import com.leonardo.apptreino.util.YouTube

/** Exercício na lista do editor, com os dados do catálogo (nome e vídeo). */
data class EditorExerciseItem(val exercise: PrescribedExercise, val catalog: CatalogExercise)

/**
 * Exercícios do treino em edição, com a alça para reordenar arrastando (ItemTouchHelper).
 *
 * Diferente das outras listas, aqui o DiffUtil roda de forma SÍNCRONA (sem ListAdapter):
 * durante o arraste a lista local muda na hora a cada troca, e só ao soltar a nova ordem
 * vai para o ViewModel. Quando o ViewModel devolve a lista, o DiffUtil não encontra
 * diferença (a tela já está na ordem certa) e nada "pula" de lugar.
 */
class EditorExerciseAdapter(
    private val onEdit: (EditorExerciseItem) -> Unit,
    private val onRemove: (EditorExerciseItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<EditorExerciseAdapter.ViewHolder>() {

    private var items: List<EditorExerciseItem> = emptyList()

    /** Nova lista vinda do ViewModel: o DiffUtil calcula só o que mudou e anima. */
    fun submitList(newItems: List<EditorExerciseItem>) {
        val diff = DiffUtil.calculateDiff(DiffCallback(items, newItems))
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    /** Troca imediata durante o arraste (antes de gravar no ViewModel). */
    fun moveItem(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices) return
        items = items.toMutableList().apply { add(to, removeAt(from)) }
        notifyItemMoved(from, to)
    }

    /** Ordem atual na tela (IDs dos exercícios), gravada no ViewModel ao soltar. */
    fun currentOrder(): List<String> = items.map { it.exercise.id }

    override fun getItemCount(): Int = items.size

    private fun getItem(position: Int): EditorExerciseItem = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemEditorExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback(
        private val old: List<EditorExerciseItem>,
        private val new: List<EditorExerciseItem>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
            EditorExerciseDiffCallback.areItemsTheSame(old[oldPosition], new[newPosition])
        override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
            EditorExerciseDiffCallback.areContentsTheSame(old[oldPosition], new[newPosition])
    }

    inner class ViewHolder(private val binding: ItemEditorExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardEditorExercise.setOnClickListener { current()?.let(onEdit) }
            binding.buttonRemove.setOnClickListener { current()?.let(onRemove) }
            setupDragHandle()
        }

        /** Tocar na alça inicia o arraste na hora (sem precisar segurar o card). */
        @SuppressLint("ClickableViewAccessibility")
        private fun setupDragHandle() {
            binding.imageDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
            }
        }

        fun bind(item: EditorExerciseItem) {
            val res = binding.root.resources
            val name = res.getString(item.catalog.nameRes)

            binding.textName.text = name
            binding.textPrescription.text = res.getString(
                R.string.prescription_summary,
                Formatters.prescription(res, item.exercise),
                Formatters.duration(res, item.exercise.restSeconds),
            )
            binding.imageThumbnail.loadVideoThumbnail(item.catalog.youtubeVideoId, YouTube.Thumbnail.MEDIUM)
            binding.imageDragHandle.contentDescription = res.getString(R.string.cd_reorder, name)
            binding.buttonRemove.contentDescription = res.getString(R.string.cd_remove, name)
        }

        private fun current(): EditorExerciseItem? =
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(::getItem)
    }
}

object EditorExerciseDiffCallback : DiffUtil.ItemCallback<EditorExerciseItem>() {
    override fun areItemsTheSame(oldItem: EditorExerciseItem, newItem: EditorExerciseItem): Boolean =
        oldItem.exercise.id == newItem.exercise.id

    override fun areContentsTheSame(oldItem: EditorExerciseItem, newItem: EditorExerciseItem): Boolean =
        oldItem == newItem
}

/** Atividades de cardio do treino em edição (ListAdapter + DiffUtil). */
class EditorCardioAdapter(
    private val onEdit: (Cardio) -> Unit,
    private val onRemove: (Cardio) -> Unit,
) : ListAdapter<Cardio, EditorCardioAdapter.ViewHolder>(EditorCardioDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemEditorCardioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEditorCardioBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardEditorCardio.setOnClickListener { current()?.let(onEdit) }
            binding.buttonRemove.setOnClickListener { current()?.let(onRemove) }
        }

        fun bind(cardio: Cardio) {
            val res = binding.root.resources
            val name = res.getString(cardio.type.labelRes)
            binding.imageType.setImageResource(cardio.type.iconRes)
            binding.textType.text = name
            binding.textSummary.text = Formatters.cardioSummary(res, cardio)
            binding.buttonRemove.contentDescription = res.getString(R.string.cd_remove, name)
        }

        private fun current(): Cardio? =
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(::getItem)
    }
}

object EditorCardioDiffCallback : DiffUtil.ItemCallback<Cardio>() {
    override fun areItemsTheSame(oldItem: Cardio, newItem: Cardio): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Cardio, newItem: Cardio): Boolean = oldItem == newItem
}
