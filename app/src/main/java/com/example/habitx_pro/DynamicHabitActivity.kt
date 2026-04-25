package com.example.habitx_pro

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DynamicHabitActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var habitId: String
    private lateinit var habitTitle: String
    private lateinit var habitType: String
    private lateinit var auth: FirebaseAuth

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    // UI Elements
    private lateinit var titleText: TextView
    private lateinit var tallyUi: LinearLayout
    private lateinit var timerUi: LinearLayout
    private lateinit var goalTimerUi: LinearLayout
    private lateinit var timeRangeUi: LinearLayout
    private lateinit var textUi: LinearLayout
    private lateinit var pedometerUi: LinearLayout
    private lateinit var historyList: ListView
    private lateinit var saveBtn: Button
    private lateinit var rangeDurationText: TextView

    // Tally State
    private var tallyCount = 0
    private lateinit var tallyCountText: TextView

    // Timer State
    private var timerSeconds = 0
    private var timerRunning = false
    private lateinit var timerText: TextView
    private lateinit var timerActionBtn: Button
    private val timerHandler = Handler(Looper.getMainLooper())

    // Goal Timer State
    private var goalRemainingSeconds = 0
    private var goalTotalSeconds = 0
    private var goalRunning = false
    private lateinit var goalTimerDisplay: TextView
    private val goalHandler = Handler(Looper.getMainLooper())

    // Time Range State
    private var selectedDate = ""
    private var startTime = ""
    private var endTime = ""

    // Pedometer State
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var initialSteps = -1
    private var currentSessionSteps = 0
    private lateinit var stepCountText: TextView

    // History
    private var dataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic_habit)
        auth = FirebaseAuth.getInstance()

        habitId = intent.getStringExtra("habit_id") ?: ""
        habitTitle = intent.getStringExtra("habit_title") ?: "Habit"
        
        val prefs = getSharedPreferences("HabitXPrefs_$userId", Context.MODE_PRIVATE)
        habitType = prefs.getString("habit_type_$habitId", "Tally") ?: "Tally"

        setupViews()
        loadHistory()
        setupTypeSpecificUi()

        if (habitType == "Pedometer") {
            checkStepPermission()
        }
    }

    private fun setupViews() {
        titleText = findViewById(R.id.habitTitleText)
        titleText.text = habitTitle
        
        rangeDurationText = findViewById(R.id.rangeDurationText)

        tallyUi = findViewById(R.id.tallyUi)
        timerUi = findViewById(R.id.timerUi)
        goalTimerUi = findViewById(R.id.goalTimerUi)
        timeRangeUi = findViewById(R.id.timeRangeUi)
        textUi = findViewById(R.id.textUi)
        pedometerUi = findViewById(R.id.pedometerUi)
        historyList = findViewById(R.id.dynamicHistoryList)
        saveBtn = findViewById(R.id.saveSessionBtn)

        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.WHITE) 
                return view
            }
        }
        historyList.adapter = adapter

        historyList.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteHistoryDialog(position)
            true
        }

        saveBtn.setOnClickListener { saveSession() }
    }

    private fun showDeleteHistoryDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Remove this session from history?")
            .setPositiveButton("Delete") { _, _ ->
                dataList.removeAt(position)
                adapter.notifyDataSetChanged()
                saveHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupTypeSpecificUi() {
        when (habitType) {
            "Tally" -> {
                tallyUi.visibility = View.VISIBLE
                tallyCountText = findViewById(R.id.tallyCountText)
                findViewById<Button>(R.id.incrementBtn).setOnClickListener {
                    tallyCount++
                    tallyCountText.text = tallyCount.toString()
                }
                findViewById<Button>(R.id.decrementBtn).setOnClickListener {
                    if (tallyCount > 0) tallyCount--
                    tallyCountText.text = tallyCount.toString()
                }
            }
            "Timer" -> {
                timerUi.visibility = View.VISIBLE
                timerText = findViewById(R.id.timerText)
                timerActionBtn = findViewById(R.id.timerActionBtn)
                timerActionBtn.setOnClickListener {
                    timerRunning = !timerRunning
                    timerActionBtn.text = if (timerRunning) "Pause Timer" else "Resume Timer"
                }
                startTimerLoop()
            }
            "GoalTimer" -> {
                goalTimerUi.visibility = View.VISIBLE
                saveBtn.visibility = View.GONE // Hidden initially for GoalTimer
                
                goalTimerDisplay = findViewById(R.id.goalTimerDisplay)
                val setGoalBtn = findViewById<Button>(R.id.setGoalBtn)
                val startBtn = findViewById<Button>(R.id.goalStartBtn)
                val pauseBtn = findViewById<Button>(R.id.goalPauseBtn)
                val resetBtn = findViewById<Button>(R.id.goalResetBtn)
                
                val hourPicker = findViewById<NumberPicker>(R.id.hourPicker)
                val minutePicker = findViewById<NumberPicker>(R.id.minutePicker)
                val secondPicker = findViewById<NumberPicker>(R.id.secondPicker)
                
                // Configure NumberPickers
                hourPicker.minValue = 0
                hourPicker.maxValue = 23
                minutePicker.minValue = 0
                minutePicker.maxValue = 59
                secondPicker.minValue = 0
                secondPicker.maxValue = 59
                
                setGoalBtn.setOnClickListener {
                    val h = hourPicker.value
                    val m = minutePicker.value
                    val s = secondPicker.value
                    
                    if (h == 0 && m == 0 && s == 0) {
                        Toast.makeText(this, "Set a time greater than 0", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    goalTotalSeconds = (h * 3600) + (m * 60) + s
                    goalRemainingSeconds = goalTotalSeconds
                    
                    goalTimerDisplay.text = String.format("%02d:%02d:%02d", h, m, s)
                    setGoalBtn.text = "Goal Confirmed: ${h}h ${m}m ${s}s"
                    saveBtn.visibility = View.GONE // Ensure it stays hidden when goal is reset/changed
                    Toast.makeText(this, "Goal Set!", Toast.LENGTH_SHORT).show()
                }

                startBtn.setOnClickListener {
                    if (!goalRunning) {
                        if (goalRemainingSeconds <= 0) {
                            Toast.makeText(this, "Please confirm a goal time first", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        goalRunning = true
                        startGoalLoop()
                    }
                }

                pauseBtn.setOnClickListener {
                    goalRunning = false
                }

                resetBtn.setOnClickListener {
                    goalRunning = false
                    saveBtn.visibility = View.GONE // Hide on reset
                    goalRemainingSeconds = goalTotalSeconds
                    val h = goalRemainingSeconds / 3600
                    val m = (goalRemainingSeconds % 3600) / 60
                    val s = goalRemainingSeconds % 60
                    goalTimerDisplay.text = String.format("%02d:%02d:%02d", h, m, s)
                }
            }
            "TimeRange" -> {
                timeRangeUi.visibility = View.VISIBLE

                val dateBtn = findViewById<Button>(R.id.startDateBtn)
                val startBtn = findViewById<Button>(R.id.startTimeBtn)
                val endBtn = findViewById<Button>(R.id.endTimeBtn)

                dateBtn.setOnClickListener {
                    val c = Calendar.getInstance()
                    DatePickerDialog(this, { _, y, m, d ->
                        selectedDate = String.format("%02d/%02d/%04d", d, m + 1, y)
                        dateBtn.text = "Date: $selectedDate"
                        updateLiveDuration()
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }

                startBtn.setOnClickListener {
                    TimePickerDialog(this, { _, h, m ->
                        startTime = String.format("%02d:%02d", h, m)
                        startBtn.text = "Start: $startTime"
                        updateLiveDuration()
                    }, 12, 0, true).show()
                }

                endBtn.setOnClickListener {
                    TimePickerDialog(this, { _, h, m ->
                        endTime = String.format("%02d:%02d", h, m)
                        endBtn.text = "End: $endTime"
                        updateLiveDuration()
                    }, 12, 0, true).show()
                }
            }
            "Text" -> {
                textUi.visibility = View.VISIBLE
            }
            "Pedometer" -> {
                pedometerUi.visibility = View.VISIBLE
                stepCountText = findViewById(R.id.stepCountText)
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                
                findViewById<Button>(R.id.resetPedometerBtn).setOnClickListener {
                    initialSteps = -1
                    currentSessionSteps = 0
                    stepCountText.text = "0"
                }
            }
        }
    }

    private fun updateLiveDuration() {
        val detail = calculateTimeRangeDetail()
        if (detail.isNotEmpty()) {
            val durationOnly = detail.substringAfter("for ")
            rangeDurationText.text = "Calculated Duration: $durationOnly"
        }
    }

    private fun checkStepPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 100)
            }
        }
    }

    private fun startTimerLoop() {
        timerHandler.post(object : Runnable {
            override fun run() {
                if (timerRunning) {
                    timerSeconds++
                    val h = timerSeconds / 3600
                    val m = (timerSeconds % 3600) / 60
                    val s = timerSeconds % 60
                    timerText.text = String.format("%02d:%02d:%02d", h, m, s)
                }
                timerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun startGoalLoop() {
        goalHandler.post(object : Runnable {
            override fun run() {
                if (goalRunning && goalRemainingSeconds > 0) {
                    goalRemainingSeconds--
                    val h = goalRemainingSeconds / 3600
                    val m = (goalRemainingSeconds % 3600) / 60
                    val s = goalRemainingSeconds % 60
                    goalTimerDisplay.text = String.format("%02d:%02d:%02d", h, m, s)
                    
                    if (goalRemainingSeconds == 0) {
                        goalRunning = false
                        saveBtn.visibility = View.VISIBLE // GOAL REACHED! Now show the save button
                        vibratePhone()
                        Toast.makeText(this@DynamicHabitActivity, "Goal Reached!", Toast.LENGTH_LONG).show()
                    } else {
                        goalHandler.postDelayed(this, 1000)
                    }
                }
            }
        })
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // Pattern: Start immediately, vibrate 1000ms, pause 500ms, repeat... total ~5 seconds
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialSteps == -1) {
                initialSteps = totalSteps
            }
            currentSessionSteps = totalSteps - initialSteps
            stepCountText.text = currentSessionSteps.toString()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveSession() {
        val now = Date()
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        
        val detail = when (habitType) {
            "Tally" -> "Count: $tallyCount"
            "Timer" -> "Duration: ${timerText.text}"
            "GoalTimer" -> {
                val completed = goalTotalSeconds - goalRemainingSeconds
                val h_comp = completed / 3600
                val m_comp = (completed % 3600) / 60
                val s_comp = completed % 60
                
                val h_goal = goalTotalSeconds / 3600
                val m_goal = (goalTotalSeconds % 3600) / 60
                val s_goal = goalTotalSeconds % 60
                
                "Goal: ${h_goal}h ${m_goal}m ${s_goal}s, Logged: ${String.format("%02d:%02d:%02d", h_comp, m_comp, s_comp)}"
            }
            "TimeRange" -> calculateTimeRangeDetail()
            "Text" -> findViewById<EditText>(R.id.textDetailsEdit).text.toString()
            "Pedometer" -> "Steps: $currentSessionSteps"
            else -> ""
        }

        if (detail.isEmpty() || detail.contains("null") || (habitType == "TimeRange" && (startTime.isEmpty() || endTime.isEmpty()))) {
            Toast.makeText(this, "Please complete the entry", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = if (habitType == "TimeRange") {
            detail
        } else {
            "on $dateStr at $timeStr - $detail"
        }
        
        dataList.add(0, entry)
        adapter.notifyDataSetChanged()
        
        saveHistory()
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        
        resetAfterSave()
    }

    private fun resetAfterSave() {
        when (habitType) {
            "Tally" -> {
                tallyCount = 0
                tallyCountText.text = "0"
            }
            "Pedometer" -> {
                initialSteps = -1
                currentSessionSteps = 0
                stepCountText.text = "0"
            }
            "TimeRange" -> {
                rangeDurationText.text = ""
            }
            "Text" -> {
                findViewById<EditText>(R.id.textDetailsEdit).setText("")
            }
            "GoalTimer" -> {
                goalRunning = false
                saveBtn.visibility = View.GONE // Hide again after successful save
                goalRemainingSeconds = goalTotalSeconds
                val h = goalRemainingSeconds / 3600
                val m = (goalRemainingSeconds % 3600) / 60
                val s = goalRemainingSeconds % 60
                goalTimerDisplay.text = String.format("%02d:%02d:%02d", h, m, s)
            }
        }
    }

    private fun calculateTimeRangeDetail(): String {
        if (startTime.isEmpty() || endTime.isEmpty()) return ""
        val displayDate = if (selectedDate.isEmpty()) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) else selectedDate
        
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date1 = sdf.parse(startTime)
            val date2 = sdf.parse(endTime)
            
            var diff = date2!!.time - date1!!.time
            if (diff < 0) {
                diff += TimeUnit.DAYS.toMillis(1)
            }
            
            val h = TimeUnit.MILLISECONDS.toHours(diff)
            val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            
            "on $displayDate for $h h $m min"
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveHistory() {
        val prefs = getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("data_$habitId", dataList.joinToString("|")).apply()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE)
        val saved = prefs.getString("data_$habitId", "")
        if (!saved.isNullOrEmpty()) {
            dataList.clear()
            dataList.addAll(saved.split("|"))
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        if (habitType == "Pedometer") {
            stepCounterSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (habitType == "Pedometer") {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null)
        goalHandler.removeCallbacksAndMessages(null)
    }
}
