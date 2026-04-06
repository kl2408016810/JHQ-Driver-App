package com.example.jhqdriverapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val chatList: List<ChatItem>,
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInitial: TextView = itemView.findViewById(R.id.tvInitial)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvVehicle: TextView = itemView.findViewById(R.id.tvVehicle)
        private val badgeUnread: TextView = itemView.findViewById(R.id.badgeUnread)  // ✅ GUNA INI

        fun bind(chat: ChatItem) {
            tvInitial.text = if (chat.otherDriverName.isNotEmpty())
                chat.otherDriverName.first().toString().uppercase() else "?"

            tvName.text = chat.otherDriverName
            tvLastMessage.text = chat.lastMessage.takeIf { it.isNotEmpty() } ?: "No messages yet"
            tvTime.text = formatTime(chat.lastMessageTime)
            tvVehicle.text = chat.otherDriverVehicle

            // 🔥 Show/hide unread badge
            if (chat.unreadCount > 0) {
                badgeUnread.visibility = View.VISIBLE
                badgeUnread.text = chat.unreadCount.toString()
            } else {
                badgeUnread.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(chat)
            }
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount() = chatList.size
}