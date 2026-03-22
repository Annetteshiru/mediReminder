package com.example.medireminder

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.medireminder.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnSignUp.setOnClickListener {
            if (validateForm()) {
                binding.btnSignUp.isEnabled = false
                binding.btnSignUp.text = "Creating account…"
                createAccount()
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                val isNewUser = result.additionalUserInfo?.isNewUser == true
                if (isNewUser) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .set(mapOf(
                            "userId"    to user.uid,
                            "email"     to (user.email ?: ""),
                            "name"      to (user.displayName ?: ""),
                            "photoUrl"  to (user.photoUrl?.toString() ?: ""),
                            "createdAt" to System.currentTimeMillis()
                        ))
                }
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
