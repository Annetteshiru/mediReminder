package com.example.medireminder

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.medireminder.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MedicationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val medications = mutableListOf<MedicationModel>()
    private val takenMedicationIds = mutableSetOf<String>()
    private var medsListener: ListenerRegistration? = null
    private var logsListener: ListenerRegistration? = null

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val todayDate: String get() = sdf.format(Date())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserGreeting()

        adapter = MedicationAdapter(
            mutableListOf(),
            onTake   = { med -> markAsTaken(med) },
            onDelete = { med -> confirmDelete(med) },
            onEdit   = { med -> openEditForm(med) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddMedication.setOnClickListener {
            startActivity(Intent(requireContext(), MedicationFormActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        fetchMedications()
        fetchTodayLogs()
        fetchWeeklyData()
    }

    private fun setupUserGreeting() {
        val user = auth.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@") ?: "User"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else      -> "Good evening,"
        }
        binding.textView6.text = greeting
        binding.textView4.text = name

        // Load Google profile photo into the avatar button if available
        val photoUrl = user?.photoUrl?.toString()
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_profile)
                .into(binding.btnProfile)
        }
    }

    private fun fetchMedications() {
        val uid = auth.currentUser?.uid ?: return
        medsListener = db.collection("users").document(uid).collection("medications")
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                medications.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(MedicationModel::class.java)?.let { med ->
                        med.id = doc.id
                        medications.add(med)
                    }
                }
                adapter.setMedications(medications)
                binding.tvPlaceholder.visibility = if (medications.isEmpty()) View.VISIBLE else View.GONE
                updateAnalytics()
            }
    }

    private fun fetchTodayLogs() {
        val uid = auth.currentUser?.uid ?: return
        logsListener = db.collection("users").document(uid).collection("logs")
            .whereEqualTo("date", todayDate)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                if (error != null) return@addSnapshotListener
                takenMedicationIds.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.getString("medicationId")?.let { takenMedicationIds.add(it) }
                }
                adapter.updateTakenIds(takenMedicationIds)
                updateAnalytics()
            }
    }

    private fun fetchWeeklyData() {
        val uid = auth.currentUser?.uid ?: return
        // Build last-7-day labels and date strings
        val dayLabels = mutableListOf<String>()
        val dateStrings = mutableListOf<String>()
        val dayNameFmt = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            dayLabels.add(dayNameFmt.format(c.time).take(3))
            dateStrings.add(sdf.format(c.time))
        }
        val sevenDaysAgo = dateStrings.first()

        db.collection("users").document(uid).collection("logs")
            .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val takenByDate = snapshot.documents.groupBy { it.getString("date") ?: "" }
                val total = medications.size.coerceAtLeast(1)
                val chartData = dateStrings.mapIndexed { i, date ->
                    val takenCount = (takenByDate[date]?.size ?: 0)
                    val pct = ((takenCount.toFloat() / total) * 100f).coerceIn(0f, 100f)
                    WeeklyBarChartView.DayData(dayLabels[i], pct)
                }
                binding.weeklyChart.setData(chartData)
            }
    }

    private fun markAsTaken(medication: MedicationModel) {
        val uid = auth.currentUser?.uid ?: return
        val log = hashMapOf(
            "userId"        to uid,
            "medicationId"  to medication.id,
            "scheduledTime" to (medication.times.firstOrNull() ?: ""),
            "taken"         to true,
            "takenAt"       to System.currentTimeMillis(),
            "date"          to todayDate
        )
        db.collection("users").document(uid).collection("logs").add(log)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "✔ ${medication.name} marked as taken!", Toast.LENGTH_SHORT).show()
                fetchWeeklyData()   // Refresh the line graph immediately
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(medication: MedicationModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Medication")
            .setMessage("Delete ${medication.name}?")
            .setPositiveButton("Delete") { _, _ -> deleteMedication(medication) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMedication(medication: MedicationModel) {
        val uid = auth.currentUser?.uid ?: return
        NotificationHelper.cancelReminders(requireContext(), medication)
        db.collection("users").document(uid).collection("medications")
            .document(medication.id).delete()
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "${medication.name} deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditForm(medication: MedicationModel) {
        startActivity(Intent(requireContext(), MedicationFormActivity::class.java).apply {
            putExtra("medicationId", medication.id)
            putExtra("medication", medication)
        })
    }

    private fun updateAnalytics() {
        val total = medications.size
        val taken = takenMedicationIds.count { id -> medications.any { it.id == id } }
        val missed = total - taken
        val rate = if (total > 0) (taken * 100) / total else 0

        binding.progressBar.max = if (total > 0) total else 1
        binding.progressBar.progress = taken
        binding.tvAdherenceCount.text = "$taken of $total taken today"

        binding.tvTotalMeds.text = total.toString()
        binding.tvTakenToday.text = taken.toString()
        binding.tvMissedToday.text = missed.toString()

        binding.textView7.text = when {
            total == 0  -> "Add your medications to get started"
            rate == 100 -> "All done! Great job!"
            rate >= 75  -> "Great progress, keep it up!"
            rate >= 50  -> "Halfway there!"
            taken == 0  -> "Don't forget your medications"
            else        -> "Going on well"
        }
    }

    override fun onStop() {
        super.onStop()
        medsListener?.remove()
        logsListener?.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
