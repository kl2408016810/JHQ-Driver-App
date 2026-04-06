package com.example.jhqdriverapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// ==================== DATA CLASS ====================
data class TaskApproval(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val taskIndex: Int = 0,
    val taskTitle: String = "",
    val materialType: String = "",
    val loads: Int = 0,
    val hasArrived: Boolean = false,
    val arrivalTime: Long = 0,
    val timestamp: Long = 0,
    val status: String = "",
    val assignedAdminId: String = "",
    val assignedAdminName: String = "",
    val date: String = ""
)

// ==================== ADAPTER ====================
class TaskApprovalAdapter(
    private val onApprove: (TaskApproval) -> Unit,
    private val onReject: (TaskApproval) -> Unit
) : RecyclerView.Adapter<TaskApprovalAdapter.TaskViewHolder>() {

    private var tasks = listOf<TaskApproval>()

    fun submitList(list: List<TaskApproval>) {
        tasks = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_approval, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDriverName: TextView = itemView.findViewById(R.id.tvDriverName)
        private val tvDriverId: TextView = itemView.findViewById(R.id.tvDriverId)
        private val tvMaterial: TextView = itemView.findViewById(R.id.tvMaterial)
        private val tvLoads: TextView = itemView.findViewById(R.id.tvLoads)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(task: TaskApproval) {
            tvDriverName.text = task.driverName
            tvDriverId.text = "ID: ${task.driverId}"
            tvMaterial.text = task.materialType
            tvLoads.text = "${task.loads} loads"
            tvDate.text = formatDate(task.timestamp)

            btnApprove.setOnClickListener {
                onApprove(task)
            }

            btnReject.setOnClickListener {
                onReject(task)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}

// ==================== ACTIVITY ====================
class TaskApprovalsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnRefresh: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TaskApprovalAdapter

    private lateinit var tvPendingCount: TextView
    private lateinit var tvTotalToday: TextView
    private var pendingCountListener: ListenerRegistration? = null
    private var totalTodayListener: ListenerRegistration? = null

    private var pendingTasksListener: ListenerRegistration? = null
    private var currentAdminId: String = ""

    companion object {
        private const val TAG = "TaskApprovalsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_approvals)

        db = FirebaseFirestore.getInstance()

        // Get current admin ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentAdminId = sharedPref.getString("driverId", "") ?: ""

        initViews()
        setupClickListeners()
        setupRecyclerView()
        setupRealTimeListener()

    }

    override fun onDestroy() {
        super.onDestroy()
        pendingTasksListener?.remove()
        pendingCountListener?.remove()
        totalTodayListener?.remove()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnRefresh = findViewById(R.id.btnRefresh)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvTotalToday = findViewById(R.id.tvTotalToday)

        tvTitle.text = "Task Approvals"
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnRefresh.setOnClickListener {
            refreshData()
        }
    }

    private fun setupRecyclerView() {
        adapter = TaskApprovalAdapter(
            onApprove = { task -> showApproveConfirmation(task) },
            onReject = { task -> showRejectConfirmation(task) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupRealTimeListener() {
        Log.d(TAG, "Setting up real-time listener for admin: $currentAdminId")

        // Listener untuk senarai pending tasks (RecyclerView)
        pendingTasksListener = db.collection("task_approvals")
            .whereEqualTo("assignedAdminId", currentAdminId)
            .whereEqualTo("status", "pending_approval")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listener error: ${error.message}")
                    tvEmptyState.text = "Error: ${error.message}"
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.d(TAG, "No pending tasks found")
                    tvEmptyState.text = "No pending approvals"
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    Log.d(TAG, "Found ${snapshots.size()} pending tasks")
                    val tasks = snapshots.map { document -> documentToTask(document) }
                    recyclerView.visibility = View.VISIBLE
                    emptyStateLayout.visibility = View.GONE
                    adapter.submitList(tasks)
                }
            }

        // Listener untuk jumlah pending (badge/stat)
        pendingCountListener = db.collection("task_approvals")
            .whereEqualTo("assignedAdminId", currentAdminId)
            .whereEqualTo("status", "pending_approval")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error counting pending: ${error.message}")
                    return@addSnapshotListener
                }
                val count = snapshots?.size() ?: 0
                tvPendingCount.text = count.toString()
            }

        // Listener untuk jumlah total hari ini (semua status)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        totalTodayListener = db.collection("task_approvals")
            .whereEqualTo("assignedAdminId", currentAdminId)
            .whereEqualTo("date", today)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error counting total today: ${error.message}")
                    return@addSnapshotListener
                }
                val total = snapshots?.size() ?: 0
                tvTotalToday.text = total.toString()
            }
    }


    private fun documentToTask(document: DocumentSnapshot): TaskApproval {
        return TaskApproval(
            id = document.id,
            driverId = document.getString("driverId") ?: "",
            driverName = document.getString("driverName") ?: "Unknown",
            taskIndex = document.getLong("taskIndex")?.toInt() ?: 0,
            taskTitle = document.getString("taskTitle") ?: "",
            materialType = document.getString("materialType") ?: "",
            loads = document.getLong("loads")?.toInt() ?: 0,
            hasArrived = document.getBoolean("hasArrived") ?: false,
            arrivalTime = document.getLong("arrivalTime") ?: 0,
            timestamp = document.getLong("timestamp") ?: 0,
            status = document.getString("status") ?: "pending",
            assignedAdminId = document.getString("assignedAdminId") ?: "",
            assignedAdminName = document.getString("assignedAdminName") ?: "",
            date = document.getString("date") ?: ""
        )
    }

    private fun refreshData() {
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
        pendingTasksListener?.remove()
        setupRealTimeListener()
    }

    private fun showApproveConfirmation(task: TaskApproval) {
        AlertDialog.Builder(this)
            .setTitle("Approve Task")
            .setMessage("Approve task for ${task.driverName}?\n\nMaterial: ${task.materialType}\nLoads: ${task.loads}")
            .setPositiveButton("Approve") { _, _ ->
                approveTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectConfirmation(task: TaskApproval) {
        AlertDialog.Builder(this)
            .setTitle("Reject Task")
            .setMessage("Reject task for ${task.driverName}?")
            .setPositiveButton("Reject") { _, _ ->
                rejectTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveTask(task: TaskApproval) {
        Log.d(TAG, "Approving task: ${task.id} with ${task.loads} loads")

        // Get user document first
        db.collection("users")
            .whereEqualTo("driverId", task.driverId)
            .get()
            .addOnSuccessListener { users ->
                if (!users.isEmpty) {
                    val userDoc = users.first()
                    val userRef = userDoc.reference

                    // Get current totalLoads
                    val currentLoads = if (userDoc.contains("totalLoads")) {
                        (userDoc.getLong("totalLoads") ?: 0).toInt()
                    } else {
                        0
                    }

                    val newTotal = currentLoads + task.loads

                    // Start batch operation
                    val batch = db.batch()

                    // Update task_approvals
                    val taskApprovalRef = db.collection("task_approvals").document(task.id)
                    batch.update(taskApprovalRef, "status", "approved")

                    // Update user's totalLoads
                    batch.update(userRef, "totalLoads", newTotal)

                    // Update daily_tasks if exists
                    db.collection("daily_tasks")
                        .whereEqualTo("driverId", task.driverId)
                        .whereEqualTo("taskTitle", task.taskTitle)
                        .whereEqualTo("date", task.date)
                        .get()
                        .addOnSuccessListener { dailyTasks ->
                            if (!dailyTasks.isEmpty) {
                                val dailyTaskRef = dailyTasks.first().reference
                                batch.update(dailyTaskRef, "status", "approved")
                            }

                            // Commit batch
                            batch.commit()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@TaskApprovalsActivity,
                                        "✅ Approved: +${task.loads} loads for ${task.driverName}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.d(TAG, "✅ Total loads updated: $currentLoads → $newTotal")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "❌ Batch failed: ${e.message}")
                                    Toast.makeText(this@TaskApprovalsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                } else {
                    Log.e(TAG, "❌ User not found: ${task.driverId}")
                    Toast.makeText(this, "Error: Driver not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error finding user: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectTask(task: TaskApproval) {
        db.collection("task_approvals").document(task.id)
            .update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "❌ Task rejected for ${task.driverName}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Task rejected successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error rejecting task: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}