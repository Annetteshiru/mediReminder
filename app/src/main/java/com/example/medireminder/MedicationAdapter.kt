package com.example.medireminder

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.medireminder.databinding.ItemMedicationBinding

class MedicationAdapter(
    private val medications: MutableList<MedicationModel>,
    private val onTake: (MedicationModel) -> Unit,
    private val onDelete: (MedicationModel) -> Unit,
    private val onEdit: (MedicationModel) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {

    private val takenMedicationIds = mutableSetOf<String>()

    inner class MedicationViewHolder(val binding: ItemMedicationBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MedicationViewHolder(binding)
    }

    override fun getItemCount() = medications.size

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        val binding = holder.binding

        binding.tvFirstLetter.text = medication.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        val circleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(medication.color))
        }
        binding.tvFirstLetter.background = circleDrawable

        binding.tvMedicationName.text = medication.name
        binding.tvDosage.text = "${medication.dosage}mg"

        if (medication.times.isNotEmpty()) {
            binding.tvTimes.text = medication.times.joinToString(" · ")
            binding.tvTimes.visibility = View.VISIBLE
        } else {
            binding.tvTimes.visibility = View.GONE
        }

        val isTaken = takenMedicationIds.contains(medication.id)
        updateTakenButton(binding, isTaken)

        binding.btnTaken.setOnClickListener {
            if (!takenMedicationIds.contains(medication.id)) {
                onTake(medication)
            }
        }

        binding.btnDelete.setOnClickListener {
            onDelete(medication)
        }

        binding.root.setOnClickListener {
            onEdit(medication)
        }
    }

    private fun updateTakenButton(binding: ItemMedicationBinding, isTaken: Boolean) {
        if (isTaken) {
            binding.btnTaken.text = "Taken ✓"
            binding.btnTaken.setBackgroundColor(Color.parseColor("#43A047"))
        } else {
            binding.btnTaken.text = "Take"
            binding.btnTaken.setBackgroundColor(Color.parseColor("#4A90D9"))
        }
    }

    fun setMedications(newMeds: List<MedicationModel>) {
        val copy = newMeds.toList()
        medications.clear()
        medications.addAll(copy)
        notifyDataSetChanged()
    }

    fun updateTakenIds(takenIds: Set<String>) {
        takenMedicationIds.clear()
        takenMedicationIds.addAll(takenIds)
        notifyDataSetChanged()
    }
}
