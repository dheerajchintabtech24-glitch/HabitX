package com.example.habitx_pro

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class YogaActivity : AppCompatActivity() {

    private var seconds = 0
    private var running = false
    private lateinit var handler: Handler

    private lateinit var timer: TextView
    private lateinit var startBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var listView: ListView

    private var dataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var auth: FirebaseAuth

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yoga)
        auth = FirebaseAuth.getInstance()

        timer = findViewById(R.id.timerText)
        startBtn = findViewById(R.id.startBtn)
        resetBtn = findViewById(R.id.resetBtn)
        saveBtn = findViewById(R.id.saveBtn)
        listView = findViewById(R.id.historyList)

        handler = Handler()

        loadData()

        // Styled adapter to ensure text is BLACK
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.BLACK)
                return view
            }
        }
        listView.adapter = adapter

        // START / STOP
        startBtn.setOnClickListener {
            running = !running
        }

        // RESET
        resetBtn.setOnClickListener {
            running = false
            seconds = 0
            updateTime()
        }

        // SAVE SESSION
        saveBtn.setOnClickListener {

            if (seconds == 0) {
                Toast.makeText(this, "No session to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val time = formatTime(seconds)

            val entry = "$date - $time"

            dataList.add(0, entry)
            saveData()

            adapter.notifyDataSetChanged()

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }

        // DELETE (LONG PRESS)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            dataList.removeAt(position)
            saveData()
            adapter.notifyDataSetChanged()
            true
        }

        handler.post(object : Runnable {
            override fun run() {
                if (running) {
                    seconds++
                    updateTime()
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun updateTime() {
        timer.text = formatTime(seconds)
    }

    private fun formatTime(sec: Int): String {
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun saveData() {
        val prefs = getSharedPreferences("YogaData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("data", dataList.joinToString("|")).apply()
    }

    private fun loadData() {
        val prefs = getSharedPreferences("YogaData_$userId", Context.MODE_PRIVATE)
        val saved = prefs.getString("data", "")

        if (!saved.isNullOrEmpty()) {
            dataList.clear()
            dataList.addAll(saved.split("|"))
        }
    }
}