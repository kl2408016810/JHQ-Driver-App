package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout  // 🔥 IMPORT LINEARLAYOUT
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// Data class for Driver Info
data class DriverInfo(
    val driverId: String,
    val name: String,
    val vehicleNo: String,
    val isOnline: Boolean
)

class NewChatActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout  // 🔥 LinearLayout
    private lateinit var tvEmptyState: TextView           // 🔥 TextView dalam layout

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: DriverSelectAdapter
    private var currentDriverId: String = ""
    private var currentDriverName: String = ""

    companion object {
        private const val TAG = "NewChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_chat)

        db = FirebaseFirestore.getInstance()

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentDriverId = sharedPref.getString("driverId", "") ?: ""
        currentDriverName = sharedPref.getString("fullName", "You") ?: "You"

        Log.d(TAG, "Current Driver ID: $currentDriverId")
        Log.d(TAG, "Current Driver Name: $currentDriverName")

        initViews()
        setupRecyclerView()
        loadAllDrivers()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        etSearch = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.recyclerView)

        // 🔥 FIX: Pisahkan emptyStateLayout dan tvEmptyState
        emptyStateLayout = findViewById(R.id.emptyStateLayout)  // LinearLayout
        tvEmptyState = findViewById(R.id.tvEmptyState)          // TextView

        tvTitle.text = "New Chat"

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = DriverSelectAdapter { driverId, driverName ->
            Log.d(TAG, "Selected driver: $driverName ($driverId)")
            startChatWithDriver(driverId, driverName)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadAllDrivers() {
        recyclerView.visibility = RecyclerView.GONE
        emptyStateLayout.visibility = LinearLayout.VISIBLE
        tvEmptyState.text = "Loading drivers..."

        Log.d(TAG, "Loading all drivers except: $currentDriverId")

        db.collection("users")
            .whereEqualTo("userType", "driver")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} total driver documents")

                val drivers = mutableListOf<DriverInfo>()

                for (doc in documents) {
                    val driverId = doc.getString("driverId") ?: "MISSING"
                    val fullName = doc.getString("fullName") ?: "Unknown"
                    val vehicleNo = doc.getString("vehicleNo") ?: "No vehicle"

                    Log.d(TAG, "Driver found: $driverId - $fullName - $vehicleNo")

                    if (driverId == currentDriverId) {
                        Log.d(TAG, "Skipping current user: $driverId")
                        continue
                    }

                    val driver = DriverInfo(
                        driverId = driverId,
                        name = fullName,
                        vehicleNo = vehicleNo,
                        isOnline = false
                    )
                    drivers.add(driver)
                }

                Log.d(TAG, "Other drivers count: ${drivers.size}")

                if (drivers.isEmpty()) {
                    tvEmptyState.text = "No other drivers found"
                    emptyStateLayout.visibility = LinearLayout.VISIBLE
                    recyclerView.visibility = RecyclerView.GONE

                    // Show sample data for testing
                    showSampleDrivers()
                } else {
                    emptyStateLayout.visibility = LinearLayout.GONE
                    recyclerView.visibility = RecyclerView.VISIBLE
                    adapter.submitList(drivers)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading drivers: ${e.message}")
                tvEmptyState.text = "Error loading drivers"
                emptyStateLayout.visibility = LinearLayout.VISIBLE
                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSampleDrivers() {
        val sampleDrivers = listOf(
            DriverInfo("DRV-2026-002", "Ahmad Bin Ali", "JHA 2345", false),
            DriverInfo("DRV-2026-003", "Siti Aminah", "JHB 6789", true),
            DriverInfo("DRV-2026-004", "Ravi Kumar", "JHC 1234", false)
        )

        emptyStateLayout.visibility = LinearLayout.GONE
        recyclerView.visibility = RecyclerView.VISIBLE
        adapter.submitList(sampleDrivers)

        Toast.makeText(this, "Showing sample drivers for testing", Toast.LENGTH_SHORT).show()
    }

    private fun startChatWithDriver(otherDriverId: String, otherDriverName: String) {
        val chatId = if (currentDriverId < otherDriverId) {
            "${currentDriverId}_$otherDriverId"
        } else {
            "${otherDriverId}_$currentDriverId"
        }

        Log.d(TAG, "Starting chat with: $otherDriverName ($otherDriverId)")
        Log.d(TAG, "Chat ID: $chatId")

        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { chatDoc ->
                if (chatDoc.exists()) {
                    Log.d(TAG, "Chat already exists")
                    openChat(chatId, otherDriverId, otherDriverName)
                } else {
                    Log.d(TAG, "Creating new chat")
                    createNewChat(chatId, otherDriverId, otherDriverName)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking chat: ${e.message}")
                Toast.makeText(this, "Error starting chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewChat(chatId: String, otherDriverId: String, otherDriverName: String) {
        val newChat = Chat(
            chatId = chatId,
            participants = listOf(currentDriverId, otherDriverId),
            participantNames = mapOf(
                currentDriverId to currentDriverName,
                otherDriverId to otherDriverName
            ),
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = mapOf(
                currentDriverId to 0,
                otherDriverId to 0
            )
        )

        db.collection("chats").document(chatId)
            .set(newChat)
            .addOnSuccessListener {
                Log.d(TAG, "Chat created successfully")
                openChat(chatId, otherDriverId, otherDriverName)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating chat: ${e.message}")
                Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChat(chatId: String, otherDriverId: String, otherDriverName: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("otherDriverId", otherDriverId)   // ganti "otherId"
        intent.putExtra("otherDriverName", otherDriverName) // ganti "otherName"
        startActivity(intent)
        finish()
    }
}