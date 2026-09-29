package com.leonardo.apptreino.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.databinding.ItemCatalogExerciseBinding
import com.leonardo.apptreino.ui.common.loadVideoThumbnail
import com.leonardo.apptreino.util.YouTube

/** Exercícios do catálogo (ListAdapter + DiffUtil: a busca e os filtros animam a lista). */
class CatalogAdapter(
    private val onPick: (CatalogExercise) -> Unit,
) : ListAdapter<CatalogExercise, CatalogAdapter.ViewHolder>(CatalogDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCatalogExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCatalogExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardCatalog.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onPick(getItem(it)) }
            }
        }

        fun bind(exercise: CatalogExercise) {
            val res = binding.root.resources
            val name = res.getString(exercise.nameRes)
            binding.textGroup.setText(exercise.group.labelRes)
            binding.textName.text = name
            binding.textTarget.setText(exercise.targetRes)
            binding.imageThumbnail.loadVideoThumbnail(exercise.youtubeVideoId, YouTube.Thumbnail.MEDIUM)
            binding.cardCatalog.contentDescription = res.getString(R.string.cd_add_exercise, name)
        }
    }
}

object CatalogDiffCallback : DiffUtil.ItemCallback<CatalogExercise>() {
    override fun areItemsTheSame(oldItem: CatalogExercise, newItem: CatalogExercise): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: CatalogExercise, newItem: CatalogExercise): Boolean = oldItem == newItem
}
