package com.example.medireminder

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.medireminder.databinding.ActivityLoginBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Skip login screen if already signed in
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            if (validateForm()) {
                binding.btnSignUp.isEnabled = false
                binding.btnSignUp.text = "Signing in…"
                signIn()
            }
        }

        binding.txtForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.txtGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val email = binding.EtEmail.text.toString().trim()
        val password = binding.EtPass.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilLoginEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilLoginEmail.error = "Enter a valid email address"
            isValid = false
        } else {
            binding.tilLoginEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilLoginPassword.error = "Password is required"
            isValid = false
        } else {
            binding.tilLoginPassword.error = null
        }

        return isValid
    }

    private fun signIn() {
        val email = binding.EtEmail.text.toString().trim()
        val password = binding.EtPass.text.toString().trim()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSignUp.isEnabled = true
                binding.btnSignUp.text = "Sign In"
                val message = when {
                    e.message?.contains("no user record") == true ||
                    e.message?.contains("user-not-found") == true ->
                        "No account found with this email."
                    e.message?.contains("password is invalid") == true ||
                    e.message?.contains("wrong-password") == true ->
                        "Incorrect password. Please try again."
                    e.message?.contains("badly formatted") == true ->
                        "Please enter a valid email address."
                    e.message?.contains("blocked") == true ||
                    e.message?.contains("too-many-requests") == true ->
                        "Too many failed attempts. Please try again later."
                    else -> e.message ?: "Sign in failed. Please try again."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = TextInputEditText(this).apply {
            setText(binding.EtEmail.text.toString().trim())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "Email address"
        }
        val container = FrameLayout(this).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(emailInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email and we'll send you a reset link.")
            .setView(container)
            .setPositiveButton("Send Link") { _, _ ->
                val resetEmail = emailInput.text.toString().trim()
                if (resetEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                    auth.sendPasswordResetEmail(resetEmail)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reset link sent to $resetEmail", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, e.message ?: "Failed to send reset email", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
