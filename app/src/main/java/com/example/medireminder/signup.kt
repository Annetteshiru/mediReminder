package com.example.medireminder

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class signup : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        val nameEt = findViewById<EditText>(R.id.EtName)
        val emailEt = findViewById<EditText>(R.id.EtEmail)
        val passwordEt = findViewById<EditText>(R.id.EtPass)
        val signBtn = findViewById<Button>(R.id.btnSignUp)
        val goToSignIn = findViewById<TextView>(R.id.txtGoToSignIn)

        signBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser

                        // Set Firebase display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        user?.updateProfile(profileUpdates)

                        // Save user document to Firestore
                        val uid = user?.uid ?: ""
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(mapOf(
                                "userId"    to uid,
                                "email"     to email,
                                "name"      to name,
                                "createdAt" to System.currentTimeMillis()
                            ))

                        Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, login::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        goToSignIn.setOnClickListener {
            startActivity(Intent(this, login::class.java))
            finish()
        }
    }
}
