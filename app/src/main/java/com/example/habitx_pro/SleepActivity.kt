package com.example.habitx_pro

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SleepActivity : AppCompatActivity() {

    private var sleepHour = 0
    private var sleepMinute = 0
    private var wakeHour = 0
    private var wakeMinute = 0

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var dataList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        val sleepBtn = findViewById<Button>(R.id.sleepTimeBtn)
        val wakeBtn = findViewById<Button>(R.id.wakeTimeBtn)
        val saveBtn = findViewById<Button>(R.id.saveSleepBtn)
        val result = findViewById<TextView>(R.id.resultText)

        listView = findViewById(R.id.sleepList)

        val prefs = getSharedPreferences("SleepData", MODE_PRIVATE)

        // Load data
        val saved = prefs.getStringSet("data", mutableSetOf())!!
        dataList = saved.toMutableList()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        sleepBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                sleepHour = h
                sleepMinute = m
                sleepBtn.text = "Sleep: $h:$m"
            }, 22, 0, true).show()
        }

        wakeBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                wakeHour = h
                wakeMinute = m
                wakeBtn.text = "Wake: $h:$m"
            }, 6, 0, true).show()
        }

        saveBtn.setOnClickListener {

            val sleepMin = sleepHour * 60 + sleepMinute
            var wakeMin = wakeHour * 60 + wakeMinute

            if (wakeMin < sleepMin) wakeMin += 24 * 60

            val total = wakeMin - sleepMin
            val hours = total / 60.0

            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

            val entry = "$date - %.1f hrs".format(hours)

            dataList.add(0, entry)

            prefs.edit().putStringSet("data", dataList.toSet()).apply()

            adapter.notifyDataSetChanged()

            result.text = "Slept: %.1f hrs".format(hours)
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
}