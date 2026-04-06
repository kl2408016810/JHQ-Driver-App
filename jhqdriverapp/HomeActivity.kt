package com.example.jhqdriverapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import androidx.appcompat.app.AlertDialog
import java.util.*

class HomeActivity : AppCompatActivity() {

    // Views
    private lateinit var tvDriverName: TextView
    private lateinit var tvCurrentLocation: TextView
    private lateinit var tvShiftStatus: TextView
    private lateinit var btnCheckIn: LinearLayout
    private lateinit var btnCheckOut: LinearLayout
    private lateinit var tvHoursWorked: TextView
    private lateinit var tvLoadsToday: TextView
    private lateinit var tvEarnings: TextView
    private lateinit var tvTotalCareerLoads: TextView
    private lateinit var tvLoadsProgress: TextView
    private lateinit var progressLoads: ProgressBar
    private lateinit var btnDailyTasks: LinearLayout
    private lateinit var btnChat: LinearLayout
    private lateinit var btnProfile: LinearLayout
    private lateinit var btnLogout: Button

    private lateinit var badgeChat: TextView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String? = null
    private var currentDriverId: String? = null

    private var tasksListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var shiftListener: ListenerRegistration? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())
    private val dailyTarget = 5
    private val ratePerLoad = 2.50

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid

        initViews()
        loadUserData()
        setupListeners()
        setupClickListeners()

        // Refresh data immediately
        refreshTodayData()
        refreshCareerData()
        checkShiftStatus()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - refreshing data")
        refreshTodayData()
        refreshCareerData()
        checkShiftStatus()
        updateChatBadge()
    }

    override fun onStop() {
        super.onStop()
        removeListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeListeners()
    }

    private fun initViews() {
        tvDriverName = findViewById(R.id.tvDriverName)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)
        tvShiftStatus = findViewById(R.id.tvShiftStatus)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        tvHoursWorked = findViewById(R.id.tvHoursWorked)
        tvLoadsToday = findViewById(R.id.tvLoadsToday)
        tvEarnings = findViewById(R.id.tvEarnings)
        tvTotalCareerLoads = findViewById(R.id.tvTotalCareerLoads)
        tvLoadsProgress = findViewById(R.id.tvLoadsProgress)
        progressLoads = findViewById(R.id.progressLoads)
        btnDailyTasks = findViewById(R.id.btnDailyTasks)
        btnChat = findViewById(R.id.btnChat)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        badgeChat = findViewById(R.id.badgeChat)

        Log.d(TAG, "✅ Views initialized")
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val fullName = sharedPref.getString("fullName", "Driver")
        val driverId = sharedPref.getString("driverId", "")
        val vehicleNo = sharedPref.getString("vehicleNo", "JHA 2345")

        currentDriverId = driverId

        tvDriverName.text = fullName
        tvCurrentLocation.text = "📍 $vehicleNo • JHQ Main Yard"

        Log.d(TAG, "🔍 Driver ID from prefs: $currentDriverId")
        Log.d(TAG, "🔍 Today's date: $today")
    }

    private fun setupListeners() {
        setupTasksListener()
        setupUserListener()
        setupShiftListener()
    }

    private fun setupTasksListener() {
        if (currentDriverId == null) return

        tasksListener = db.collection("task_approvals")
            .whereEqualTo("driverId", currentDriverId)
            .whereEqualTo("date", today)
            .whereEqualTo("status", "approved")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Tasks listener error: ${error.message}")
                    return@addSnapshotListener
                }

                Log.d(TAG, "🔄 Tasks listener triggered - refreshing data")
                refreshTodayData()
            }
    }

    private fun setupUserListener() {
        if (currentUserId == null) return

        userListener = db.collection("users").document(currentUserId!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "User listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val totalLoads = snapshot.getLong("totalLoads") ?: 0
                    runOnUiThread {
                        tvTotalCareerLoads.text = "$totalLoads loads"
                        Log.d(TAG, "Career total updated: $totalLoads")
                    }
                }
            }
    }


    private fun setupShiftListener() {
        if (currentDriverId == null) return

        shiftListener = db.collection("shifts")
            .whereEqualTo("driverId", currentDriverId)
            .whereEqualTo("date", today)
            .whereEqualTo("checkOutTime", null)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Shift listener error: ${error.message}")
                    return@addSnapshotListener
                }

                runOnUiThread {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        tvShiftStatus.text = "● Active"
                        tvShiftStatus.setTextColor(Color.parseColor("#722F37"))

                        btnCheckOut.isEnabled = true
                        btnCheckOut.alpha = 1.0f
                        btnCheckIn.isEnabled = false
                        btnCheckIn.alpha = 0.5f

                        val shift = snapshots.first()
                        val checkInTime = shift.getLong("checkInTime") ?: 0
                        if (checkInTime > 0) {
                            val hoursWorked = calculateHoursWorked(checkInTime)
                            tvHoursWorked.text = String.format("%.1f", hoursWorked)
                        }
                    } else {
                        tvShiftStatus.text = "○ Inactive"
                        tvShiftStatus.setTextColor(Color.parseColor("#8B4C4F"))
                        tvHoursWorked.text = "0.0"

                        btnCheckIn.isEnabled = true
                        btnCheckIn.alpha = 1.0f
                        btnCheckOut.isEnabled = false
                        btnCheckOut.alpha = 0.5f
                    }
                }
            }
    }

    private fun updateChatBadge() {
        if (currentDriverId == null) return

        db.collection("chats")
            .whereEqualTo("receiverId", currentDriverId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                runOnUiThread {
                    if (count > 0) {
                        badgeChat.text = count.toString()
                        badgeChat.visibility = View.VISIBLE
                    } else {
                        badgeChat.visibility = View.GONE
                    }
                }
            }
    }

    // 🔥 MAIN FUNCTION UNTUK REFRESH DATA
    private fun refreshTodayData() {
        if (currentDriverId == null) {
            Log.e(TAG, "❌ refreshTodayData: driverId is NULL")
            return
        }

        Log.d(TAG, "========== REFRESH TODAY'S DATA ==========")
        Log.d(TAG, "📌 Driver ID: $currentDriverId")
        Log.d(TAG, "📌 Date: $today")
        Log.d(TAG, "📌 Collection: task_approvals")
        Log.d(TAG, "📌 Query: driverId=$currentDriverId, date=$today, status=approved")

        db.collection("task_approvals")
            .whereEqualTo("driverId", currentDriverId)
            .whereEqualTo("date", today)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "🔥 Query executed successfully")
                Log.d(TAG, "📦 Found ${documents.size()} approved tasks")

                if (documents.isEmpty()) {
                    Log.d(TAG, "⚠️ NO DOCUMENTS FOUND!")
                    Log.d(TAG, "Possible issues:")
                    Log.d(TAG, "  - Wrong driverId? (current: $currentDriverId)")
                    Log.d(TAG, "  - Wrong date? (current: $today)")
                    Log.d(TAG, "  - Status not 'approved'?")
                    Log.d(TAG, "  - Collection name wrong? (using task_approvals)")

                    runOnUiThread {
                        tvLoadsToday.text = "0"
                        tvEarnings.text = "RM0.00"
                        tvLoadsProgress.text = "0/$dailyTarget loads"
                        progressLoads.progress = 0
                    }
                    return@addOnSuccessListener
                }

                var totalLoadsToday = 0
                for (doc in documents) {
                    val loads = doc.getLong("loads")?.toInt() ?: 0
                    val material = doc.getString("materialType") ?: "Unknown"
                    val docId = doc.id

                    totalLoadsToday += loads
                    Log.d(TAG, "   📄 Document: $docId")
                    Log.d(TAG, "      - Material: $material")
                    Log.d(TAG, "      - Loads: $loads")
                }

                val earnings = totalLoadsToday * ratePerLoad
                Log.d(TAG, "📊 TOTAL Loads: $totalLoadsToday")
                Log.d(TAG, "💰 TOTAL Earnings: RM$earnings")

                runOnUiThread {
                    // Update UI
                    tvLoadsToday.text = totalLoadsToday.toString()
                    tvEarnings.text = String.format("RM%.2f", earnings)
                    tvLoadsProgress.text = "$totalLoadsToday/$dailyTarget loads"
                    progressLoads.progress = totalLoadsToday
                    progressLoads.max = dailyTarget

                    Log.d(TAG, "✅ UI Updated:")
                    Log.d(TAG, "   - tvLoadsToday: ${tvLoadsToday.text}")
                    Log.d(TAG, "   - tvEarnings: ${tvEarnings.text}")
                    Log.d(TAG, "   - tvLoadsProgress: ${tvLoadsProgress.text}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Firestore query failed!")
                Log.e(TAG, "❌ Error: ${e.message}")
                e.printStackTrace()
            }
    }

    private fun refreshCareerData() {
        if (currentUserId == null) return

        db.collection("users").document(currentUserId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalLoads = document.getLong("totalLoads") ?: 0
                    runOnUiThread {
                        tvTotalCareerLoads.text = "$totalLoads loads"
                        Log.d(TAG, "Career total refreshed: $totalLoads")
                    }
                }
            }
    }

    private fun calculateHoursWorked(checkInTime: Long): Double {
        val now = System.currentTimeMillis()
        return (now - checkInTime) / (1000.0 * 60 * 60)
    }

    private fun checkShiftStatus() {
        if (currentDriverId == null) return

        db.collection("shifts")
            .whereEqualTo("driverId", currentDriverId)
            .whereEqualTo("date", today)
            .whereEqualTo("checkOutTime", null)
            .get()
            .addOnSuccessListener { documents ->
                runOnUiThread {
                    if (!documents.isEmpty()) {
                        tvShiftStatus.text = "● Active"
                        tvShiftStatus.setTextColor(Color.parseColor("#722F37"))

                        btnCheckOut.isEnabled = true
                        btnCheckOut.alpha = 1.0f
                        btnCheckIn.isEnabled = false
                        btnCheckIn.alpha = 0.5f

                        val shift = documents.first()
                        val checkInTime = shift.getLong("checkInTime") ?: 0
                        if (checkInTime > 0) {
                            val hoursWorked = calculateHoursWorked(checkInTime)
                            tvHoursWorked.text = String.format("%.1f", hoursWorked)
                        }
                    } else {
                        tvShiftStatus.text = "○ Inactive"
                        tvShiftStatus.setTextColor(Color.parseColor("#8B4C4F"))
                        tvHoursWorked.text = "0.0"

                        btnCheckIn.isEnabled = true
                        btnCheckIn.alpha = 1.0f
                        btnCheckOut.isEnabled = false
                        btnCheckOut.alpha = 0.5f
                    }
                }
            }
    }

    private fun removeListeners() {
        tasksListener?.remove()
        tasksListener = null
        userListener?.remove()
        userListener = null
        shiftListener?.remove()
        shiftListener = null
        Log.d(TAG, "All listeners removed")
    }

    private fun setupChatListener() {
        if (currentDriverId == null) return

        db.collection("chats")
            .whereEqualTo("receiverId", currentDriverId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val count = snapshots?.size() ?: 0
                runOnUiThread {
                    if (count > 0) {
                        badgeChat.text = count.toString()
                        badgeChat.visibility = View.VISIBLE
                    } else {
                        badgeChat.visibility = View.GONE
                    }
                }
            }
    }

    private fun setupClickListeners() {
        btnCheckIn.setOnClickListener {
            Log.d(TAG, "Check In clicked")
            startActivity(Intent(this, CheckInActivity::class.java))
        }

        btnCheckOut.setOnClickListener {
            Log.d(TAG, "Check Out clicked")
            if (btnCheckOut.isEnabled) {
                startActivity(Intent(this, CheckOutActivity::class.java))
            } else {
                Toast.makeText(this, "No active shift", Toast.LENGTH_SHORT).show()
            }
        }

        btnDailyTasks.setOnClickListener {
            Log.d(TAG, "Daily Tasks clicked")
            startActivity(Intent(this, DailyTasksActivity::class.java))
        }

        btnChat.setOnClickListener {
            Log.d(TAG, "Chat clicked")
            startActivity(Intent(this, ChatListActivity::class.java))
            badgeChat.visibility = View.GONE
        }

        btnProfile.setOnClickListener {
            Log.d(TAG, "Profile clicked")
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun performLogout() {
        removeListeners()

        getSharedPreferences("user_prefs", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        auth.signOut()

        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}