package com.example.jhqdriverapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: TextView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var radioGroupRole: RadioGroup
    private lateinit var radioDriver: RadioButton
    private lateinit var radioAdmin: RadioButton
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView

    // Password toggle
    private lateinit var ivPasswordToggle: TextView
    private lateinit var ivConfirmPasswordToggle: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Warna Wine untuk UI
    private val wineColor = Color.parseColor("#722F37")

    companion object {
        private const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupListeners()
        setupPasswordToggles()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        radioGroupRole = findViewById(R.id.radioGroupRole)
        radioDriver = findViewById(R.id.radioDriver)
        radioAdmin = findViewById(R.id.radioAdmin)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        // Password toggles
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)
        ivConfirmPasswordToggle = findViewById(R.id.ivConfirmPasswordToggle)

        // Set default as driver
        radioDriver.isChecked = true

        // Set wine color for toggle icons
        ivPasswordToggle.setTextColor(wineColor)
        ivConfirmPasswordToggle.setTextColor(wineColor)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSignUp.setOnClickListener {
            performSignUp()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupPasswordToggles() {
        // Toggle for password
        ivPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, ivPasswordToggle, isPasswordVisible)
        }

        // Toggle for confirm password
        ivConfirmPasswordToggle.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etConfirmPassword, ivConfirmPasswordToggle, isConfirmPasswordVisible)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, toggleView: TextView, isVisible: Boolean) {
        if (isVisible) {
            // Show password
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            toggleView.text = "👁️‍🗨️"  // Open eye
        } else {
            // Hide password
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleView.text = "👁️"  // Closed eye
        }
        // Set wine color
        toggleView.setTextColor(wineColor)
        // Move cursor to end
        editText.setSelection(editText.text.length)
    }

    private fun performSignUp() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Get user type
        val userType = when (radioGroupRole.checkedRadioButtonId) {
            R.id.radioAdmin -> "admin"
            else -> "driver"
        }

        // Validation
        if (fullName.isEmpty()) {
            etFullName.error = "Full name required"
            etFullName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email required"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email format"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password required"
            etPassword.requestFocus()
            return
        }

        // Different password requirements for admin/driver
        val minPasswordLength = if (userType == "admin") 8 else 6
        if (password.length < minPasswordLength) {
            etPassword.error = if (userType == "admin")
                "Admin password min 8 chars"
            else
                "Password min 6 chars"
            etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        // Disable button
        btnSignUp.isEnabled = false
        btnSignUp.text = "CREATING ACCOUNT..."

        Log.d(TAG, "Creating account for: $email, type: $userType")

        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        saveUserToFirestore(user, fullName, phone, userType)
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)

                    // Error messages
                    val errorMessage = task.exception?.message ?: "Registration failed"

                    when {
                        errorMessage.contains("already in use", ignoreCase = true) -> {
                            etEmail.error = "Email already registered"
                            etEmail.requestFocus()
                            Toast.makeText(this, "❌ Email already registered", Toast.LENGTH_SHORT).show()
                        }
                        errorMessage.contains("badly formatted", ignoreCase = true) -> {
                            etEmail.error = "Invalid email format"
                            etEmail.requestFocus()
                        }
                        errorMessage.contains("weak password", ignoreCase = true) -> {
                            etPassword.error = "Password too weak"
                            etPassword.requestFocus()
                        }
                        else -> {
                            Toast.makeText(this, "❌ Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    btnSignUp.isEnabled = true
                    btnSignUp.text = "CREATE ACCOUNT"
                }
            }
    }

    // Generate vehicle number
    private fun generateVehicleNumber(): String {
        val prefixes = listOf("JHA", "JHB", "JHC", "JHD", "JHE", "JHF")
        val numbers = (1000..9999).random().toString()
        val randomPrefix = prefixes.random()
        return "$randomPrefix $numbers"
    }

    // Generate driver ID
    private fun generateDriverId(): String {
        val dateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val year = dateFormat.format(Date())
        val randomNum = (1..999).random().toString().padStart(3, '0')
        return "DRV-$year-$randomNum"
    }

    // Generate admin ID
    private fun generateAdminId(): String {
        val dateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val year = dateFormat.format(Date())
        val randomNum = (1..999).random().toString().padStart(3, '0')
        return "ADM-$year-$randomNum"
    }

    // Save user to Firestore
    private fun saveUserToFirestore(user: FirebaseUser, fullName: String, phone: String, userType: String) {

        // Generate ID based on user type (NO LONGER FROM USER INPUT)
        val userId = when (userType) {
            "admin" -> generateAdminId()
            else -> generateDriverId()
        }

        // Auto generate vehicle number (for drivers)
        val vehicleNumber = if (userType == "driver") {
            generateVehicleNumber()
        } else {
            "" // Admin no vehicle
        }

        // Prepare user data
        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to user.email,
            "driverId" to userId,
            "userType" to userType,
            "phone" to phone,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // Add vehicle number only for drivers
        if (userType == "driver") {
            userData["vehicleNo"] = vehicleNumber
            userData["totalLoads"] = 0  // Initialize total loads
        }

        Log.d(TAG, "Saving user to Firestore: $userData")

        // Save to Firestore
        db.collection("users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User saved successfully. Generated ID: $userId")

                // Save to SharedPreferences
                saveUserSession(user.uid, fullName, userId, userType, vehicleNumber, user.email ?: "")

                // Success message
                Toast.makeText(this, "✅ Account created successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to appropriate dashboard
                if (userType == "admin") {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving user to Firestore", e)
                btnSignUp.isEnabled = true
                btnSignUp.text = "CREATE ACCOUNT"
                Toast.makeText(
                    baseContext,
                    "❌ Failed to save user data",
                    Toast.LENGTH_SHORT
                ).show()

                // Delete auth user since Firestore failed
                user.delete()
            }
    }

    // Save session
    private fun saveUserSession(userId: String, fullName: String, driverId: String,
                                userType: String, vehicleNo: String, email: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("userId", userId)
            putString("fullName", fullName)
            putString("driverId", driverId)
            putString("userType", userType)
            putString("email", email)

            if (userType == "driver") {
                putString("vehicleNo", vehicleNo)
            }

            putBoolean("isLoggedIn", true)
            putBoolean("rememberMe", true)
            apply()
        }
        Log.d(TAG, "User session saved with ID: $driverId")
    }
}