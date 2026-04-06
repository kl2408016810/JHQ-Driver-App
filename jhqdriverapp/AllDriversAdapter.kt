package com.example.jhqdriverapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AllDriversAdapter(
    private val drivers: List<Driver>,
    private val onItemClick: (Driver) -> Unit
) : RecyclerView.Adapter<AllDriversAdapter.DriverViewHolder>() {

    inner class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDriverInitial: TextView = itemView.findViewById(R.id.tvDriverInitial)
        private val tvDriverName: TextView = itemView.findViewById(R.id.tvDriverName)
        private val tvDriverId: TextView = itemView.findViewById(R.id.tvDriverId)
        private val tvDriverEmail: TextView = itemView.findViewById(R.id.tvDriverEmail)
        private val tvDriverPhone: TextView = itemView.findViewById(R.id.tvDriverPhone)
        private val tvDriverStatus: TextView = itemView.findViewById(R.id.tvDriverStatus)


        fun bind(driver: Driver) {
            // Set initial
            tvDriverInitial.text = if (driver.fullName.isNotEmpty())
                driver.fullName.first().toString().uppercase() else "?"

            tvDriverName.text = driver.fullName
            tvDriverId.text = driver.driverId
            tvDriverEmail.text = driver.email
            tvDriverPhone.text = driver.phone

            // Set status
            if (driver.status == "active") {
                tvDriverStatus.text = "● ACTIVE"
                tvDriverStatus.setTextColor(android.graphics.Color.parseColor("#722F37"))
                tvDriverStatus.setBackgroundResource(R.drawable.badge_wine_light)
            } else {
                tvDriverStatus.text = "○ INACTIVE"
                tvDriverStatus.setTextColor(android.graphics.Color.parseColor("#8B4C4F"))
                tvDriverStatus.setBackgroundResource(R.drawable.badge_wine_light)
            }

            itemView.setOnClickListener {
                onItemClick(driver)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_all_driver, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        holder.bind(drivers[position])
    }

    override fun getItemCount() = drivers.size
}