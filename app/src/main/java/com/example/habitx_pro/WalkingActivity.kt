package com.example.habitx_pro

import android.content.Context
import android.graphics.Color
import android.hardware.*
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class WalkingActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private lateinit var stepsText: TextView
    private lateinit var goalText: TextView
    private lateinit var stepProgressBar: ProgressBar
    private lateinit var saveBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var listView: ListView

    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var stepGoal = 10000

    private var dataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walking)

        stepsText = findViewById(R.id.stepsText)
        goalText = findViewById(R.id.goalText)
        stepProgressBar = findViewById(R.id.stepProgressBar)
        saveBtn = findViewById(R.id.saveBtn)
        resetBtn = findViewById(R.id.resetBtn)
        listView = findViewById(R.id.historyList)

        loadData()

        // Styled adapter for history list
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.WHITE)
                return view
            }
        }
        listView.adapter = adapter

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "No Step Counter Sensor found on this device", Toast.LENGTH_LONG).show()
        }

        // ✅ EDIT GOAL
        goalText.setOnClickListener {
            showEditGoalDialog()
        }

        // ✅ SAVE SESSION
        saveBtn.setOnClickListener {
            val currentSteps = (totalSteps - previousTotalSteps).toInt()

            if (currentSteps == 0) {
                Toast.makeText(this, "No steps to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            val entry = "$date - $currentSteps steps"

            dataList.add(0, entry)
            saveDataList()
            adapter.notifyDataSetChanged()

            Toast.makeText(this, "Session Saved!", Toast.LENGTH_SHORT).show()
        }

        // ✅ RESET
        resetBtn.setOnClickListener {
            previousTotalSteps = totalSteps
            saveBase()
            updateUI(0)
            Toast.makeText(this, "Steps Reset", Toast.LENGTH_SHORT).show()
        }

        // ✅ DELETE (LONG PRESS)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            dataList.removeAt(position)
            saveDataList()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showEditGoalDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Step Goal")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(stepGoal.toString())
        builder.setView(input)

        builder.setPositiveButton("Set") { _, _ ->
            val newGoal = input.text.toString().toIntOrNull()
            if (newGoal != null && newGoal > 0) {
                stepGoal = newGoal
                saveGoal()
                updateUI((totalSteps - previousTotalSteps).toInt())
                Toast.makeText(this, "Goal updated to $stepGoal", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid goal", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun updateUI(steps: Int) {
        stepsText.text = "$steps"
        goalText.text = "Goal: $stepGoal"
        stepProgressBar.max = stepGoal
        stepProgressBar.progress = steps
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        // Initial UI update
        updateUI((totalSteps - previousTotalSteps).toInt())
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            totalSteps = event.values[0]
            val currentSteps = (totalSteps - previousTotalSteps).toInt()
            updateUI(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveBase() {
        val prefs = getSharedPreferences("stepsPrefs", MODE_PRIVATE)
        prefs.edit().putFloat("previousSteps", previousTotalSteps).apply()
    }

    private fun saveGoal() {
        val prefs = getSharedPreferences("stepsPrefs", MODE_PRIVATE)
        prefs.edit().putInt("stepGoal", stepGoal).apply()
    }

    private fun saveDataList() {
        val prefs = getSharedPreferences("WalkingData", MODE_PRIVATE)
        prefs.edit().putString("data", dataList.joinToString("|")).apply()
    }

    private fun loadData() {
        // Load history
        val prefs = getSharedPreferences("WalkingData", MODE_PRIVATE)
        val saved = prefs.getString("data", "")
        if (!saved.isNullOrEmpty()) {
            dataList.clear()
            dataList.addAll(saved.split("|"))
        }

        // Load step baseline and goal
        val basePrefs = getSharedPreferences("stepsPrefs", MODE_PRIVATE)
        previousTotalSteps = basePrefs.getFloat("previousSteps", 0f)
        stepGoal = basePrefs.getInt("stepGoal", 10000)
    }
}
