package com.example.medireminder

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.medireminder.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUserInfo()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun setupUserInfo() {
        val user = auth.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: "User"
        val email = user?.email ?: ""

        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        binding.tvProfileInitial.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

        // Load Google profile photo if available
        val photoUrl = user?.photoUrl?.toString()
        if (!photoUrl.isNullOrEmpty()) {
            binding.ivProfilePhoto.visibility = View.VISIBLE
            binding.tvProfileInitial.visibility = View.GONE
            Glide.with(this)
                .load(photoUrl)
                .transform(CircleCrop())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProfilePhoto)
        }
    }
}
