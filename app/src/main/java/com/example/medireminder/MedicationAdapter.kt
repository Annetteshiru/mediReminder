package com.example.medireminder

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.medireminder.databinding.ItemMedicationBinding

class MedicationAdapter(
    private val medications: MutableList<MedicationModel>
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {


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

        // First letter
        binding.tvFirstLetter.text = medication.name.first().uppercaseChar().toString()

        // Set circle color to the pill's color
        val circleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(medication.color))
        }
        binding.tvFirstLetter.background = circleDrawable

        // Name and dosage
        binding.tvMedicationName.text = medication.name
        binding.tvDosage.text = "${medication.dosage}mg"

        // ─── Taken button toggle
        var isTaken = false

        // Reset state when view is recycled
        updateTakenButton(binding, isTaken)

        binding.btnTaken.setOnClickListener {
            isTaken = !isTaken
            updateTakenButton(binding, isTaken)
        }
    }


    private fun updateTakenButton(
        binding: ItemMedicationBinding,
        isTaken: Boolean
    ) {
        if (isTaken) {
            binding.btnTaken.text = "Taken ✓"
            binding.btnTaken.setBackgroundColor(Color.parseColor("#43A047")) // Green
        } else {
            binding.btnTaken.text = "Take"
            binding.btnTaken.setBackgroundColor(Color.parseColor("#4A90D9")) // Blue
        }
    }

    //add new medication to list
    fun addMedication(medication: MedicationModel) {
        medications.add(medication)
        notifyItemInserted(medications.size - 1)
    }
    fun setMedications(newMeds: List<MedicationModel>) {
        medications.clear()
        medications.addAll(newMeds)
        notifyDataSetChanged()
    }
}