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

class ProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: TextView
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverId: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvVehicleNo: TextView
    private lateinit var tvTotalLoads: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        loadDriverData()
        setupClickListeners()
    }

    private fun initViews() {

        btnBack = findViewById(R.id.btnBack)

        tvDriverName = findViewById(R.id.tvDriverName)
        tvDriverId = findViewById(R.id.tvDriverId)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvVehicleNo = findViewById(R.id.tvVehicleNo)
        tvTotalLoads = findViewById(R.id.tvTotalLoads)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        Log.d(TAG, "All views initialized")
    }

    private fun loadDriverData() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val driverId = intent.getStringExtra("driverId") ?: sharedPref.getString("driverId", "")

        Log.d(TAG, "Loading data for driver: $driverId")

        db.collection("users")
            .whereEqualTo("driverId", driverId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty()) {
                    val data = documents.first()
                    val fullName = data.getString("fullName") ?: ""
                    val email = data.getString("email") ?: ""
                    val phone = data.getString("phone") ?: ""
                    val id = data.getString("driverId") ?: ""
                    val vehicleNo = data.getString("vehicleNo") ?: "JHA 1234"

                    val totalLoads = if (data.contains("totalLoads")) {
                        data.getLong("totalLoads") ?: 0
                    } else {
                        0
                    }

                    tvDriverName.text = fullName
                    tvEmail.text = email
                    tvPhone.text = phone
                    tvDriverId.text = id
                    tvVehicleNo.text = vehicleNo
                    tvTotalLoads.text = totalLoads.toString()

                    Log.d(TAG, "Loaded data for: $fullName, total loads: $totalLoads")
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading profile: ${e.message}")
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }
}