package com.example.jhqdriverapp

data class Shift(
    val shiftId: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val date: String = "",
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val checkInTimestamp: Long = 0,
    val checkOutTimestamp: Long = 0,
    val totalHours: Double = 0.0,
    val loadsCompleted: Int = 0,
    val breaksTaken: Int = 0,
    val earnings: Double = 0.0,
    val location: String = "",
    val status: String = "active" // "active" or "completed"
)