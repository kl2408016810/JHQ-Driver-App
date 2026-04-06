package com.example.jhqdriverapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AuthManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_DRIVER_ID = "driverId"
        private const val KEY_DRIVER_NAME = "fullName"
        private const val KEY_USER_TYPE = "userType"
        private const val KEY_EMAIL = "email"
        private const val KEY_VEHICLE_NO = "vehicleNo"
        private const val KEY_PHONE = "phone"
        private const val TAG = "AuthManager"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ==================== LOGIN ====================

    fun loginWithEmail(email: String, password: String, callback: (Boolean, String, User?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: run {
                        callback(false, "Login failed", null)
                        return@addOnCompleteListener
                    }

                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val user = User(
                                    driverId = document.getString("driverId") ?: "",
                                    fullName = document.getString("fullName") ?: "",
                                    email = document.getString("email") ?: "",
                                    phone = document.getString("phone") ?: "",
                                    userType = document.getString("userType") ?: "driver",
                                    vehicleNo = document.getString("vehicleNo") ?: "",
                                    uid = document.id
                                )
                                saveUserSession(user)
                                callback(true, "Login successful", user)
                            } else {
                                callback(false, "User data not found", null)
                            }
                        }
                        .addOnFailureListener {
                            callback(false, "Database error", null)
                        }
                } else {
                    val errorMsg = task.exception?.message ?: "Login failed"
                    callback(false, errorMsg, null)
                }
            }
    }

    // ==================== REGISTER ====================

    fun registerWithEmail(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        userType: String,
        callback: (Boolean, String, User?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: run {
                        callback(false, "Registration failed", null)
                        return@addOnCompleteListener
                    }

                    val driverId = if (userType == "admin") {
                        "ADM-${System.currentTimeMillis()}"
                    } else {
                        "DRV-${System.currentTimeMillis()}"
                    }

                    val vehicleNo = if (userType == "driver") {
                        "JHA ${(1000..9999).random()}"
                    } else {
                        ""
                    }

                    val userData = hashMapOf(
                        "driverId" to driverId,
                        "fullName" to fullName,
                        "email" to email,
                        "phone" to phone,
                        "userType" to userType,
                        "vehicleNo" to vehicleNo,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            val user = User(
                                driverId = driverId,
                                fullName = fullName,
                                email = email,
                                phone = phone,
                                userType = userType,
                                vehicleNo = vehicleNo,
                                uid = uid
                            )
                            saveUserSession(user)
                            callback(true, "Registration successful", user)
                        }
                        .addOnFailureListener { e ->
                            firebaseUser.delete()
                            callback(false, "Failed to save: ${e.message}", null)
                        }
                } else {
                    val errorMsg = task.exception?.message ?: "Registration failed"
                    callback(false, errorMsg, null)
                }
            }
    }

    // ==================== SESSION ====================

    private fun saveUserSession(user: User) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_DRIVER_ID, user.driverId)
            putString(KEY_DRIVER_NAME, user.fullName)
            putString(KEY_USER_TYPE, user.userType)
            putString(KEY_EMAIL, user.email)
            putString(KEY_VEHICLE_NO, user.vehicleNo)
            putString(KEY_PHONE, user.phone)
            apply()
        }
    }

    fun logout() {
        auth.signOut()
        prefs.edit().clear().apply()
    }



    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getCurrentDriverId(): String = prefs.getString(KEY_DRIVER_ID, "") ?: ""
    // Tambah function ni dalam AuthManager.kt
    fun getCurrentDriverName(): String {
        return prefs.getString(KEY_DRIVER_NAME, "Unknown Driver") ?: "Unknown Driver"
    }
    // Tambah dalam AuthManager.kt
    fun saveCheckIn(driverId: String, driverName: String, location: String, callback: (Boolean, String) -> Unit) {
        callback(true, "Check-in saved")
    }

    fun saveCheckOut(driverId: String, hours: Double, loads: Int, breaks: Int, earnings: Double, callback: (Boolean, String) -> Unit) {
        callback(true, "Check-out saved")
    }
    fun getUserType(): String = prefs.getString(KEY_USER_TYPE, "driver") ?: "driver"
    fun getCurrentVehicleNo(): String = prefs.getString(KEY_VEHICLE_NO, "") ?: ""
}