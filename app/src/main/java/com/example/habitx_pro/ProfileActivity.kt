package com.example.habitx_pro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid

        // Display current user email
        userEmail.text = currentUser?.email ?: "Not logged in"

        // Fetch user name from Firebase Realtime Database
        if (currentUserId != null) {
            val userRef = database.getReference("users").child(currentUserId).child("name")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.getValue(String::class.java)
                    if (name != null) {
                        profileName.setText(name)
                        // Also sync to local shared preferences for offline use
                        sharedPref.edit().putString("name", name).apply()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Fallback to local shared preferences if database fetch fails
                    val savedName = sharedPref.getString("name", "")
                    profileName.setText(savedName)
                }
            })
        } else {
            val savedName = sharedPref.getString("name", "")
            profileName.setText(savedName)
        }

        saveProfileBtn.setOnClickListener {
            val enteredName = profileName.text.toString().trim()
            
            if (enteredName.isNotEmpty()) {
                // Update in Firebase
                if (currentUserId != null) {
                    database.getReference("users").child(currentUserId).child("name").setValue(enteredName)
                }
                
                // Update locally
                sharedPref.edit()
                    .putString("name", enteredName)
                    .apply()
                
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
}
