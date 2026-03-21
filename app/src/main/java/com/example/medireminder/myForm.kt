package com.example.medireminder

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medireminder.databinding.ActivityMyFormBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import android.app.TimePickerDialog
import android.view.View

class myForm : AppCompatActivity() {

    private lateinit var binding: ActivityMyFormBinding

    // Pill color options: label to hex
    private val pillColors = listOf(
        "White"  to "#FFFFFF",
        "Yellow" to "#FFD700",
        "Orange" to "#FF8C00",
        "Pink"   to "#FF69B4",
        "Red"    to "#E53935",
        "Blue"   to "#4A90D9",
        "Green"  to "#43A047",
        "Purple" to "#8E24AA",
        "Brown"  to "#795548",
        "Gray"   to "#9E9E9E"
    )

    private var selectedColor: String = "#FFFFFF"  // Default white
    private val scheduledTimes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupColorPicker()
        setupAddTimeButton()
        setupSaveButton()
    }

    // ─── Color Picker ───
    private fun setupColorPicker() {
        pillColors.forEach { (label, hex) ->
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(52, 52).also { it.marginEnd = 12 }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                    setStroke(3, Color.parseColor("#CCCCCC"))
                }
                contentDescription = label
                tag = hex

                setOnClickListener { view ->
                    selectedColor = view.tag as String
                    updateColorSelection(view)
                }
            }
            binding.colorPickerContainer.addView(circle)
        }
        // Pre-select first color
        binding.colorPickerContainer.getChildAt(0)?.let { updateColorSelection(it) }
    }

    private fun updateColorSelection(selectedView: View) {
        for (i in 0 until binding.colorPickerContainer.childCount) {
            val child = binding.colorPickerContainer.getChildAt(i)
            (child.background as? GradientDrawable)?.setStroke(3, Color.parseColor("#CCCCCC"))
            child.scaleX = 1f
            child.scaleY = 1f
        }
        (selectedView.background as? GradientDrawable)?.setStroke(5, Color.parseColor("#1A1A2E"))
        selectedView.scaleX = 1.2f
        selectedView.scaleY = 1.2f
    }

    // ─── Time Picker ───
    private fun setupAddTimeButton() {
        binding.btnAddTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val formattedTime = formatTime(hour, minute)
                    if (!scheduledTimes.contains(formattedTime)) {
                        scheduledTimes.add(formattedTime)
                        addTimeChip(formattedTime)
                    } else {
                        Toast.makeText(this, "Time already added", Toast.LENGTH_SHORT).show()
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun addTimeChip(time: String) {
        val chip = Chip(this).apply {
            text = time
            isCloseIconVisible = true
            setChipBackgroundColorResource(android.R.color.white)
            setTextColor(Color.parseColor("#1A1A2E"))
            chipStrokeWidth = 2f
            setChipStrokeColorResource(android.R.color.darker_gray)

            setOnCloseIconClickListener {
                scheduledTimes.remove(time)
                binding.chipGroupTimes.removeView(this)
            }
        }
        binding.chipGroupTimes.addView(chip)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%d:%02d %s".format(displayHour, minute, amPm)
    }

    // ─── Validation & Save ───
    private fun setupSaveButton() {
        binding.btnSaveMedication.setOnClickListener {
            if (validateForm()) saveMedication()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val name = binding.etMedicationName.text.toString().trim()
        val dosage = binding.etDosage.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilMedicationName.error = "Medication name is required"
            isValid = false
        } else binding.tilMedicationName.error = null

        if (dosage.isEmpty()) {
            binding.tilDosage.error = "Dosage is required"
            isValid = false
        } else binding.tilDosage.error = null

        return isValid
    }

    private fun saveMedication() {
        val medication = MedicationModel(
            name   = binding.etMedicationName.text.toString().trim(),
            dosage = binding.etDosage.text.toString().trim(),
            color  = selectedColor,
            times  = scheduledTimes.toList(),
            notes  = binding.etNotes.text.toString().trim()
        )

        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(uid)
            .collection("medications")
            .add(medication)
            .addOnSuccessListener {
                // Return medication to MainActivity immediately
                val resultIntent = Intent()
                resultIntent.putExtra("newMedication", medication)
                setResult(Activity.RESULT_OK, resultIntent)

                Toast.makeText(this, "✅ ${medication.name} saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving medication: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}