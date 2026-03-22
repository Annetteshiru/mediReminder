package com.example.medireminder

import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medireminder.databinding.ActivityMyFormBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MedicationFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyFormBinding

    private var editMedicationId: String? = null
    private var isEditMode = false

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

    private var selectedColor = "#FFFFFF"
    private val scheduledTimes = mutableListOf<String>()
    private val selectedOffsets = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editMedicationId = intent.getStringExtra("medicationId")
        isEditMode = editMedicationId != null

        binding.btnBack.setOnClickListener { finish() }

        setupFrequencySpinner()
        setupColorPicker()
        setupAddTimeButton()
        setupReminderOffsets()
        setupSaveButton()

        if (isEditMode) {
            binding.textView2.text = "Edit Medication"
            binding.textView3.text = "Update the details below"
            val medication = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("medication", MedicationModel::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("medication") as? MedicationModel
            }
            medication?.let { prefillForm(it) }
        }
    }

    private fun setupFrequencySpinner() {
        val options = arrayOf("Daily", "As needed")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = adapter
    }

    private fun prefillForm(medication: MedicationModel) {
        binding.etMedicationName.setText(medication.name)
        binding.etDosage.setText(medication.dosage)
        binding.etNotes.setText(medication.notes)
        selectedColor = medication.color

        val freqOptions = listOf("Daily", "As needed")
        val freqIndex = freqOptions.indexOf(medication.frequency).coerceAtLeast(0)
        binding.spinnerFrequency.setSelection(freqIndex)

        medication.times.forEach { time ->
            scheduledTimes.add(time)
            addTimeChip(time)
        }

        medication.reminderOffsets.forEach { offset ->
            selectedOffsets.add(offset)
            when (offset) {
                60L -> binding.cbReminder1hr.isChecked = true
                30L -> binding.cbReminder30min.isChecked = true
                5L  -> binding.cbReminder5min.isChecked = true
            }
        }
    }

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

        if (!isEditMode) {
            binding.colorPickerContainer.getChildAt(0)?.let { updateColorSelection(it) }
        } else {
            for (i in 0 until binding.colorPickerContainer.childCount) {
                val child = binding.colorPickerContainer.getChildAt(i)
                if (child.tag == selectedColor) { updateColorSelection(child); break }
            }
        }
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

    private fun setupAddTimeButton() {
        binding.btnAddTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val formattedTime = formatTime(hour, minute)
                if (!scheduledTimes.contains(formattedTime)) {
                    scheduledTimes.add(formattedTime)
                    addTimeChip(formattedTime)
                } else {
                    Toast.makeText(this, "Time already added", Toast.LENGTH_SHORT).show()
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun setupReminderOffsets() {
        binding.cbReminder1hr.setOnCheckedChangeListener { _, checked ->
            if (checked) selectedOffsets.add(60L) else selectedOffsets.remove(60L)
        }
        binding.cbReminder30min.setOnCheckedChangeListener { _, checked ->
            if (checked) selectedOffsets.add(30L) else selectedOffsets.remove(30L)
        }
        binding.cbReminder5min.setOnCheckedChangeListener { _, checked ->
            if (checked) selectedOffsets.add(5L) else selectedOffsets.remove(5L)
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

    private fun setupSaveButton() {
        binding.btnSaveMedication.setOnClickListener {
            if (validateForm()) {
                binding.btnSaveMedication.isEnabled = false
                binding.btnSaveMedication.text = "Saving…"
                saveMedication()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        if (binding.etMedicationName.text.toString().trim().isEmpty()) {
            binding.tilMedicationName.error = "Medication name is required"
            isValid = false
        } else binding.tilMedicationName.error = null

        if (binding.etDosage.text.toString().trim().isEmpty()) {
            binding.tilDosage.error = "Dosage is required"
            isValid = false
        } else binding.tilDosage.error = null

        return isValid
    }

    private fun saveMedication() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val frequency = binding.spinnerFrequency.selectedItem?.toString() ?: "Daily"
        val medication = MedicationModel(
            name = binding.etMedicationName.text.toString().trim(),
            dosage = binding.etDosage.text.toString().trim(),
            color = selectedColor,
            frequency = frequency,
            times = scheduledTimes.toList(),
            reminderOffsets = selectedOffsets.toList(),
            notes = binding.etNotes.text.toString().trim()
        )

        val db = FirebaseFirestore.getInstance()
        val medsRef = db.collection("users").document(uid).collection("medications")

        if (isEditMode && editMedicationId != null) {
            medication.id = editMedicationId!!
            NotificationHelper.cancelReminders(this, medication)
            medsRef.document(editMedicationId!!)
                .set(medication)
                .addOnSuccessListener {
                    NotificationHelper.scheduleReminders(this, medication)
                    Toast.makeText(this, "${medication.name} updated!", Toast.LENGTH_SHORT).show()
                    MainActivity.switchToMedsOnResume = true
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSaveMedication.isEnabled = true
                    binding.btnSaveMedication.text = "Save Medication"
                }
        } else {
            medsRef.add(medication)
                .addOnSuccessListener { docRef ->
                    medication.id = docRef.id
                    docRef.update("id", docRef.id)
                    NotificationHelper.scheduleReminders(this, medication)
                    Toast.makeText(this, "${medication.name} saved!", Toast.LENGTH_SHORT).show()
                    MainActivity.switchToMedsOnResume = true
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSaveMedication.isEnabled = true
                    binding.btnSaveMedication.text = "Save Medication"
                }
        }
    }
}
