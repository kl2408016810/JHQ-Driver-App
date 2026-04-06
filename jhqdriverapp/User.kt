package com.example.jhqdriverapp

data class User(
    val id: String = "",
    val driverId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val userType: String = "driver",
    val vehicleNo: String = "",
    val uid: String = "",
    val status: String = "active",
    val createdAt: String = "",
    val totalLoads: Int = 0
) {
    fun getStatusText(): String {
        return when (status) {
            "approved" -> "Approved"
            "pending" -> "Pending Approval"
            else -> "Unknown"
        }
    }

    fun approveAccount(): User {
        return this.copy(status = "approved")
    }

    companion object {
        fun getSampleDrivers(): List<User> {
            return listOf(
                User(
                    id = "1",
                    driverId = "DRV-2024-015",
                    fullName = "Ahmad Bin Ali",
                    email = "ahmad@jhq.com",
                    phone = "012-3456789",
                    password = "driver123",
                    status = "approved",
                    createdAt = "2024-01-15",
                    userType = "driver"
                )
            )
        }
    }
}