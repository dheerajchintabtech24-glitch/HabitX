package com.example.habitx_pro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_home)

            setupRecyclerView()

            // Add Habit Button
            findViewById<Button>(R.id.addHabitBtn).setOnClickListener {
                Toast.makeText(this, "Under Development", Toast.LENGTH_SHORT).show()
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

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.habitsRecyclerView)
        
        // Use GridLayout with 2 columns
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Initial Habit List (Can be moved to Firebase later)
        val habitList = listOf(
            Habit("1", "Walking", "Track steps", "walk_card", "WalkingActivity"),
            Habit("2", "Meditation", "Relax mind", "meditation_card", "MeditationActivity"),
            Habit("3", "Cooking", "Track meals", "cooking_card", "CookingActivity"),
            Habit("4", "Sleep", "Track sleep", "sleep_card", "SleepActivity"),
            Habit("5", "Yoga", "Track time", "yoga_card", "YogaActivity"),
            Habit("6", "Swimming", "Track laps", "swim_card", "SwimmingActivity")
        )

        val adapter = HabitAdapter(habitList) { habit ->
            openHabitActivity(habit.activityClassName)
        }
        recyclerView.adapter = adapter
    }

    private fun openHabitActivity(className: String) {
        try {
            val packageName = packageName
            val intent = Intent(this, Class.forName("$packageName.$className"))
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_home
    }
}
