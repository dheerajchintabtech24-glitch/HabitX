package com.example.habitx_pro

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        sharedPref = getSharedPreferences("HabitX_$userId", MODE_PRIVATE)

        val profileName = findViewById<TextInputEditText>(R.id.profileName)
        val userEmail = findViewById<TextView>(R.id.userEmail)
        val saveProfileBtn = findViewById<Button>(R.id.saveProfileBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        val profileImage = findViewById<ShapeableImageView>(R.id.profileImage)

        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid

        // Display current user email
        userEmail.text = currentUser?.email ?: "Not logged in"

        // Setup image picker
        val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    profileImage.setImageURI(it)
                    
                    // Save locally
                    sharedPref.edit().putString("profile_image_uri", it.toString()).apply()
                    
                    // Save to Firebase
                    if (currentUserId != null) {
                        database.getReference("users").child(currentUserId).child("profileImageUri").setValue(it.toString())
                    }
                } catch (e: Exception) {
                    profileImage.setImageURI(it)
                }
            }
        }

        profileImage.setOnClickListener {
            val options = arrayOf("Default Image", "Add image from device")
            AlertDialog.Builder(this)
                .setTitle("Profile Photo")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Default Image
                            profileImage.setImageResource(R.drawable.profile_image)
                            sharedPref.edit().remove("profile_image_uri").apply()
                            if (currentUserId != null) {
                                database.getReference("users").child(currentUserId).child("profileImageUri").removeValue()
                            }
                            Toast.makeText(this, "Default image restored", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            // Add from device
                            pickImage.launch(arrayOf("image/*"))
                        }
                    }
                }
                .show()
        }

        // Fetch user info from Firebase Realtime Database
        if (currentUserId != null) {
            val userRef = database.getReference("users").child(currentUserId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    if (name != null) {
                        profileName.setText(name)
                        sharedPref.edit().putString("name", name).apply()
                    }
                    
                    val imageUriStr = snapshot.child("profileImageUri").getValue(String::class.java)
                    if (imageUriStr != null) {
                        try {
                            profileImage.setImageURI(Uri.parse(imageUriStr))
                            sharedPref.edit().putString("profile_image_uri", imageUriStr).apply()
                        } catch (e: Exception) {
                            profileImage.setImageResource(R.drawable.profile_image)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.profile_image)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    loadLocalData(profileName, profileImage)
                }
            })
        } else {
            loadLocalData(profileName, profileImage)
        }

        saveProfileBtn.setOnClickListener {
            val enteredName = profileName.text.toString().trim()
            
            if (enteredName.isNotEmpty()) {
                if (currentUserId != null) {
                    database.getReference("users").child(currentUserId).child("name").setValue(enteredName)
                }
                sharedPref.edit().putString("name", enteredName).apply()
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadLocalData(nameInput: TextInputEditText, imageView: ShapeableImageView) {
        val savedName = sharedPref.getString("name", "")
        nameInput.setText(savedName)
        
        val savedUri = sharedPref.getString("profile_image_uri", null)
        if (savedUri != null) {
            try {
                imageView.setImageURI(Uri.parse(savedUri))
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.profile_image)
            }
        } else {
            imageView.setImageResource(R.drawable.profile_image)
        }
    }
}
