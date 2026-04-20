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

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        sharedPref = getSharedPreferences("HabitX", MODE_PRIVATE)

        val profileName = findViewById<TextInputEditText>(R.id.profileName)
        val profileAge = findViewById<TextInputEditText>(R.id.profileAge)
        val userEmail = findViewById<TextView>(R.id.userEmail)
        val saveProfileBtn = findViewById<Button>(R.id.saveProfileBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        // Display current user email
        val currentUser = auth.currentUser
        userEmail.text = currentUser?.email ?: "Not logged in"

        // Load name from shared preferences
        val savedName = sharedPref.getString("name", "")
        profileName.setText(savedName)

        // Load saved age if exists
        val savedAge = sharedPref.getString("age", "")
        profileAge.setText(savedAge)

        saveProfileBtn.setOnClickListener {
            val enteredName = profileName.text.toString()
            val enteredAge = profileAge.text.toString()
            
            if (enteredName.isNotEmpty() && enteredAge.isNotEmpty()) {
                sharedPref.edit()
                    .putString("name", enteredName)
                    .putString("age", enteredAge)
                    .apply()
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
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