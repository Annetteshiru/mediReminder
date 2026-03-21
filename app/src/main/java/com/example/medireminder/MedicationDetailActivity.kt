package com.example.medireminder

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medireminder.databinding.ActivityMedicationDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MedicationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicationDetailBinding
    private lateinit var medication: MedicationModel
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        medication = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("medication", MedicationModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("medication") as? MedicationModel
        } ?: run { finish(); return }

        populateDetails()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnDetailEdit.setOnClickListener {
            startActivity(Intent(this, myForm::class.java).apply {
                putExtra("medicationId", medication.id)
                putExtra("medication", medication)
            })
            finish()
        }

        binding.btnDetailDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Medication")
                .setMessage("Are you sure you want to delete ${medication.name}?")
                .setPositiveButton("Delete") { _, _ -> deleteMedication() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun populateDetails() {
        // Header identity
        binding.tvDetailName.text = medication.name
        binding.tvDetailDosage.text = "${medication.dosage}mg"
        binding.tvDetailInitial.text = medication.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        // Color circle for initial
        val circleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(medication.color))
        }
        binding.tvDetailInitial.background = circleDrawable

        // Pill color swatch
        val colorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(medication.color))
            setStroke(2, Color.parseColor("#CCCCCC"))
        }
        binding.vDetailColor.background = colorDrawable

        // Frequency
        binding.tvDetailFrequency.text = medication.frequency.ifBlank { "Daily" }

        // Schedule times
        binding.tvDetailTimes.text = if (medication.times.isEmpty()) {
            "No times set"
        } else {
            medication.times.joinToString("\n")
        }

        // Reminders
        binding.tvDetailReminders.text = if (medication.reminderOffsets.isEmpty()) {
            "No reminders set"
        } else {
            medication.reminderOffsets.sortedDescending().joinToString("\n") { offset ->
                when (offset) {
                    60L -> "1 hour before"
                    30L -> "30 minutes before"
                    5L  -> "5 minutes before"
                    0L  -> "At scheduled time"
                    else -> "$offset minutes before"
                }
            }
        }

        // Notes
        binding.tvDetailNotes.text = medication.notes.ifBlank { "No notes" }
    }

    private fun deleteMedication() {
        val uid = auth.currentUser?.uid ?: return
        NotificationHelper.cancelReminders(this, medication)
        db.collection("users").document(uid).collection("medications")
            .document(medication.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${medication.name} deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
