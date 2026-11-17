package com.example.smartfarmadvisor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfarmadvisor.data.IrrigationEntity
import com.example.smartfarmadvisor.databinding.ItemIrrigationBinding

class IrrigationAdapter(
    private val onDeleteClick: (IrrigationEntity) -> Unit
) : ListAdapter<IrrigationEntity, IrrigationAdapter.IrrigationViewHolder>(IrrigationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IrrigationViewHolder {
        val binding = ItemIrrigationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IrrigationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IrrigationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IrrigationViewHolder(private val binding: ItemIrrigationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IrrigationEntity) {
            binding.txtFarmName.text = item.farmName
            binding.txtIrrigationDetails.text = "Date: ${item.date}  •  Duration: ${item.durationMinutes} mins"

            binding.btnDeleteRecord.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    class IrrigationDiffCallback : DiffUtil.ItemCallback<IrrigationEntity>() {
        override fun areItemsTheSame(oldItem: IrrigationEntity, newItem: IrrigationEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IrrigationEntity, newItem: IrrigationEntity): Boolean {
            return oldItem == newItem
        }
    }
}
