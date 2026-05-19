package com.example.habitx_pro

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ProgressActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val gson = Gson()
    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var currentMonthText: TextView
    private lateinit var selectedDateText: TextView
    private lateinit var dailyProgressDetails: TextView
    private lateinit var prevMonthBtn: ImageButton
    private lateinit var nextMonthBtn: ImageButton

    private val progressMap = mutableMapOf<String, MutableList<String>>()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        auth = FirebaseAuth.getInstance()

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        currentMonthText = findViewById(R.id.currentMonthText)
        selectedDateText = findViewById(R.id.selectedDateText)
        dailyProgressDetails = findViewById(R.id.dailyProgressDetails)
        prevMonthBtn = findViewById(R.id.prevMonthBtn)
        nextMonthBtn = findViewById(R.id.nextMonthBtn)

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }

        fetchAllProgress()
        setupCalendar()

        prevMonthBtn.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            setupCalendar()
        }

        nextMonthBtn.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            setupCalendar()
        }
    }

    private fun fetchAllProgress() {
        progressMap.clear()

        fun addEntry(fullText: String?, activityName: String) {
            if (fullText == null) return
            
            // Try to extract date from strings like "14/10/2023", "14 Oct 2023", etc.
            val datePatterns = listOf(
                """\d{1,2}/\d{1,2}/\d{4}""".toRegex(),
                """\d{1,2} [A-Z][a-z]{2} \d{4}""".toRegex()
            )

            var cleanDate: String? = null
            
            for (pattern in datePatterns) {
                val match = pattern.find(fullText)
                if (match != null) {
                    val rawDate = match.value
                    try {
                        // Normalize to dd/MM/yyyy
                        val parsedDate = if (rawDate.contains("/")) {
                            SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(rawDate)
                        } else {
                            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).parse(rawDate)
                        }
                        if (parsedDate != null) {
                            cleanDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsedDate)
                            break
                        }
                    } catch (e: Exception) {}
                }
            }

            if (cleanDate != null) {
                progressMap.getOrPut(cleanDate) { mutableListOf() }.add("$activityName: $fullText")
            }
        }

        // 1. Walking
        val walkingPrefs = getSharedPreferences("WalkingData_$userId", Context.MODE_PRIVATE)
        walkingPrefs.getString("data", "")?.split("|")?.forEach { addEntry(it, "Walking") }

        // 2. Yoga
        val yogaPrefs = getSharedPreferences("YogaData_$userId", Context.MODE_PRIVATE)
        yogaPrefs.getString("data", "")?.split("|")?.forEach { addEntry(it, "Yoga") }

        // 3. Sleep
        val sleepPrefs = getSharedPreferences("SleepData_$userId", Context.MODE_PRIVATE)
        sleepPrefs.getString("history", "")?.split("|")?.forEach { addEntry(it, "Sleep") }

        // 4. Swim
        val swimPrefs = getSharedPreferences("SwimData_$userId", Context.MODE_PRIVATE)
        swimPrefs.getString("data", "")?.split("|")?.forEach { addEntry(it, "Swimming") }

        // 5. Meditation
        val medPrefs = getSharedPreferences("MeditationData_$userId", Context.MODE_PRIVATE)
        medPrefs.getString("data", "")?.split("|")?.forEach { addEntry(it, "Meditation") }

        // 6. Gym
        val gymPrefs = getSharedPreferences("GymData_$userId", Context.MODE_PRIVATE)
        gymPrefs.getString("data", "")?.split("|")?.forEach { addEntry(it, "Gym") }

        // 7. Cooking
        val cookingPrefs = getSharedPreferences("CookingData_$userId", Context.MODE_PRIVATE)
        cookingPrefs.getString("data", "")?.split("|")?.forEach { addEntry(it, "Cooking") }

        // 8. Singing
        val singingPrefs = getSharedPreferences("SingingData_$userId", Context.MODE_PRIVATE)
        singingPrefs.getString("last_recorded", null)?.let { addEntry(it, "Singing") }

        // 9. Custom Habits
        val habitXPrefs = getSharedPreferences("HabitXPrefs_$userId", Context.MODE_PRIVATE)
        val dataPrefs = getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE)
        val json = habitXPrefs.getString("custom_habits", null)
        if (json != null) {
            val typeToken = object : TypeToken<List<Habit>>() {}.type
            val habits: List<Habit> = gson.fromJson(json, typeToken)
            for (habit in habits) {
                val savedDataJson = dataPrefs.getString("data_${habit.id}", "")
                if (!savedDataJson.isNullOrEmpty()) {
                    val historyList: List<String> = try {
                        val listType = object : TypeToken<List<String>>() {}.type
                        gson.fromJson(savedDataJson, listType)
                    } catch (e: Exception) {
                        savedDataJson.split("|").filter { it.isNotEmpty() }
                    }
                    historyList.forEach { entry ->
                        addEntry(entry, habit.title)
                    }
                }
            }
        }
    }

    private fun setupCalendar() {
        val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        currentMonthText.text = monthYearFormatter.format(calendar.time)

        val daysInMonth = getDaysInMonthList()
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = CalendarAdapter(daysInMonth)
    }

    private fun getDaysInMonthList(): List<String> {
        val daysList = mutableListOf<String>()
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val dayOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        for (i in 0 until dayOffset) {
            daysList.add("")
        }

        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDays) {
            daysList.add(i.toString())
        }
        return daysList
    }

    inner class CalendarAdapter(private val days: List<String>) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dayText: TextView = view.findViewById(R.id.dayText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val day = days[position]
            holder.dayText.text = day

            if (day.isEmpty()) {
                holder.dayText.background = null
                holder.itemView.setOnClickListener(null)
                return
            }

            val dateStr = String.format("%02d/%02d/%d", day.toInt(), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
            val hasProgress = progressMap.containsKey(dateStr)

            if (hasProgress) {
                holder.dayText.setBackgroundResource(R.drawable.bg_calendar_green)
            } else {
                holder.dayText.setBackgroundResource(R.drawable.bg_calendar_red)
            }
            holder.dayText.setTextColor(Color.WHITE)

            holder.itemView.setOnClickListener {
                selectedDateText.text = "Progress for $dateStr"
                val progress = progressMap[dateStr]
                if (progress.isNullOrEmpty()) {
                    dailyProgressDetails.text = "No progress done for this day."
                    dailyProgressDetails.setTextColor(Color.parseColor("#FF6666"))
                } else {
                    val details = progress.joinToString("\n\n") { "• $it" }
                    dailyProgressDetails.text = details
                    dailyProgressDetails.setTextColor(Color.WHITE)
                }
            }
        }

        override fun getItemCount() = days.size
    }
}
