package com.example.jhqdriverapp

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnUpdatePassword: Button

    // Password Toggles
    private lateinit var ivCurrentToggle: TextView
    private lateinit var ivNewToggle: TextView
    private lateinit var ivConfirmToggle: TextView

    private var isCurrentVisible = false
    private var isNewVisible = false
    private var isConfirmVisible = false

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupPasswordToggles()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword)

        ivCurrentToggle = findViewById(R.id.ivCurrentToggle)
        ivNewToggle = findViewById(R.id.ivNewToggle)
        ivConfirmToggle = findViewById(R.id.ivConfirmToggle)
    }

    private fun setupPasswordToggles() {
        ivCurrentToggle.setOnClickListener {
            isCurrentVisible = !isCurrentVisible
            togglePasswordVisibility(etCurrentPassword, ivCurrentToggle, isCurrentVisible)
        }

        ivNewToggle.setOnClickListener {
            isNewVisible = !isNewVisible
            togglePasswordVisibility(etNewPassword, ivNewToggle, isNewVisible)
        }

        ivConfirmToggle.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            togglePasswordVisibility(etConfirmPassword, ivConfirmToggle, isConfirmVisible)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, toggleView: TextView, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            toggleView.text = "👁️‍🗨️"
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleView.text = "👁️"
        }
        editText.setSelection(editText.text.length)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }

    private fun updatePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (currentPassword.isEmpty()) {
            etCurrentPassword.error = "Required"
            return
        }

        if (newPassword.isEmpty()) {
            etNewPassword.error = "Required"
            return
        }

        if (newPassword.length < 6) {
            etNewPassword.error = "Min 6 characters"
            return
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Passwords don't match"
            return
        }

        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        btnUpdatePassword.isEnabled = false
        btnUpdatePassword.text = "UPDATING..."

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            btnUpdatePassword.isEnabled = true
                            btnUpdatePassword.text = "UPDATE PASSWORD"

                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "✅ Password updated", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this, "❌ Update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    btnUpdatePassword.isEnabled = true
                    btnUpdatePassword.text = "UPDATE PASSWORD"
                    Toast.makeText(this, "❌ Current password incorrect", Toast.LENGTH_SHORT).show()
                }
            }
    }
}