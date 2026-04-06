package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

class DailyTasksActivity : AppCompatActivity() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var tvPendingTasks: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView
    private lateinit var btnMarkComplete: Button
    private lateinit var btnHistory: Button

    private val taskList = mutableListOf<DailyTask>()
    private lateinit var taskAdapter: DailyTaskAdapter
    private var selectedTask: DailyTask? = null
    private var adminList: List<Admin> = emptyList()

    private lateinit var authManager: AuthManager
    private lateinit var db: FirebaseFirestore

    private val TAG = "DailyTasksActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dailytasks)

        authManager = AuthManager(this)
        db = FirebaseFirestore.getInstance()

        initViews()
        setupRecyclerView()
        setupClickListeners()
        updateDate()

        loadAdmins { admins ->
            adminList = admins
            loadLocalTasks()
            listenForUpdates()
        }
    }

    private fun initViews() {
        rvTasks = findViewById(R.id.rvTasks)
        tvTotalTasks = findViewById(R.id.tvTotalTasks)
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks)
        tvPendingTasks = findViewById(R.id.tvPendingTasks)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        btnBack = findViewById(R.id.btnBack)
        btnFilter = findViewById(R.id.btnFilter)
        btnMarkComplete = findViewById(R.id.btnMarkComplete)
    }

    private fun setupRecyclerView() {
        rvTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = DailyTaskAdapter(
            tasks = taskList,
            onTaskClick = { task -> onTaskClicked(task) },
            onSendClick = { task -> onSendClicked(task) }
        )
        rvTasks.adapter = taskAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnFilter.setOnClickListener { showFilterOptions() }

        btnMarkComplete.setOnClickListener {
            selectedTask?.let { task ->
                onSendClicked(task)
            } ?: run {
                Toast.makeText(this, "⚠️ Please select a task first", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun updateDate() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        tvTodayDate.text = "📅 Today, ${dateFormat.format(Date())}"
    }

    private fun loadLocalTasks() {
        val driverId = authManager.getCurrentDriverId()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Buat tasks dulu (gunakan adminList yang telah dimuat)
        if (taskList.isEmpty()) {
            taskList.addAll(createFiveMaterialTasks(driverId, adminList))
        }

        // Ulang untuk 5 tugasan (indeks 0..4)
        for (i in 0..4) {
            val docId = "${driverId}_${today}_$i"
            val finalI = i
            db.collection("task_approvals").document(docId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val hasArrived = document.getBoolean("hasArrived") ?: false
                        val arrivalTime = document.getLong("arrivalTime") ?: 0
                        val firestoreStatus = document.getString("status") ?: "pending"

                        Log.d(TAG, "📦 Task $finalI: hasArrived=$hasArrived, status=$firestoreStatus")

                        if (finalI < taskList.size) {
                            val currentTask = taskList[finalI]
                            taskList[finalI] = currentTask.copy(
                                hasArrived = hasArrived,
                                arrivalTime = arrivalTime,
                                status = firestoreStatus
                            )
                            taskAdapter.notifyItemChanged(finalI)
                        }
                    } else {
                        Log.d(TAG, "📦 No document for task $finalI")
                    }
                }
        }

        // Delay sikit untuk pastikan semua data loaded
        Handler(Looper.getMainLooper()).postDelayed({
            taskAdapter.notifyDataSetChanged()
            updateStats()
        }, 1000)
    }

    private fun createFiveMaterialTasks(driverId: String, admins: List<Admin>): List<DailyTask> {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Jika tiada admin, guna placeholder supaya aplikasi tidak crash
        val safeAdmins = if (admins.isEmpty()) {
            listOf(
                Admin("ADM-2026-001", "Admin 1", ""),
                Admin("ADM-2026-002", "Admin 2", ""),
                Admin("ADM-2026-003", "Admin 3", ""),
                Admin("ADM-2026-004", "Admin 4", ""),
                Admin("ADM-2026-005", "Admin 5", "")
            )
        } else {
            admins
        }

        return listOf(
            DailyTask(
                id = UUID.randomUUID().toString(),
                title = "🚛 Sand Delivery",
                description = "Load 3 tons of fine sand from Quarry A to Site B",
                status = "pending",
                time = "08:30 AM",
                category = "Delivery",
                priority = "high",
                driverId = driverId,
                date = currentDate,
                loads = 2,
                materialType = "Sand",
                customer = "Supermix Concrete",
                location = "Quarry A → Site B",
                taskIndex = 0,
                assignedAdminId = safeAdmins[0 % safeAdmins.size].adminId,
                assignedAdminName = safeAdmins[0 % safeAdmins.size].fullName
            ),
            DailyTask(
                id = UUID.randomUUID().toString(),
                title = "Gravel Transport",
                description = "Transport 5 tons of 3/4\" gravel to Construction Site C",
                status = "pending",
                time = "10:00 AM",
                category = "Transport",
                priority = "high",
                driverId = driverId,
                date = currentDate,
                loads = 3,
                materialType = "Gravel",
                customer = "Hanson Quarry",
                location = "Quarry B → Site C",
                taskIndex = 1,
                assignedAdminId = safeAdmins[1 % safeAdmins.size].adminId,
                assignedAdminName = safeAdmins[1 % safeAdmins.size].fullName
            ),
            DailyTask(
                id = UUID.randomUUID().toString(),
                title = "Cement Loading",
                description = "Load and deliver 50 bags of cement to Warehouse D",
                status = "pending",
                time = "11:30 AM",
                category = "Loading",
                priority = "medium",
                driverId = driverId,
                date = currentDate,
                loads = 1,
                materialType = "Cement",
                customer = "Lafarge Cement",
                location = "Cement Plant → Warehouse D",
                taskIndex = 2,
                assignedAdminId = safeAdmins[2 % safeAdmins.size].adminId,
                assignedAdminName = safeAdmins[2 % safeAdmins.size].fullName
            ),
            DailyTask(
                id = UUID.randomUUID().toString(),
                title = "Soil Disposal",
                description = "Collect excavated soil from Site E and dispose at Landfill",
                status = "pending",
                time = "02:00 PM",
                category = "Disposal",
                priority = "low",
                driverId = driverId,
                date = currentDate,
                loads = 2,
                materialType = "Soil",
                customer = "IJM Construction",
                location = "Site E → Landfill",
                taskIndex = 3,
                assignedAdminId = safeAdmins[3 % safeAdmins.size].adminId,
                assignedAdminName = safeAdmins[3 % safeAdmins.size].fullName
            ),
            DailyTask(
                id = UUID.randomUUID().toString(),
                title = "Red Clay Transport",
                description = "Deliver red clay to Brick Factory F",
                status = "pending",
                time = "04:00 PM",
                category = "Transport",
                priority = "medium",
                driverId = driverId,
                date = currentDate,
                loads = 2,
                materialType = "Clay",
                customer = "Brick Factory F",
                location = "Clay Pit → Brick Factory",
                taskIndex = 4,
                assignedAdminId = safeAdmins[4 % safeAdmins.size].adminId,
                assignedAdminName = safeAdmins[4 % safeAdmins.size].fullName
            )
        )
    }

    private fun updateStats() {
        val total = taskList.size
        val completed = taskList.count { it.status == "approved" }
        val pending = taskList.count { it.status == "pending" || it.status == "pending_approval" }

        tvTotalTasks.text = total.toString()
        tvCompletedTasks.text = completed.toString()
        tvPendingTasks.text = pending.toString()
    }

    private fun onTaskClicked(task: DailyTask) {
        selectedTask = task
        showTaskDetailsDialog(task)
        btnMarkComplete.visibility = if (task.status == "pending") View.VISIBLE else View.GONE
    }

    private fun onSendClicked(task: DailyTask) {
        if (!task.hasArrived) {
            Toast.makeText(this, "⚠️ Please arrive at location first", Toast.LENGTH_LONG).show()
            showTaskDetailsDialog(task)
            return
        }
        // Task already sent during arrival, nothing else needed
        Toast.makeText(this, "Task already sent for approval", Toast.LENGTH_SHORT).show()
    }

    private fun loadAdmins(onComplete: (List<Admin>) -> Unit) {
        db.collection("users")
            .whereEqualTo("userType", "admin")
            .get()
            .addOnSuccessListener { documents ->
                val admins = documents.mapNotNull { doc ->
                    val adminId = doc.getString("driverId") ?: return@mapNotNull null
                    Admin(
                        adminId = adminId,
                        fullName = doc.getString("fullName") ?: "Unknown",
                        email = doc.getString("email") ?: ""
                    )
                }
                onComplete(admins)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading admins: ${e.message}")
                onComplete(emptyList())
            }
    }

    private fun showAdminSelectionDialog(task: DailyTask, adminList: List<Admin>) {
        val adminNames = adminList.map { it.fullName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Admin")
            .setItems(adminNames) { _, which ->
                val selectedAdmin = adminList[which]
                // Hanya panggil sendForApproval (yang menggunakan update)
                sendForApproval(task, selectedAdmin.adminId, selectedAdmin.fullName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendForApproval(task: DailyTask, adminId: String, adminName: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val currentDriverId = sharedPref.getString("driverId", "") ?: ""
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val docId = "${currentDriverId}_${today}_${task.taskIndex}"

        val updates = mapOf(
            "assignedAdminId" to adminId,
            "assignedAdminName" to adminName,
            "status" to "pending_approval"
        )

        db.collection("task_approvals").document(docId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Task sent for approval to $adminName")
                Toast.makeText(this, "Task sent for approval", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForUpdates() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val driverId = authManager.getCurrentDriverId()

        db.collection("task_approvals")
            .whereEqualTo("driverId", driverId)
            .get()  // GUNA get() instead of listener
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val docId = doc.id
                    // docId format: DRV-2026-051_2026-03-13_2
                    val parts = docId.split("_")
                    if (parts.size == 3) {
                        val taskIndex = parts[2].toIntOrNull()
                        val newStatus = doc.getString("status")

                        if (taskIndex != null && taskIndex in 0..4 && newStatus != null) {
                            if (taskList[taskIndex].status != newStatus) {
                                taskList[taskIndex] = taskList[taskIndex].copy(status = newStatus)
                                taskAdapter.notifyItemChanged(taskIndex)
                            }
                        }
                    }
                }
                updateStats()
            }
    }

    private fun showTaskDetailsDialog(task: DailyTask) {
        selectedTask = task
        btnMarkComplete.visibility = if (task.status == "pending") View.VISIBLE else View.GONE

        val materialEmoji = when (task.materialType) {
            "Sand" -> "🟫"
            "Gravel" -> "🪨"
            "Cement" -> "🏭"
            "Soil" -> "🟤"
            "Clay" -> "🧱"
            else -> "📦"
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_task_details, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val tvLocation = dialogView.findViewById<TextView>(R.id.tvLocation)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvTime)
        val btnArrive = dialogView.findViewById<Button>(R.id.btnArrive)
        val tvAdminName = dialogView.findViewById<TextView>(R.id.tvAdminName)

        tvTitle.text = "$materialEmoji ${task.materialType} DELIVERY"
        tvDescription.text = task.description
        tvLocation.text = "📍 Location: ${task.location}"
        tvTime.text = "⏰ Time: ${task.time}"

        tvAdminName.text = if (task.assignedAdminName.isNotEmpty()) task.assignedAdminName else "Not assigned"

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val currentDriverId = sharedPref.getString("driverId", "") ?: ""
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val taskIndex = task.taskIndex
        val docId = "${currentDriverId}_${today}_${taskIndex}"

        Log.d(TAG, "📄 Reading document: $docId")

        db.collection("task_approvals").document(docId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val hasArrived = document.getBoolean("hasArrived") ?: false
                    if (hasArrived) {
                        val arrivalTime = document.getLong("arrivalTime") ?: 0
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val arrivalTimeStr = sdf.format(Date(arrivalTime))

                        btnArrive.isEnabled = false
                        btnArrive.text = "✅ ARRIVED at $arrivalTimeStr"
                        btnArrive.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))

                        // ❌ No btnSend
                    } else {
                        btnArrive.isEnabled = true
                        btnArrive.text = "📍 I HAVE ARRIVED"
                        btnArrive.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                    }
                } else {
                    btnArrive.isEnabled = true
                    btnArrive.text = "📍 I HAVE ARRIVED"
                    btnArrive.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to read document: ${e.message}")
            }

        btnArrive.setOnClickListener {
            val index = taskList.indexOfFirst { it.id == task.id }
            if (index == -1) {
                Log.e(TAG, "❌ Task not found in taskList")
                return@setOnClickListener
            }

            val arrivalTime = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val docId = "${currentDriverId}_${today}_${task.taskIndex}"

            val taskData = hashMapOf<String, Any>(
                "driverId" to currentDriverId,
                "driverName" to (sharedPref.getString("fullName", "") ?: ""),
                "taskIndex" to task.taskIndex,
                "taskTitle" to task.title,
                "materialType" to task.materialType,
                "loads" to task.loads,
                "hasArrived" to true,
                "arrivalTime" to arrivalTime,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending_approval",
                "date" to today,
                "assignedAdminId" to task.assignedAdminId,
                "assignedAdminName" to task.assignedAdminName
            )

            db.collection("task_approvals").document(docId)
                .set(taskData, SetOptions.merge())
                .addOnSuccessListener {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val arrivalTimeStr = sdf.format(Date(arrivalTime))
                    btnArrive.text = "✅ ARRIVED at $arrivalTimeStr"
                    btnArrive.isEnabled = false
                    btnArrive.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))

                    // Update local task
                    taskList[index] = taskList[index].copy(
                        hasArrived = true,
                        arrivalTime = arrivalTime
                    )
                    taskAdapter.notifyItemChanged(index)

                    Toast.makeText(this, "📍 Arrival recorded! Task sent to admin.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to save arrival: ${e.message}")
                    Toast.makeText(this, "❌ Failed to record arrival", Toast.LENGTH_SHORT).show()
                }
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun showFilterOptions() {
        val options = arrayOf("All", "Pending", "Approved", "Sand", "Gravel", "Cement", "Soil", "Clay")
        AlertDialog.Builder(this)
            .setTitle("Filter Tasks")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAllTasks()
                    1 -> filterByStatus(listOf("pending", "pending_approval"))
                    2 -> filterByStatus(listOf("approved"))
                    3 -> filterByMaterial("Sand")
                    4 -> filterByMaterial("Gravel")
                    5 -> filterByMaterial("Cement")
                    6 -> filterByMaterial("Soil")
                    7 -> filterByMaterial("Clay")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAllTasks() {
        rvTasks.adapter = taskAdapter
        taskAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Showing all tasks", Toast.LENGTH_SHORT).show()
    }

    private fun filterByStatus(statuses: List<String>) {
        val filtered = taskList.filter { it.status in statuses }
        showFilteredResult(filtered, "filtered by status")
    }

    private fun filterByMaterial(material: String) {
        val filtered = taskList.filter { it.materialType == material }
        showFilteredResult(filtered, "$material tasks")
    }

    private fun showFilteredResult(filteredList: List<DailyTask>, message: String) {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No tasks found", Toast.LENGTH_SHORT).show()
        } else {
            val tempAdapter = DailyTaskAdapter(
                tasks = filteredList.toMutableList(),
                onTaskClick = { task -> onTaskClicked(task) },
                onSendClick = { task -> onSendClicked(task) }
            )
            rvTasks.adapter = tempAdapter
            Toast.makeText(this, "Showing ${filteredList.size} tasks", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        rvTasks.adapter = taskAdapter
        taskAdapter.notifyDataSetChanged()
    }
}