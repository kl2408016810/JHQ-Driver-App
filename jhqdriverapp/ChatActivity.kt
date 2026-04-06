package com.example.jhqdriverapp

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvDriverName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDriverInitial: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var chatId: String = ""
    private var otherDriverId: String = ""
    private var otherDriverName: String = ""
    private var currentDriverId: String = ""
    private var currentDriverName: String = ""

    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "ChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Terima key yang dihantar dari NewChatActivity
        otherDriverId = intent.getStringExtra("otherId") ?: ""
        otherDriverName = intent.getStringExtra("otherName") ?: ""

        // Jika masih kosong, cuba key lain (fallback)
        if (otherDriverId.isEmpty()) {
            otherDriverId = intent.getStringExtra("otherDriverId") ?: ""
        }
        if (otherDriverName.isEmpty() || otherDriverName == "Unknown") {
            otherDriverName = intent.getStringExtra("otherDriverName") ?: ""
        }

        Log.d(TAG, "onCreate: otherDriverId='$otherDriverId', otherDriverName='$otherDriverName'")

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentDriverId = sharedPref.getString("driverId", "") ?: ""
        currentDriverName = sharedPref.getString("fullName", "You") ?: "You"

        db = FirebaseFirestore.getInstance()

        initViews()
        setupRecyclerView()

        if (otherDriverId.isEmpty()) {
            // Cuba recovery
            recoverFromExistingChat()
        } else {
            chatId = getChatId(currentDriverId, otherDriverId)
            // Ambil nama dari Firestore untuk pastikan betul
            fetchDriverName()
            loadMessages()
            markMessagesAsRead()
        }
    }

    private fun recoverFromExistingChat() {
        db.collection("chats")
            .whereArrayContains("participants", currentDriverId)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val other = participants.firstOrNull { it != currentDriverId }
                    if (other != null) {
                        otherDriverId = other
                        chatId = getChatId(currentDriverId, otherDriverId)
                        fetchDriverName()
                        loadMessages()
                        markMessagesAsRead()
                        return@addOnSuccessListener
                    }
                }
                Toast.makeText(this, "No chat found. Please start a new chat.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading chat. Please try again.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun fetchDriverName() {
        if (otherDriverId.isEmpty()) return
        db.collection("users")
            .whereEqualTo("driverId", otherDriverId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty()) {
                    val name = documents.first().getString("fullName") ?: "Unknown"
                    otherDriverName = name
                    setDriverNameUI(name)
                } else {
                    setDriverNameUI("Unknown Driver")
                }
            }
            .addOnFailureListener {
                setDriverNameUI("Error")
            }
    }

    private fun setDriverNameUI(name: String) {
        tvDriverName.text = name
        tvDriverInitial.text = if (name.isNotEmpty() && name != "Unknown" && name != "Error")
            name.first().toString().uppercase() else "?"
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvStatus = findViewById(R.id.tvStatus)
        tvDriverInitial = findViewById(R.id.tvDriverInitial)
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvStatus.text = "● Online"
        btnBack.setOnClickListener { finish() }
        btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(currentDriverId)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter
    }

    private fun loadMessages() {
        if (otherDriverId.isEmpty()) return
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen error: ${error.message}")
                    return@addSnapshotListener
                }
                messages.clear()
                snapshot?.documents?.forEach { doc ->
                    messages.add(
                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0,
                            read = doc.getBoolean("read") ?: false
                        )
                    )
                }
                adapter.submitList(messages.toList())
                if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage() {
        if (otherDriverId.isEmpty()) {
            Toast.makeText(this, "Cannot send message: missing recipient", Toast.LENGTH_SHORT).show()
            return
        }
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Type a message", Toast.LENGTH_SHORT).show()
            return
        }
        etMessage.text.clear()
        ensureChatDocumentExists {
            val message = hashMapOf(
                "senderId" to currentDriverId,
                "senderName" to currentDriverName,
                "message" to messageText,
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )
            db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener { updateChatLastMessage(messageText) }
                .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun ensureChatDocumentExists(callback: () -> Unit) {
        if (otherDriverId.isEmpty()) {
            callback()
            return
        }
        val chatDocRef = db.collection("chats").document(chatId)
        chatDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val initialData = mapOf(
                    "participants" to listOf(currentDriverId, otherDriverId),
                    "participantNames" to mapOf(currentDriverId to currentDriverName, otherDriverId to otherDriverName),
                    "lastMessage" to "",
                    "lastMessageTime" to 0L,
                    "unreadCount" to mapOf(currentDriverId to 0, otherDriverId to 0)
                )
                chatDocRef.set(initialData).addOnSuccessListener { callback() }
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to create chat: ${e.message}"); callback() }
            } else {
                callback()
            }
        }.addOnFailureListener { e -> Log.e(TAG, "Error checking chat: ${e.message}"); callback() }
    }

    private fun updateChatLastMessage(lastMessage: String) {
        val updates = mutableMapOf<String, Any>(
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis()
        )
        updates["unreadCount.$otherDriverId"] = FieldValue.increment(1)
        db.collection("chats").document(chatId).update(updates)
            .addOnSuccessListener { Log.d(TAG, "Last message updated") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to update last message: ${e.message}") }
    }

    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }

    private fun markMessagesAsRead() {
        if (otherDriverId.isEmpty()) return
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("senderId", otherDriverId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { messages ->
                val batch = db.batch()
                messages.forEach { batch.update(it.reference, "read", true) }
                batch.commit().addOnSuccessListener {
                    Log.d(TAG, "✅ All messages marked as read")
                    resetUnreadCount()
                }
            }
    }

    private fun resetUnreadCount() {
        val fieldPath = "unreadCount.$currentDriverId"
        db.collection("chats").document(chatId).update(fieldPath, 0)
            .addOnSuccessListener { Log.d(TAG, "✅ Unread count reset") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to reset unread: ${e.message}") }
    }
}