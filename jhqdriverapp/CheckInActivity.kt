package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

class CheckInActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: TextView
    private lateinit var btnCheckIn: Button
    private lateinit var tvShiftStatus: TextView
    private lateinit var tvCurrentLocation: TextView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentDriverId: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())

    companion object {
        private const val TAG = "CheckInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkin)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get driver ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentDriverId = sharedPref.getString("driverId", "")

        initViews()
        setupClickListeners()
        checkExistingShift()
        loadDriverInfo()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        tvShiftStatus = findViewById(R.id.tvShiftStatus)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)

        Log.d(TAG, "Views initialized")
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            Log.d(TAG, "Back clicked")
            finish()
        }

        btnCheckIn.setOnClickListener {
            Log.d(TAG, "Check In clicked")
            performCheckIn()
        }
    }

    private fun loadDriverInfo() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val vehicleNo = sharedPref.getString("vehicleNo", "JHA 2345")
        tvCurrentLocation.text = "📍 $vehicleNo • JHQ Main Yard"
    }

    private fun checkExistingShift() {
        if (currentDriverId == null) {
            Log.e(TAG, "Driver ID is null")
            return
        }

        db.collection("shifts")
            .whereEqualTo("driverId", currentDriverId)
            .whereEqualTo("date", today)
            .whereEqualTo("checkOutTime", null)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty()) {
                    // Already have active shift
                    tvShiftStatus.text = "● Already Checked In Today"
                    tvShiftStatus.setTextColor(android.graphics.Color.parseColor("#722F37"))
                    btnCheckIn.isEnabled = false
                    btnCheckIn.alpha = 0.5f
                    btnCheckIn.text = "ALREADY CHECKED IN"
                    Log.d(TAG, "Active shift found - cannot check in again")
                } else {
                    // No active shift
                    tvShiftStatus.text = "○ Ready to Check In"
                    tvShiftStatus.setTextColor(android.graphics.Color.parseColor("#8B4C4F"))
                    btnCheckIn.isEnabled = true
                    btnCheckIn.alpha = 1.0f
                    btnCheckIn.text = "CHECK IN"
                    Log.d(TAG, "No active shift - can check in")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking shift: ${e.message}")
            }
    }

    private fun performCheckIn() {
        if (currentDriverId == null) {
            Toast.makeText(this, "Driver ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val checkInTime = System.currentTimeMillis()

        val shiftData = hashMapOf(
            "driverId" to currentDriverId,
            "date" to today,
            "checkInTime" to checkInTime,
            "checkOutTime" to null,
            "timestamp" to FieldValue.serverTimestamp()
        )

        btnCheckIn.isEnabled = false
        btnCheckIn.text = "PROCESSING..."

        db.collection("shifts")
            .add(shiftData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Check in successful")
                Toast.makeText(this, "✅ Check In Successful!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Check in failed: ${e.message}")
                Toast.makeText(this, "❌ Check In failed", Toast.LENGTH_SHORT).show()
                btnCheckIn.isEnabled = true
                btnCheckIn.text = "CHECK IN"
            }
    }
}