package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etVehicleNo: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid

        initViews()
        loadCurrentData()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etVehicleNo = findViewById(R.id.etVehicleNo)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun loadCurrentData() {
        if (currentUserId == null) return

        db.collection("users").document(currentUserId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("fullName") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val vehicleNo = document.getString("vehicleNo") ?: ""

                    etFullName.setText(fullName)
                    etPhone.setText(phone)
                    etVehicleNo.setText(vehicleNo)
                }
            }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val vehicleNo = etVehicleNo.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Name required"
            return
        }

        if (currentUserId == null) return

        btnSave.isEnabled = false
        btnSave.text = "SAVING..."
        
        val updates = mapOf(
            "fullName" to fullName,
            "phone" to phone,
            "vehicleNo" to vehicleNo
        )

        db.collection("users").document(currentUserId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Profile updated", Toast.LENGTH_SHORT).show()

                // Update SharedPreferences
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                sharedPref.edit().apply {
                    putString("fullName", fullName)
                    putString("vehicleNo", vehicleNo)
                    apply()
                }

                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "SAVE CHANGES"
            }
    }
}