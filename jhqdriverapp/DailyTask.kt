package com.example.jhqdriverapp

data class DailyTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    var status: String = "pending",
    val time: String = "",
    val category: String = "",
    val priority: String = "medium",
    val driverId: String = "",
    val date: String = "",
    val timestamp: Long = 0,
    val loads: Int = 1,
    val materialType: String = "Sand",
    val customer: String = "",
    val location: String = "",
    val assignedAdminId: String = "",
    val assignedAdminName: String = "",


    // 🔥 NEW FIELDS FOR ARRIVAL
    var hasArrived: Boolean = false,
    var arrivalTime: Long = 0,
    val taskIndex: Int = 0
) {
    fun getStatusText(): String {
        return when (status) {
            "pending" -> "Pending"
            "pending_approval" -> "Pending Approval"
            "approved" -> "Approved"
            "rejected" -> "Rejected"
            else -> status
        }
    }
}