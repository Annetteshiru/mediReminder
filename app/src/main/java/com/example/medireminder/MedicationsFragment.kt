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
import com.example.medireminder.databinding.FragmentMedicationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationsFragment : Fragment() {

    private var _binding: FragmentMedicationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MedicationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val medications = mutableListOf<MedicationModel>()
    private val takenMedicationIds = mutableSetOf<String>()
    private var medsListener: ListenerRegistration? = null
    private var logsListener: ListenerRegistration? = null

    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MedicationAdapter(
            mutableListOf(),
            onTake   = { med -> markAsTaken(med) },
            onDelete = { med -> confirmDelete(med) },
            onEdit   = { med -> openDetail(med) }
        )
        binding.recyclerViewMeds.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMeds.adapter = adapter

        binding.btnAddMed.setOnClickListener {
            startActivity(Intent(requireContext(), myForm::class.java))
        }

        binding.btnBack.setOnClickListener {
            (requireActivity() as? MainActivity)?.switchToHomeTab()
        }

        fetchMedications()
        fetchTodayLogs()
    }

    private fun fetchMedications() {
        val uid = auth.currentUser?.uid ?: return
        medsListener = db.collection("users").document(uid).collection("medications")
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                binding.tvMedsPlaceholder.visibility = if (medications.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun fetchTodayLogs() {
        val uid = auth.currentUser?.uid ?: return
        logsListener = db.collection("logs")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", todayDate)
            .whereEqualTo("taken", true)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                if (error != null) return@addSnapshotListener
                takenMedicationIds.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.getString("medicationId")?.let { takenMedicationIds.add(it) }
                }
                adapter.updateTakenIds(takenMedicationIds)
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
        db.collection("logs").add(log)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "${medication.name} marked as taken!", Toast.LENGTH_SHORT).show()
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
    }

    private fun openDetail(medication: MedicationModel) {
        startActivity(Intent(requireContext(), MedicationDetailActivity::class.java).apply {
            putExtra("medication", medication)
        })
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
