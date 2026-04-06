package com.example.jhqdriverapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DailyTaskAdapter(
    private val tasks: MutableList<DailyTask>,
    private val onTaskClick: (DailyTask) -> Unit,
    private val onSendClick: (DailyTask) -> Unit
) : RecyclerView.Adapter<DailyTaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvArrival: TextView = itemView.findViewById(R.id.tvArrival)  // 🔥 NEW
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnComplete: Button = itemView.findViewById(R.id.btnComplete)

        init {
            itemView.setOnClickListener {
                onTaskClick(tasks[adapterPosition])
            }
            btnComplete.setOnClickListener {
                onSendClick(tasks[adapterPosition])
            }
        }

        fun bind(task: DailyTask) {
            tvTime.text = task.time
            tvTitle.text = task.title

            val materialEmoji = when (task.materialType) {
                "Sand" -> "🟫"
                "Gravel" -> "🪨"
                "Cement" -> "🏭"
                "Soil" -> "🟤"
                "Clay" -> "🧱"
                else -> "📦"
            }
            tvDescription.text = "$materialEmoji ${task.description}"
            tvCategory.text = task.materialType

            // 🔥 ARRIVAL STATUS
            if (task.hasArrived) {
                tvArrival.text = "📍 ARRIVED"
                tvArrival.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                tvArrival.text = "🚫 NOT ARRIVED"
                tvArrival.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }

            // TASK STATUS
            when (task.status) {
                "pending" -> {
                    tvStatus.text = "⏳ PENDING"
                    tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                    btnComplete.visibility = View.VISIBLE
                    btnComplete.text = "✅ SEND"
                }
                "pending_approval" -> {
                    tvStatus.text = "⏳ PENDING APPROVAL"
                    tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
                    btnComplete.visibility = View.GONE
                }
                "approved" -> {
                    tvStatus.text = "✅ APPROVED"
                    tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                    btnComplete.visibility = View.GONE
                }
                "rejected" -> {
                    tvStatus.text = "❌ REJECTED"
                    tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
                    btnComplete.visibility = View.GONE
                }
                else -> {
                    tvStatus.text = task.status
                    tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#666666"))
                    btnComplete.visibility = View.GONE
                }
            }
            tvStatus.setTextColor(android.graphics.Color.WHITE)
            tvStatus.setPadding(12, 4, 12, 4)
        }
    }
}