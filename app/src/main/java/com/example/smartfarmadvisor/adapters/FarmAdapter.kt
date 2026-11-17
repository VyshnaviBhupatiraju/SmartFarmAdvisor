package com.example.smartfarmadvisor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfarmadvisor.data.FarmEntity
import com.example.smartfarmadvisor.databinding.ItemFarmBinding
import java.util.Locale

class FarmAdapter(
    private val onDeleteClick: (FarmEntity) -> Unit,
    private val onItemClick: ((FarmEntity) -> Unit)? = null
) : ListAdapter<FarmEntity, FarmAdapter.FarmViewHolder>(FarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FarmViewHolder {
        val binding = ItemFarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FarmViewHolder(private val binding: ItemFarmBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(farm: FarmEntity) {
            binding.txtFarmName.text = farm.farmName
            binding.txtCropAndLocation.text = String.format(
                Locale.US,
                "Crop: %s  •  Lat: %.4f, Lon: %.4f",
                farm.cropName,
                farm.latitude,
                farm.longitude
            )

            binding.btnDeleteFarm.setOnClickListener {
                onDeleteClick(farm)
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(farm)
            }
        }
    }

    class FarmDiffCallback : DiffUtil.ItemCallback<FarmEntity>() {
        override fun areItemsTheSame(oldItem: FarmEntity, newItem: FarmEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FarmEntity, newItem: FarmEntity): Boolean {
            return oldItem == newItem
        }
    }
}
