package com.example.habitx_pro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProgressActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        auth = FirebaseAuth.getInstance()

        updateWalkingSummary()
        updateYogaSummary()
        updateSleepSummary()
        updateSwimmingSummary()
        updateMeditationSummary()
        updateCookingSummary()
        updateDynamicHabitsSummary()
    }

    private fun updateWalkingSummary() {
        val summaryText = findViewById<TextView>(R.id.walkingSummary)
        val prefs = getSharedPreferences("WalkingData_$userId", Context.MODE_PRIVATE)
        val data = prefs.getString("data", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|").firstOrNull()
            summaryText.text = "Latest: $lastEntry"
        } else {
            summaryText.text = "No steps saved yet"
        }
    }

    private fun updateYogaSummary() {
        val summaryText = findViewById<TextView>(R.id.yogaSummary)
        val prefs = getSharedPreferences("YogaData_$userId", Context.MODE_PRIVATE)
        val data = prefs.getString("data", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|").firstOrNull()
            summaryText.text = "Latest: $lastEntry"
        } else {
            summaryText.text = "No yoga sessions yet"
        }
    }

    private fun updateSleepSummary() {
        val summaryText = findViewById<TextView>(R.id.sleepSummary)
        val prefs = getSharedPreferences("SleepData_$userId", Context.MODE_PRIVATE)
        val data = prefs.getStringSet("data", null)
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.firstOrNull()
            summaryText.text = "Latest: $lastEntry"
        } else {
            summaryText.text = "No sleep records yet"
        }
    }

    private fun updateSwimmingSummary() {
        val summaryText = findViewById<TextView>(R.id.swimmingSummary)
        val prefs = getSharedPreferences("SwimData_$userId", Context.MODE_PRIVATE)
        val data = prefs.getString("data", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|").firstOrNull()
            summaryText.text = "Latest: $lastEntry"
        } else {
            summaryText.text = "No swimming laps yet"
        }
    }

    private fun updateMeditationSummary() {
        val summaryText = findViewById<TextView>(R.id.meditationSummary)
        val prefs = getSharedPreferences("MeditationData_$userId", Context.MODE_PRIVATE)
        val data = prefs.getString("data", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|").firstOrNull()
            summaryText.text = "Latest: $lastEntry"
        } else {
            summaryText.text = "No meditation sessions yet"
        }
    }

    private fun updateCookingSummary() {
        val summaryText = findViewById<TextView>(R.id.cookingSummary)
        val prefs = getSharedPreferences("CookingData_$userId", Context.MODE_PRIVATE)
        val data = prefs.getString("recipes", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|||").firstOrNull()?.split("\n")?.firstOrNull()
            summaryText.text = "Latest Dish: $lastEntry"
        } else {
            summaryText.text = "No recipes saved yet"
        }
    }

    private fun updateDynamicHabitsSummary() {
        val container = findViewById<LinearLayout>(R.id.dynamicHabitsProgressContainer)
        container.removeAllViews()

        val prefs = getSharedPreferences("HabitXPrefs_$userId", Context.MODE_PRIVATE)
        val dataPrefs = getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE)
        val json = prefs.getString("custom_habits", null) ?: return
        
        val type = object : TypeToken<List<Habit>>() {}.type
        val habits: List<Habit> = Gson().fromJson(json, type)

        val inflater = LayoutInflater.from(this)

        for (habit in habits) {
            val cardView = inflater.inflate(R.layout.item_progress_card, container, false)
            val title = cardView.findViewById<TextView>(R.id.habitProgressTitle)
            val summary = cardView.findViewById<TextView>(R.id.habitProgressSummary)

            title.text = habit.title
            
            val savedData = dataPrefs.getString("data_${habit.id}", "")
            if (!savedData.isNullOrEmpty()) {
                val lastEntry = savedData.split("|").firstOrNull()
                summary.text = "Latest: $lastEntry"
            } else {
                summary.text = "No records yet"
            }

            container.addView(cardView)
        }
    }
}
