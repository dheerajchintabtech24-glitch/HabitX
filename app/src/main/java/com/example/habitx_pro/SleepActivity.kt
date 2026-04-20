package com.example.habitx_pro

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SleepActivity : AppCompatActivity() {

    private var sleepHour = 22
    private var sleepMinute = 0
    private var wakeHour = 7
    private var wakeMinute = 0
    private var selectedCalendar = Calendar.getInstance()

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var dataList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        val dateBtn = findViewById<Button>(R.id.dateBtn)
        val sleepBtn = findViewById<Button>(R.id.sleepTimeBtn)
        val wakeBtn = findViewById<Button>(R.id.wakeTimeBtn)
        val saveBtn = findViewById<Button>(R.id.saveSleepBtn)
        val result = findViewById<TextView>(R.id.resultText)

        listView = findViewById(R.id.sleepList)

        // Initialize display
        updateDateButtonText(dateBtn)

        val prefs = getSharedPreferences("SleepData", MODE_PRIVATE)

        // Load data
        val saved = prefs.getStringSet("data", mutableSetOf())!!
        dataList = saved.toMutableList()

        // Custom adapter to ensure visibility on black background
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.WHITE)
                return view
            }
        }
        listView.adapter = adapter

        dateBtn.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateButtonText(dateBtn)
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        sleepBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                sleepHour = h
                sleepMinute = m
                sleepBtn.text = String.format("Bed: %02d:%02d", h, m)
            }, sleepHour, sleepMinute, true).show()
        }

        wakeBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                wakeHour = h
                wakeMinute = m
                wakeBtn.text = String.format("Wake: %02d:%02d", h, m)
            }, wakeHour, wakeMinute, true).show()
        }

        saveBtn.setOnClickListener {
            val sleepMin = sleepHour * 60 + sleepMinute
            var wakeMin = wakeHour * 60 + wakeMinute

            // If wake time is earlier than sleep time, assume it's the next day
            if (wakeMin <= sleepMin) wakeMin += 24 * 60

            val totalMinutes = wakeMin - sleepMin
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60

            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(selectedCalendar.time)

            val entry = "$dateStr: Slept $hours h $minutes m"

            dataList.add(0, entry)
            prefs.edit().putStringSet("data", dataList.toSet()).apply()
            adapter.notifyDataSetChanged()

            result.text = "Duration: $hours hrs $minutes mins"
            Toast.makeText(this, "Sleep record saved!", Toast.LENGTH_SHORT).show()
        }

        // DELETE (long press)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            dataList.removeAt(position)
            prefs.edit().putStringSet("data", dataList.toSet()).apply()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun updateDateButtonText(btn: Button) {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        btn.text = "Date: " + sdf.format(selectedCalendar.time)
    }
}