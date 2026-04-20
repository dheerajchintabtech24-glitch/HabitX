package com.example.habitx_pro

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SwimmingActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var saveBtn: Button
    private lateinit var listView: ListView

    private var dataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swimming)

        input = findViewById(R.id.lapInput)
        saveBtn = findViewById(R.id.saveLap)
        listView = findViewById(R.id.historyList)

        loadData()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        saveBtn.setOnClickListener {
            val laps = input.text.toString()

            if (laps.isEmpty()) {
                Toast.makeText(this, "Enter laps", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val entry = "$date - $laps laps"

            dataList.add(0, entry)
            saveData()

            adapter.notifyDataSetChanged()
            input.text.clear()
        }

        // DELETE (long press)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            dataList.removeAt(position)
            saveData()
            adapter.notifyDataSetChanged()
            true
        }
    }

    private fun saveData() {
        val prefs = getSharedPreferences("SwimData", MODE_PRIVATE)
        prefs.edit().putString("data", dataList.joinToString("|")).apply()
    }

    private fun loadData() {
        val prefs = getSharedPreferences("SwimData", MODE_PRIVATE)
        val saved = prefs.getString("data", "")

        if (!saved.isNullOrEmpty()) {
            dataList = saved.split("|").toMutableList()
        }
    }
}