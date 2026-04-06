package com.example.jhqdriverapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DriverSelectAdapter(
    private val onItemClick: (String, String) -> Unit
) : RecyclerView.Adapter<DriverSelectAdapter.DriverViewHolder>() {

    private var drivers = listOf<DriverInfo>()

    fun submitList(list: List<DriverInfo>) {
        drivers = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_driver_select, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        holder.bind(drivers[position])
    }

    override fun getItemCount() = drivers.size

    inner class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvVehicle: TextView = itemView.findViewById(R.id.tvVehicle)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val driver = drivers[position]
                    onItemClick(driver.driverId, driver.name)
                }
            }
        }

        fun bind(driver: DriverInfo) {
            tvIcon.text = "👤"
            tvName.text = driver.name
            tvVehicle.text = driver.vehicleNo

            tvStatus.text = if (driver.isOnline) "🟢 Online" else "⚪ Offline"

            val color = if (driver.isOnline) {
                ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
            } else {
                ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
            }
            tvStatus.setTextColor(color)
        }
    }
}