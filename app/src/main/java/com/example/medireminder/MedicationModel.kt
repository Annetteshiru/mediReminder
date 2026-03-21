package com.example.medireminder

import java.io.Serializable

data class MedicationModel(
    var name: String = "",
    var dosage: String = "",
    var color: String = "#FFFFFF",
    var times: List<String> = emptyList(),
    var notes: String = ""
) : Serializable