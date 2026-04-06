package com.example.jhqdriverapp

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "JHQDriverPrefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val KEY_DRIVER_NAME = "driver_name"
        private const val KEY_IS_CHECKED_IN = "is_checked_in"
        private const val KEY_SHIFT_START_TIME = "shift_start_time"
        private const val KEY_CURRENT_LOCATION = "current_location"
        private const val KEY_BREAKS_TODAY = "breaks_today"
        private const val KEY_LOADS_TODAY = "loads_today"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Login state
    fun saveLoginState(driverId: String, driverName: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_DRIVER_ID, driverId)
            putString(KEY_DRIVER_NAME, driverName)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getDriverId(): String = prefs.getString(KEY_DRIVER_ID, "") ?: ""
    fun getDriverName(): String = prefs.getString(KEY_DRIVER_NAME, "") ?: ""

    // Shift management
    fun saveShiftStart(timeMillis: Long, location: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_CHECKED_IN, true)
            putLong(KEY_SHIFT_START_TIME, timeMillis)
            putString(KEY_CURRENT_LOCATION, location)
            apply()
        }
    }

    fun isCheckedIn(): Boolean = prefs.getBoolean(KEY_IS_CHECKED_IN, false)
    fun getShiftStartTime(): Long = prefs.getLong(KEY_SHIFT_START_TIME, 0)
    fun getCurrentLocation(): String = prefs.getString(KEY_CURRENT_LOCATION, "JHQ Main Yard") ?: "JHQ Main Yard"

    // Daily stats
    fun incrementBreaks() {
        val current = prefs.getInt(KEY_BREAKS_TODAY, 0)
        prefs.edit().putInt(KEY_BREAKS_TODAY, current + 1).apply()
    }

    fun incrementLoads() {
        val current = prefs.getInt(KEY_LOADS_TODAY, 0)
        prefs.edit().putInt(KEY_LOADS_TODAY, current + 1).apply()
    }

    fun getBreaksToday(): Int = prefs.getInt(KEY_BREAKS_TODAY, 0)
    fun getLoadsToday(): Int = prefs.getInt(KEY_LOADS_TODAY, 0)

    // Reset daily (boleh panggil saat midnight atau login baru)
    fun resetDailyStats() {
        prefs.edit().apply {
            putInt(KEY_BREAKS_TODAY, 0)
            putInt(KEY_LOADS_TODAY, 0)
            putBoolean(KEY_IS_CHECKED_IN, false)
            putLong(KEY_SHIFT_START_TIME, 0)
            apply()
        }
    }



    fun logout() {
        prefs.edit().clear().apply()
    }
}