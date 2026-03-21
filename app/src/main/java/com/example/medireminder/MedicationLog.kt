package com.example.medireminder

data class MedicationLog(
    var id: String = "",
    var userId: String = "",
    var medicationId: String = "",
    var scheduledTime: String = "",
    var taken: Boolean = false,
    var takenAt: Long = 0L,
    var date: String = ""
)
