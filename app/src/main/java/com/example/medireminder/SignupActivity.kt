package com.example.medireminder

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medireminder.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnSignUp.setOnClickListener {
            if (validateForm()) {
                binding.btnSignUp.isEnabled = false
                binding.btnSignUp.text = "Creating account…"
                createAccount()
            }
        }

        binding.txtGoToSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val name = binding.EtName.text.toString().trim()
        val email = binding.EtEmail.text.toString().trim()
        val password = binding.EtPass.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilSignupName.error = "Name is required"
            isValid = false
        } else if (name.length < 2) {
            binding.tilSignupName.error = "Name must be at least 2 characters"
            isValid = false
        } else {
            binding.tilSignupName.error = null
        }

        if (email.isEmpty()) {
            binding.tilSignupEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilSignupEmail.error = "Enter a valid email address"
            isValid = false
        } else {
            binding.tilSignupEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilSignupPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilSignupPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilSignupPassword.error = null
        }

        return isValid
    }

    private fun createAccount() {
        val name = binding.EtName.text.toString().trim()
        val email = binding.EtEmail.text.toString().trim()
        val password = binding.EtPass.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser ?: return@addOnSuccessListener
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(mapOf(
                        "userId"    to user.uid,
                        "email"     to email,
                        "name"      to name,
                        "photoUrl"  to "",
                        "createdAt" to System.currentTimeMillis()
                    ))
                Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSignUp.isEnabled = true
                binding.btnSignUp.text = "Create Account"
                val message = when {
                    e.message?.contains("email address is already") == true ->
                        "This email is already registered. Try signing in."
                    e.message?.contains("badly formatted") == true ->
                        "Please enter a valid email address."
                    e.message?.contains("weak-password") == true ||
                    e.message?.contains("at least 6") == true ->
                        "Password must be at least 6 characters."
                    else -> e.message ?: "Sign up failed. Please try again."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }
}
