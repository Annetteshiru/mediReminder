package com.example.medireminder

import java.io.Serializable

data class MedicationModel(
    var id: String = "",
    var name: String = "",
    var dosage: String = "",
    var color: String = "#FFFFFF",
    var frequency: String = "Daily",
    var times: List<String> = emptyList(),
    var reminderOffsets: List<Long> = emptyList(),
    var notes: String = ""
) : Serializable
