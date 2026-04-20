package com.example.habitx_pro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        sharedPref = getSharedPreferences("HabitX", MODE_PRIVATE)

        val profileName = findViewById<EditText>(R.id.profileName)
        val profileAge = findViewById<EditText>(R.id.profileAge)
        val saveProfileBtn = findViewById<Button>(R.id.saveProfileBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        // Load name from shared preferences
        val name = sharedPref.getString("name", "")
        profileName.setText(name)

        // Load saved age if exists
        val age = sharedPref.getString("age", "")
        profileAge.setText(age)

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
            // 🔥 Actual Firebase Sign Out
            auth.signOut()
            
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}