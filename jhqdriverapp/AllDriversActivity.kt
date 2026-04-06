package com.example.jhqdriverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

// ==================== DATA CLASS ====================
data class Driver(
    val uid: String = "",
    val driverId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: String = "",
    val vehicleNo: String = "",
    val status: String = "inactive",
    val totalLoads: Int = 0
)

// ==================== ACTIVITY ====================
class AllDriversActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var btnAll: Button
    private lateinit var btnActive: Button
    private lateinit var btnInactive: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvEmptyStateMessage: TextView
    private lateinit var tvTotalDriversCount: TextView
    private lateinit var tvActiveDriversCount: TextView
    private lateinit var tvInactiveDriversCount: TextView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var driversListener: ListenerRegistration? = null

    private var currentFilter = "all"
    private var allDrivers = listOf<Driver>()
    private var filteredDrivers = listOf<Driver>()

    companion object {
        private const val TAG = "AllDriversActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_drivers)

        db = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()
        setupRecyclerView()
        loadDriversFromFirebase()
    }

    override fun onDestroy() {
        super.onDestroy()
        driversListener?.remove()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnAll = findViewById(R.id.btnAll)
        btnActive = findViewById(R.id.btnActive)
        btnInactive = findViewById(R.id.btnInactive)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvEmptyStateMessage = findViewById(R.id.tvEmptyStateMessage)
        tvTotalDriversCount = findViewById(R.id.tvTotalDriversCount)
        tvActiveDriversCount = findViewById(R.id.tvActiveDriversCount)
        tvInactiveDriversCount = findViewById(R.id.tvInactiveDriversCount)

        updateFilterButtons("all")
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAll.setOnClickListener {
            updateFilterButtons("all")
            currentFilter = "all"
            applyFilter()
        }

        btnActive.setOnClickListener {
            updateFilterButtons("active")
            currentFilter = "active"
            applyFilter()
        }

        btnInactive.setOnClickListener {
            updateFilterButtons("inactive")
            currentFilter = "inactive"
            applyFilter()
        }
    }

    private fun updateFilterButtons(selected: String) {
        // Reset semua button
        btnAll.setBackgroundResource(R.drawable.filter_button_wine)
        btnActive.setBackgroundResource(R.drawable.filter_button_wine)
        btnInactive.setBackgroundResource(R.drawable.filter_button_wine)

        btnAll.setTextColor(android.graphics.Color.WHITE)
        btnActive.setTextColor(android.graphics.Color.WHITE)
        btnInactive.setTextColor(android.graphics.Color.WHITE)

        // Highlight yang dipilih
        when (selected) {
            "all" -> {
                btnAll.setBackgroundResource(R.drawable.filter_button_wine_selected)
                btnAll.setTextColor(android.graphics.Color.WHITE)
            }
            "active" -> {
                btnActive.setBackgroundResource(R.drawable.filter_button_wine_selected)
                btnActive.setTextColor(android.graphics.Color.WHITE)
            }
            "inactive" -> {
                btnInactive.setBackgroundResource(R.drawable.filter_button_wine_selected)
                btnInactive.setTextColor(android.graphics.Color.WHITE)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDriversFromFirebase() {
        showLoading(true)

        driversListener = db.collection("users")
            .whereEqualTo("userType", "driver")
            .addSnapshotListener { snapshots, error ->
                showLoading(false)

                if (error != null) {
                    Log.e(TAG, "Error loading drivers: ${error.message}")
                    tvEmptyStateMessage.text = "Error: ${error.message}"
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    allDrivers = emptyList()
                    updateUI()
                    return@addSnapshotListener
                }

                val driversList = mutableListOf<Driver>()

                for (doc in snapshots) {
                    val driver = Driver(
                        uid = doc.id,
                        driverId = doc.getString("driverId") ?: "",
                        fullName = doc.getString("fullName") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        userType = doc.getString("userType") ?: "driver",
                        vehicleNo = doc.getString("vehicleNo") ?: "",
                        totalLoads = doc.getLong("totalLoads")?.toInt() ?: 0
                    )
                    driversList.add(driver)
                }

                checkDriversStatus(driversList)
            }
    }

    private fun checkDriversStatus(drivers: List<Driver>) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val updatedDrivers = mutableListOf<Driver>()
        var processedCount = 0

        if (drivers.isEmpty()) {
            allDrivers = emptyList()
            updateUI()
            return
        }

        for (driver in drivers) {
            db.collection("shifts")
                .whereEqualTo("driverId", driver.driverId)
                .whereEqualTo("date", today)
                .whereEqualTo("checkOutTime", null)
                .get()
                .addOnSuccessListener { shifts ->
                    val status = if (!shifts.isEmpty()) "active" else "inactive"
                    val updatedDriver = driver.copy(status = status)
                    updatedDrivers.add(updatedDriver)
                    processedCount++

                    if (processedCount == drivers.size) {
                        allDrivers = updatedDrivers.sortedByDescending { it.status == "active" }
                        updateUI()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking status for ${driver.driverId}: ${e.message}")
                    val updatedDriver = driver.copy(status = "inactive")
                    updatedDrivers.add(updatedDriver)
                    processedCount++

                    if (processedCount == drivers.size) {
                        allDrivers = updatedDrivers.sortedByDescending { it.status == "active" }
                        updateUI()
                    }
                }
        }
    }

    private fun updateUI() {
        val totalCount = allDrivers.size
        val activeCount = allDrivers.count { it.status == "active" }
        val inactiveCount = allDrivers.count { it.status == "inactive" }

        tvTotalDriversCount.text = totalCount.toString()
        tvActiveDriversCount.text = activeCount.toString()
        tvInactiveDriversCount.text = inactiveCount.toString()

        applyFilter()
    }

    private fun applyFilter() {
        filteredDrivers = when (currentFilter) {
            "active" -> allDrivers.filter { it.status == "active" }
            "inactive" -> allDrivers.filter { it.status == "inactive" }
            else -> allDrivers
        }

        if (filteredDrivers.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE

            tvEmptyStateMessage.text = when (currentFilter) {
                "active" -> "No active drivers"
                "inactive" -> "No inactive drivers"
                else -> "No drivers found"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE

            // GUNA ADAPTER DARI FILE AllDriversAdapter.kt
            val adapter = AllDriversAdapter(filteredDrivers) { driver ->
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("driverId", driver.driverId)
                startActivity(intent)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            tvEmptyStateMessage.text = "Loading drivers..."
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }
}