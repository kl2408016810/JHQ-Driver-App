package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var cbRememberMe: CheckBox
    private lateinit var ivPasswordToggle: TextView

    private var isPasswordVisible = false
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupPasswordToggle()
        setupClickListeners()

        Log.d(TAG, "LoginActivity created")
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        // ❌ NO tvForgotPassword
        cbRememberMe = findViewById(R.id.cbRememberMe)
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)

        Log.d(TAG, "All views initialized")
    }

    private fun setupPasswordToggle() {
        ivPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivPasswordToggle.text = "👁️‍🗨️"
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivPasswordToggle.text = "👁️"
            }
            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            loginUser()
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // ❌ NO tvForgotPassword click listener
    }

    private fun loginUser() {
        val email = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etUsername.error = "Email required"
            etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password required"
            etPassword.requestFocus()
            return
        }

        // Optional: Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.error = "Invalid email format"
            etUsername.requestFocus()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "LOGGING IN..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                btnLogin.text = "SIGN IN"

                if (task.isSuccessful) {
                    // Login success
                    val user = auth.currentUser
                    if (user != null) {
                        // Check if email is verified? (optional)
                        getUserTypeAndRedirect()
                    }
                } else {
                    // Handle specific errors
                    val exception = task.exception
                    when {
                        exception is FirebaseAuthInvalidUserException -> {
                            // Email not found
                            etUsername.error = "Email not registered"
                            Toast.makeText(this, "❌ Email not registered", Toast.LENGTH_SHORT).show()
                        }
                        exception is FirebaseAuthInvalidCredentialsException -> {
                            // Wrong password
                            etPassword.error = "Incorrect password"
                            Toast.makeText(this, "❌ Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "❌ Login failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun getUserTypeAndRedirect() {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userType = document.getString("userType") ?: "driver"
                    val fullName = document.getString("fullName") ?: ""
                    val driverId = document.getString("driverId") ?: ""
                    val vehicleNo = document.getString("vehicleNo") ?: ""

                    // Save session
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("userId", user.uid)
                        putString("fullName", fullName)
                        putString("driverId", driverId)
                        putString("userType", userType)
                        putString("email", user.email)
                        if (vehicleNo.isNotEmpty()) {
                            putString("vehicleNo", vehicleNo)
                        }
                        putBoolean("isLoggedIn", true)
                        apply()
                    }

                    Log.d(TAG, "User logged in: $userType, $fullName")

                    // Redirect based on user type
                    if (userType == "admin") {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                    } else {
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                    finish()
                } else {
                    Log.e(TAG, "User document not found")
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user data: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}