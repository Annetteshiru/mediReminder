package com.example.medireminder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.medireminder.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatDelegate

// Force light mode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MedicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Set up RecyclerView and adapter
        adapter = MedicationAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Add Medication button
        binding.btnAddMedication.setOnClickListener {
            val intent = Intent(this, myForm::class.java)
            startActivity(intent)
        }
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()  // Sign out the user
            startActivity(Intent(this, login::class.java))  // Go back to login screen
            finish()  // Prevent user from coming back with back button
        }
        // Fetch medications from Firestore
        fetchMedications()
    }

    private fun fetchMedications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .collection("medications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading medications: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val meds = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MedicationModel::class.java)
                } ?: emptyList()

                // Update adapter safely via helper function
                adapter.setMedications(meds)

                // Show placeholder if empty
                binding.tvPlaceholder.visibility = if (meds.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}