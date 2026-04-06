package com.example.jhqdriverapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: TextView
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminId: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var btnEdit: TextView

    // Edit Mode Views
    private lateinit var editFieldsContainer: LinearLayout
    private lateinit var etFullName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnChangePassword: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        loadAdminData()
        setupClickListeners()
        setViewMode()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvAdminName = findViewById(R.id.tvAdminName)
        tvAdminId = findViewById(R.id.tvAdminId)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        btnEdit = findViewById(R.id.btnEdit)

        editFieldsContainer = findViewById(R.id.editFieldsContainer)
        etFullName = findViewById(R.id.etFullName)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnChangePassword = findViewById(R.id.btnChangePassword)
    }

    private fun loadAdminData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = currentUser.uid

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminId = document.getString("driverId") ?: "N/A"
                    val fullName = document.getString("fullName") ?: ""
                    val phone = document.getString("phone") ?: "Not set"
                    val email = document.getString("email") ?: currentUser.email ?: ""

                    tvAdminName.text = fullName
                    tvAdminId.text = adminId
                    tvEmail.text = email
                    tvPhone.text = phone
                    etFullName.setText(fullName)

                    val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit()
                        .putString("fullName", fullName)
                        .putString("driverId", adminId)
                        .apply()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnEdit.setOnClickListener { setEditMode() }

        btnSave.setOnClickListener { saveProfile() }

        btnCancel.setOnClickListener {
            setViewMode()
            loadAdminData()
        }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
    }

    private fun setViewMode() {
        tvAdminName.visibility = TextView.VISIBLE
        btnEdit.visibility = TextView.VISIBLE
        editFieldsContainer.visibility = LinearLayout.GONE
        btnChangePassword.visibility = Button.VISIBLE
    }

    private fun setEditMode() {
        tvAdminName.visibility = TextView.GONE
        btnEdit.visibility = TextView.GONE
        editFieldsContainer.visibility = LinearLayout.VISIBLE
        btnChangePassword.visibility = Button.GONE
    }

    private fun saveProfile() {
        val newName = etFullName.text.toString().trim()
        if (newName.isEmpty()) {
            etFullName.error = "Name cannot be empty"
            etFullName.requestFocus()
            return
        }

        val currentUser = auth.currentUser ?: return

        btnSave.isEnabled = false
        btnSave.text = "SAVING..."

        val updates = hashMapOf<String, Any>(
            "fullName" to newName
        )

        db.collection("users").document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("fullName", newName).apply()

                tvAdminName.text = newName
                setViewMode()

                btnSave.isEnabled = true
                btnSave.text = "SAVE CHANGES"
                Toast.makeText(this, "✅ Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                btnSave.text = "SAVE CHANGES"
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("UPDATE") { dialog, _ ->
                val currentPwd = etCurrentPassword.text.toString()
                val newPwd = etNewPassword.text.toString()
                val confirmPwd = etConfirmPassword.text.toString()

                if (newPwd.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPwd != confirmPwd) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPwd, newPwd)
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "✅ Password updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Current password incorrect", Toast.LENGTH_SHORT).show()
                }
            }
    }
}