package com.example.habitx_pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_home)

            // Setup Card Click Listeners
            val walking = findViewById<View>(R.id.walkingCard)
            walking?.setOnClickListener {
                startActivity(Intent(this, WalkingActivity::class.java))
            }

            val meditation = findViewById<View>(R.id.meditationCard)
            meditation?.setOnClickListener {
                startActivity(Intent(this, MeditationActivity::class.java))
            }

            val cooking = findViewById<View>(R.id.cookingCard)
            cooking?.setOnClickListener {
                startActivity(Intent(this, CookingActivity::class.java))
            }

            val sleep = findViewById<View>(R.id.sleepCard)
            sleep?.setOnClickListener {
                startActivity(Intent(this, SleepActivity::class.java))
            }

            val yoga = findViewById<View>(R.id.yogaCard)
            yoga?.setOnClickListener {
                startActivity(Intent(this, YogaActivity::class.java))
            }

            val swimming = findViewById<View>(R.id.swimmingCard)
            swimming?.setOnClickListener {
                startActivity(Intent(this, SwimmingActivity::class.java))
            }

            // Add Habit Button - Exact message requested
            findViewById<Button>(R.id.addHabitBtn).setOnClickListener {
                Toast.makeText(this, "Under Devlopment", Toast.LENGTH_SHORT).show()
            }

            // Bottom Navigation Logic
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_home

            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> true
                    R.id.nav_progress -> {
                        startActivity(Intent(this, ProgressActivity::class.java))
                        true
                    }
                    R.id.nav_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    }
                    else -> false
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_home
    }
}
