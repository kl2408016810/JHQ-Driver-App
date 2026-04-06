package com.example.jhqdriverapp

import java.util.*

data class ShiftRecord(
    val id: String = UUID.randomUUID().toString(),
    val driverId: String,
    val date: String, // "2024-02-20"
    val checkInTime: String, // "06:30"
    val checkOutTime: String? = null,
    val location: String,
    val totalHours: Float = 0f,
    val breakMinutes: Int = 0,
    val loadsCompleted: Int = 0,
    val earnings: Double = 0.0
)