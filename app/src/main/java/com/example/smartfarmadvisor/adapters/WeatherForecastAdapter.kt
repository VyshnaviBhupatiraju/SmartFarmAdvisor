package com.example.smartfarmadvisor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfarmadvisor.databinding.ItemWeatherForecastBinding
import java.util.Locale

data class DailyForecastItem(
    val date: String,
    val maxTemp: Double?,
    val rainfall: Double?,
    val et0: Double?
)

class WeatherForecastAdapter(
    private var items: List<DailyForecastItem> = emptyList()
) : RecyclerView.Adapter<WeatherForecastAdapter.ForecastViewHolder>() {

    fun submitList(newItems: List<DailyForecastItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemWeatherForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ForecastViewHolder(private val binding: ItemWeatherForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyForecastItem) {
            binding.txtForecastDate.text = item.date
            binding.txtForecastTemp.text = item.maxTemp?.let {
                String.format(Locale.US, "🌡 %.1f°C", it)
            } ?: "🌡 --"

            binding.txtForecastRain.text = item.rainfall?.let {
                String.format(Locale.US, "🌧 %.1f mm", it)
            } ?: "🌧 --"

            binding.txtForecastET0.text = item.et0?.let {
                String.format(Locale.US, "☀️ ET0: %.1f", it)
            } ?: "☀️ ET0: --"
        }
    }
}
