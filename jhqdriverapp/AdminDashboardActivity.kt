package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import androidx.appcompat.app.AlertDialog
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminId: TextView

    // Stats
    private lateinit var tvTotalDrivers: TextView
    private lateinit var tvActiveToday: TextView
    private lateinit var tvPendingApprovals: TextView

    // Management Cards
    private lateinit var cardAllDrivers: LinearLayout
    private lateinit var cardTaskApprovals: LinearLayout
    private lateinit var cardAdminProfile: LinearLayout
    private lateinit var badgeApprovals: TextView

    // Buttons
    private lateinit var btnLogout: Button

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())

    companion object {
        private const val TAG = "AdminDashboard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()
        loadAdminData()

        // 🔥 PANGGIL FUNCTION INI
        loadStatsFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        loadAdminData()
        loadStatsFromFirebase()  // Refresh bila balik ke dashboard
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvAdminName = findViewById(R.id.tvAdminName)
        tvAdminId = findViewById(R.id.tvAdminId)

        tvTotalDrivers = findViewById(R.id.tvTotalDrivers)
        tvActiveToday = findViewById(R.id.tvActiveToday)
        tvPendingApprovals = findViewById(R.id.tvPendingApprovals)

        cardAllDrivers = findViewById(R.id.cardAllDrivers)
        cardTaskApprovals = findViewById(R.id.cardTaskApprovals)
        cardAdminProfile = findViewById(R.id.cardAdminProfile)
        badgeApprovals = findViewById(R.id.badgeApprovals)

        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupClickListeners() {
        cardAllDrivers.setOnClickListener {
            startActivity(Intent(this, AllDriversActivity::class.java))
        }

        cardTaskApprovals.setOnClickListener {
            startActivity(Intent(this, TaskApprovalsActivity::class.java))
        }

        cardAdminProfile.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
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

    // 🔥 FUNCTION UNTUK LOAD STATS
    private fun loadStatsFromFirebase() {
        Log.d(TAG, "Loading stats for date: $today")

        // Total Drivers
        db.collection("users")
            .whereEqualTo("userType", "driver")
            .get()
            .addOnSuccessListener { documents ->
                val total = documents.size()
                tvTotalDrivers.text = total.toString()
                Log.d(TAG, "Total drivers: $total")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading drivers: ${e.message}")
                tvTotalDrivers.text = "0"
            }

        // Active Today
        db.collection("shifts")
            .whereEqualTo("date", today)
            .whereEqualTo("checkOutTime", null)
            .get()
            .addOnSuccessListener { documents ->
                val active = documents.size()
                tvActiveToday.text = active.toString()
                Log.d(TAG, "Active today: $active")

                // Log untuk debug
                for (doc in documents) {
                    Log.d(TAG, "   Active shift: ${doc.id}, driver: ${doc.getString("driverId")}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading active shifts: ${e.message}")
                tvActiveToday.text = "0"
            }

        // Pending Approvals
        db.collection("task_approvals")
            .whereEqualTo("status", "pending_approval")
            .get()
            .addOnSuccessListener { documents ->
                val pending = documents.size()
                tvPendingApprovals.text = pending.toString()

                // Update badge
                if (pending > 0) {
                    badgeApprovals.text = pending.toString()
                    badgeApprovals.visibility = View.VISIBLE
                } else {
                    badgeApprovals.visibility = View.GONE
                }

                Log.d(TAG, "Pending approvals: $pending")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading pending: ${e.message}")
                tvPendingApprovals.text = "0"
                badgeApprovals.visibility = View.GONE
            }
    }

    private fun loadAdminData() {
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val fullName = doc.getString("fullName") ?: "Admin"
                    val adminId = doc.getString("driverId") ?: "ADM-${System.currentTimeMillis()}"

                    tvAdminName.text = fullName
                    tvAdminId.text = adminId

                    // Update greeting
                    updateWelcomeMessage(fullName)

                    // Save to SharedPreferences
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("fullName", fullName)
                        putString("driverId", adminId)
                        putString("userId", user.uid)
                        apply()
                    }
                }
            }
    }

    private fun updateWelcomeMessage(adminName: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }

        tvWelcome.text = greeting
    }

    private fun performLogout() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}