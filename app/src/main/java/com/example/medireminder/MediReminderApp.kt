package com.example.medireminder


import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MediReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force light mode across the entire app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}