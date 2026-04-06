package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CheckOutActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var tvShiftDate: TextView
    private lateinit var tvCheckInTime: TextView
    private lateinit var tvHoursWorked: TextView
    private lateinit var tvLoadsCompleted: TextView
    private lateinit var tvEarnings: TextView
    private lateinit var btnConfirmCheckOut: Button
    private lateinit var btnCancel: Button

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentDriverId: String? = null
    private var currentShiftId: String? = null
    private var checkInTime: Long = 0

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val ratePerLoad = 2.50

    companion object {
        private const val TAG = "CheckOutActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get driver ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentDriverId = sharedPref.getString("driverId", "")

        initViews()
        setupClickListeners()
        loadShiftData()
    }

    private fun initViews() {

        btnBack = findViewById(R.id.btnBack)

        tvShiftDate = findViewById(R.id.tvShiftDate)
        tvCheckInTime = findViewById(R.id.tvCheckInTime)
        tvHoursWorked = findViewById(R.id.tvHoursWorked)
        tvLoadsCompleted = findViewById(R.id.tvLoadsCompleted)
        tvEarnings = findViewById(R.id.tvEarnings)
        btnConfirmCheckOut = findViewById(R.id.btnConfirmCheckOut)
        btnCancel = findViewById(R.id.btnCancel)

        Log.d(TAG, "✅ Views initialized - btnBack is ImageView")
    }

    private fun setupClickListeners() {

        btnBack.setOnClickListener {
            Log.d(TAG, "Back clicked")
            finish()  // Balik ke activity sebelumnya
        }

        btnCancel.setOnClickListener {
            Log.d(TAG, "Cancel clicked")
            finish()
        }

        btnConfirmCheckOut.setOnClickListener {
            performCheckOut()
        }
    }

    private fun loadShiftData() {
        if (currentDriverId == null) {
            Toast.makeText(this, "Driver ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get active shift
        db.collection("shifts")
            .whereEqualTo("driverId", currentDriverId)
            .whereEqualTo("date", today)
            .whereEqualTo("checkOutTime", null)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty()) {
                    Toast.makeText(this, "No active shift found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val shift = documents.first()
                currentShiftId = shift.id
                checkInTime = shift.getLong("checkInTime") ?: 0

                // 🔥 GET TODAY'S LOADS untuk shift summary
                db.collection("task_approvals")
                    .whereEqualTo("driverId", currentDriverId)
                    .whereEqualTo("date", today)
                    .whereEqualTo("status", "approved")
                    .get()
                    .addOnSuccessListener { tasks ->
                        var totalLoads = 0
                        for (task in tasks) {
                            totalLoads += task.getLong("loads")?.toInt() ?: 0
                        }

                        val earnings = totalLoads * ratePerLoad
                        val hoursWorked = calculateHoursWorked(checkInTime)


                        runOnUiThread {
                            tvShiftDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                            tvCheckInTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(checkInTime))
                            tvHoursWorked.text = String.format("%.1f hours", hoursWorked)
                            tvLoadsCompleted.text = "$totalLoads loads"
                            tvEarnings.text = String.format("RM %.2f", earnings)
                        }
                    }
            }
    }

    private fun updateShiftSummary(totalLoads: Int) {
        tvShiftDate.text = dateFormat.format(Date())
        tvCheckInTime.text = timeFormat.format(Date(checkInTime))

        val hoursWorked = calculateHoursWorked(checkInTime)
        tvHoursWorked.text = String.format("%.1f hours", hoursWorked)

        tvLoadsCompleted.text = "$totalLoads loads"

        val earnings = totalLoads * ratePerLoad
        tvEarnings.text = String.format("RM %.2f", earnings)

        Log.d(TAG, "📊 Shift summary updated: $totalLoads loads, RM$earnings")
    }

    private fun calculateHoursWorked(checkInTime: Long): Double {
        val now = System.currentTimeMillis()
        return (now - checkInTime) / (1000.0 * 60 * 60)
    }

    private fun performCheckOut() {
        if (currentShiftId == null) {
            Toast.makeText(this, "No active shift found", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Performing check out for shift: $currentShiftId")

        btnConfirmCheckOut.isEnabled = false
        btnConfirmCheckOut.text = "PROCESSING..."

        val checkOutTime = System.currentTimeMillis()

        db.collection("shifts").document(currentShiftId!!)
            .update("checkOutTime", checkOutTime)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Check out successful")

                // Show summary popup
                showShiftSummaryPopup()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error checking out: ${e.message}")
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnConfirmCheckOut.isEnabled = true
                btnConfirmCheckOut.text = "CONFIRM CHECK OUT"
            }
    }

    private fun showShiftSummaryPopup() {
        val date = tvShiftDate.text.toString()
        val checkInTime = tvCheckInTime.text.toString()
        val hoursWorked = tvHoursWorked.text.toString()
        val loadsCompleted = tvLoadsCompleted.text.toString()
        val earnings = tvEarnings.text.toString()

        val dialog = AlertDialog.Builder(this)
            .setTitle("✅ Shift Summary")
            .setMessage("""
                
                📅 Date: $date
                ⏰ Check In: $checkInTime
                ⏱️ Hours Worked: $hoursWorked
                📦 Loads Completed: $loadsCompleted
                💰 Total Earnings: $earnings
                
                Thank you for your hard work!
                
            """.trimIndent())
            .setPositiveButton("OK") { _, _ ->
                // Go back to Home
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }
}