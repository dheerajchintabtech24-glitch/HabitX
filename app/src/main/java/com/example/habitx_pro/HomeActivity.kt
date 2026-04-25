package com.example.habitx_pro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var habitList = mutableListOf<Habit>()
    private lateinit var adapter: HabitAdapter
    private lateinit var auth: FirebaseAuth

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        try {
            setContentView(R.layout.activity_home)

            recyclerView = findViewById(R.id.habitsRecyclerView)
            val layoutManager = GridLayoutManager(this, 2)
            
            // Set SpanSizeLookup for the header
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter.getItemViewType(position) == HabitAdapter.VIEW_TYPE_HEADER) 2 else 1
                }
            }
            
            recyclerView.layoutManager = layoutManager

            loadHabits()

            // Add Habit Button
            findViewById<Button>(R.id.addHabitBtn).setOnClickListener {
                startActivity(Intent(this, CreateHabitActivity::class.java))
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

    private fun loadHabits() {
        val prefs = getSharedPreferences("HabitXPrefs_$userId", Context.MODE_PRIVATE)
        val json = prefs.getString("custom_habits", null)
        
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        val savedHabits: MutableList<Habit>? = Gson().fromJson(json, type)

        habitList = mutableListOf(
            Habit("1", "Walking", "Track steps", "walk_card", "WalkingActivity"),
            Habit("2", "Meditation", "Relax mind", "meditation_card", "MeditationActivity"),
            Habit("3", "Cooking", "Track meals", "cooking_card", "CookingActivity"),
            Habit("4", "Sleep", "Track sleep", "sleep_card", "SleepActivity"),
            Habit("5", "Yoga", "Track time", "yoga_card", "YogaActivity"),
            Habit("6", "Swimming", "Track laps", "swim_card", "SwimmingActivity")
        )

        if (savedHabits != null && savedHabits.isNotEmpty()) {
            habitList.add(Habit("HEADER_ADDED", "Added Habits", "", "", ""))
            habitList.addAll(savedHabits)
        }

        adapter = HabitAdapter(habitList, { habit ->
            if (habit.id != "HEADER_ADDED") openHabitActivity(habit)
        }, { habit ->
            if (habit.id != "HEADER_ADDED") showDeleteConfirmation(habit)
        })
        recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmation(habit: Habit) {
        // Only allow deleting custom habits
        if (habit.id.length < 5 || habit.id == "HEADER_ADDED") {
            Toast.makeText(this, "System habits cannot be deleted", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '${habit.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteHabit(habit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHabit(habit: Habit) {
        val prefs = getSharedPreferences("HabitXPrefs_$userId", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("custom_habits", null)
        
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        val habits: MutableList<Habit> = gson.fromJson(json, type) ?: mutableListOf()

        habits.removeAll { it.id == habit.id }
        prefs.edit().putString("custom_habits", gson.toJson(habits)).apply()
        
        // Also clean up habit type and data
        prefs.edit().remove("habit_type_${habit.id}").apply()
        getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE).edit().remove("data_${habit.id}").apply()

        loadHabits()
        Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show()
    }

    private fun openHabitActivity(habit: Habit) {
        try {
            val intent = if (habit.activityClassName == "DynamicHabitActivity") {
                Intent(this, DynamicHabitActivity::class.java).apply {
                    putExtra("habit_id", habit.id)
                    putExtra("habit_title", habit.title)
                }
            } else {
                val packageName = packageName
                Intent(this, Class.forName("$packageName.${habit.activityClassName}"))
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening activity", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHabits()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_home
    }
}
