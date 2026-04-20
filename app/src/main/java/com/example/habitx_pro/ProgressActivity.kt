package com.example.habitx_pro

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProgressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        updateWalkingSummary()
        updateYogaSummary()
        updateMeditationSummary()
        updateCookingSummary()
    }

    private fun updateWalkingSummary() {
        val summaryText = findViewById<TextView>(R.id.walkingSummary)
        val prefs = getSharedPreferences("WalkingData", Context.MODE_PRIVATE)
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
        val prefs = getSharedPreferences("YogaData", Context.MODE_PRIVATE)
        val data = prefs.getString("data", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|").firstOrNull()
            summaryText.text = "Latest: $lastEntry"
        } else {
            summaryText.text = "No yoga sessions yet"
        }
    }

    private fun updateMeditationSummary() {
        val summaryText = findViewById<TextView>(R.id.meditationSummary)
        val prefs = getSharedPreferences("MeditationData", Context.MODE_PRIVATE)
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
        val prefs = getSharedPreferences("CookingData", Context.MODE_PRIVATE)
        val data = prefs.getString("recipes", "")
        
        if (!data.isNullOrEmpty()) {
            val lastEntry = data.split("|||").firstOrNull()?.split("\n")?.firstOrNull()
            summaryText.text = "Latest Dish: $lastEntry"
        } else {
            summaryText.text = "No recipes saved yet"
        }
    }
}
