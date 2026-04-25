package com.example.habitx_pro

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)
        auth = FirebaseAuth.getInstance()

        val habitNameEdit = findViewById<TextInputEditText>(R.id.habitNameEdit)
        val typeRadioGroup = findViewById<RadioGroup>(R.id.typeRadioGroup)
        val saveBtn = findViewById<Button>(R.id.saveHabitBtn)

        saveBtn.setOnClickListener {
            val name = habitNameEdit.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedTypeId = typeRadioGroup.checkedRadioButtonId
            val type = when (selectedTypeId) {
                R.id.radioTally -> "Tally"
                R.id.radioTimer -> "Timer"
                R.id.radioGoalTimer -> "GoalTimer"
                R.id.radioTimeRange -> "TimeRange"
                R.id.radioText -> "Text"
                R.id.radioPedometer -> "Pedometer"
                else -> "Tally"
            }

            saveNewHabit(name, type)
            finish()
        }
    }

    private fun saveNewHabit(name: String, type: String) {
        val prefs = getSharedPreferences("HabitXPrefs_$userId", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("custom_habits", null)
        
        val habitType = object : TypeToken<MutableList<Habit>>() {}.type
        val habits: MutableList<Habit> = if (json == null) {
            mutableListOf()
        } else {
            gson.fromJson(json, habitType)
        }

        val id = UUID.randomUUID().toString()
        val newHabit = Habit(id, name, "Track $type", "custom_habit_icon", "DynamicHabitActivity")
        
        prefs.edit().putString("habit_type_$id", type).apply()
        
        habits.add(newHabit)
        prefs.edit().putString("custom_habits", gson.toJson(habits)).apply()
        
        Toast.makeText(this, "Habit '$name' created!", Toast.LENGTH_SHORT).show()
    }
}
