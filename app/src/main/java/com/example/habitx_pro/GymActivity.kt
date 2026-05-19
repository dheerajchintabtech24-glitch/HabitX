package com.example.habitx_pro

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class GymActivity : AppCompatActivity() {

    private lateinit var exerciseContainer: LinearLayout
    private lateinit var historyListView: ListView
    private lateinit var auth: FirebaseAuth
    
    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    private var dataList = mutableListOf<String>()
    private lateinit var historyAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gym)
        auth = FirebaseAuth.getInstance()

        exerciseContainer = findViewById(R.id.exerciseContainer)
        historyListView = findViewById(R.id.gymHistoryList)
        val addExerciseBtn = findViewById<Button>(R.id.addExerciseBtn)
        val saveBtn = findViewById<Button>(R.id.saveGymBtn)

        loadHistory()

        historyAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.WHITE)
                return view
            }
        }
        historyListView.adapter = historyAdapter

        addExerciseBtn.setOnClickListener { addExerciseRow() }
        saveBtn.setOnClickListener { saveWorkout() }

        // Add first row by default
        addExerciseRow()

        historyListView.setOnItemLongClickListener { _, _, position, _ ->
            dataList.removeAt(position)
            saveHistory()
            historyAdapter.notifyDataSetChanged()
            true
        }
    }

    private fun addExerciseRow() {
        val row = LayoutInflater.from(this).inflate(R.layout.item_gym_exercise, exerciseContainer, false)
        val nameEdit = row.findViewById<EditText>(R.id.exerciseNameEdit)
        val countText = row.findViewById<TextView>(R.id.repCountText)
        val plusBtn = row.findViewById<Button>(R.id.repPlusBtn)
        val minusBtn = row.findViewById<Button>(R.id.repMinusBtn)
        val removeBtn = row.findViewById<ImageButton>(R.id.removeExerciseBtn)

        var count = 0
        plusBtn.setOnClickListener {
            count++
            countText.text = count.toString()
        }
        minusBtn.setOnClickListener {
            if (count > 0) count--
            countText.text = count.toString()
        }
        removeBtn.setOnClickListener {
            exerciseContainer.removeView(row)
        }

        exerciseContainer.addView(row)
    }

    private fun saveWorkout() {
        val workoutDetails = StringBuilder()
        var hasData = false

        for (i in 0 until exerciseContainer.childCount) {
            val row = exerciseContainer.getChildAt(i)
            val name = row.findViewById<EditText>(R.id.exerciseNameEdit).text.toString()
            val reps = row.findViewById<TextView>(R.id.repCountText).text.toString()

            if (name.isNotBlank()) {
                if (hasData) workoutDetails.append(", ")
                workoutDetails.append("$name: $reps reps")
                hasData = true
            }
        }

        if (!hasData) {
            Toast.makeText(this, "Please enter at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm", Locale.getDefault()).format(Date())
        val entry = "on $date - $workoutDetails"

        dataList.add(0, entry)
        saveHistory()
        historyAdapter.notifyDataSetChanged()
        
        Toast.makeText(this, "Workout Saved!", Toast.LENGTH_SHORT).show()
        
        // Clear for next session
        exerciseContainer.removeAllViews()
        addExerciseRow()
    }

    private fun saveHistory() {
        val prefs = getSharedPreferences("GymData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("data", dataList.joinToString("|")).apply()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("GymData_$userId", Context.MODE_PRIVATE)
        val saved = prefs.getString("data", "")
        if (!saved.isNullOrEmpty()) {
            dataList.clear()
            dataList.addAll(saved.split("|"))
        }
    }
}
