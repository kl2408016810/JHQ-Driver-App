package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

// ==================== DATA CLASS ====================
data class ChatItem(
    val chatId: String = "",
    val otherDriverId: String = "",
    val otherDriverName: String = "",
    val otherDriverVehicle: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0
)

// ==================== ADAPTER ====================
class ChatListAdapter(
    private val chatList: List<ChatItem>,
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInitial: TextView = itemView.findViewById(R.id.tvInitial)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvVehicle: TextView = itemView.findViewById(R.id.tvVehicle)
        private val badgeUnread: TextView = itemView.findViewById(R.id.badgeUnread)

        fun bind(chat: ChatItem) {
            // Set initial
            tvInitial.text = if (chat.otherDriverName.isNotEmpty())
                chat.otherDriverName.first().toString().uppercase() else "?"

            tvName.text = chat.otherDriverName
            tvLastMessage.text = chat.lastMessage.takeIf { it.isNotEmpty() } ?: "No messages yet"
            tvTime.text = formatTime(chat.lastMessageTime)
            tvVehicle.text = chat.otherDriverVehicle

            // Show/hide unread badge
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
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount() = chatList.size
}

// ==================== ACTIVITY ====================
class ChatListActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var btnNewChat: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvEmptyState: TextView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var currentDriverId: String? = null
    private var chatListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "ChatListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        db = FirebaseFirestore.getInstance()

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentDriverId = sharedPref.getString("driverId", "")

        initViews()
        setupClickListeners()
        loadChats()
    }

    override fun onResume() {
        super.onResume()
        // Refresh bila balik dari chat
        loadChats()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener?.remove()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnNewChat = findViewById(R.id.btnNewChat)
        recyclerView = findViewById(R.id.recyclerView)

        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        Log.d(TAG, "✅ Views initialized")
        Log.d(TAG, "emptyStateLayout: $emptyStateLayout")
        Log.d(TAG, "tvEmptyState: $tvEmptyState")
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            Log.d(TAG, "Back clicked")
            finish()
        }

        btnNewChat.setOnClickListener {
            Log.d(TAG, "New Chat clicked")
            startActivity(Intent(this, NewChatActivity::class.java))
        }
    }

    private fun loadChats() {
        if (currentDriverId == null) {
            Log.e(TAG, "Driver ID is null")
            showEmptyState(true)
            return
        }

        Log.d(TAG, "Loading chats for driver: $currentDriverId")

        chatListener = db.collection("chats")
            .whereArrayContains("participants", currentDriverId!!)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading chats: ${error.message}")
                    showEmptyState(true)
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty()) {
                    Log.d(TAG, "No chats found")
                    showEmptyState(true)
                    return@addSnapshotListener
                }

                Log.d(TAG, "Found ${snapshots.size()} chats")

                val chatItems = mutableListOf<ChatItem>()
                var processedCount = 0

                if (snapshots.isEmpty()) {
                    updateUI(chatItems)
                    return@addSnapshotListener
                }

                for (doc in snapshots) {
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val otherParticipantId = participants.firstOrNull { it != currentDriverId }

                    if (otherParticipantId == null) {
                        processedCount++
                        if (processedCount == snapshots.size()) {
                            updateUI(chatItems)
                        }
                        continue
                    }

                    // Get unread count
                    val unreadMap = doc.get("unreadCount") as? Map<String, Any>
                    val unreadCount = (unreadMap?.get(currentDriverId) as? Long)?.toInt() ?: 0

                    // Get other driver's details from users collection
                    db.collection("users")
                        .whereEqualTo("driverId", otherParticipantId)
                        .get()
                        .addOnSuccessListener { users ->
                            if (!users.isEmpty()) {
                                val userDoc = users.first()
                                val driverName = userDoc.getString("fullName") ?: "Unknown"
                                val vehicleNo = userDoc.getString("vehicleNo") ?: ""

                                val chatItem = ChatItem(
                                    chatId = doc.id,
                                    otherDriverId = otherParticipantId,
                                    otherDriverName = driverName,
                                    otherDriverVehicle = vehicleNo,
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageTime = doc.getLong("lastMessageTime") ?: 0,
                                    unreadCount = unreadCount
                                )
                                chatItems.add(chatItem)
                            }

                            processedCount++
                            if (processedCount == snapshots.size()) {
                                updateUI(chatItems)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error getting user: ${e.message}")
                            processedCount++
                            if (processedCount == snapshots.size()) {
                                updateUI(chatItems)
                            }
                        }
                }
            }
    }

    private fun updateUI(chatItems: List<ChatItem>) {
        // Sort by last message time (newest first)
        val sortedList = chatItems.sortedByDescending { it.lastMessageTime }

        if (sortedList.isEmpty()) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
            val adapter = ChatListAdapter(sortedList) { chat ->
                openChat(chat)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun openChat(chat: ChatItem) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("otherDriverId", chat.otherDriverId)
        intent.putExtra("otherDriverName", chat.otherDriverName)
        startActivity(intent)
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            tvEmptyState.text = "No chats yet"
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }
}