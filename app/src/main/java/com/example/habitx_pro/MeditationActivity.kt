package com.example.habitx_pro

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MeditationActivity : AppCompatActivity() {

    private var seconds = 0
    private var running = false
    private var isPlayingMusic = false
    private lateinit var handler: Handler
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var timer: TextView
    private lateinit var startBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var musicBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var listView: ListView

    private var dataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var auth: FirebaseAuth

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meditation)
        auth = FirebaseAuth.getInstance()

        timer = findViewById(R.id.timerText)
        startBtn = findViewById(R.id.startBtn)
        resetBtn = findViewById(R.id.resetBtn)
        musicBtn = findViewById(R.id.musicBtn)
        saveBtn = findViewById(R.id.saveBtn)
        listView = findViewById(R.id.historyList)

        handler = Handler(Looper.getMainLooper())

        loadData()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        // START / STOP
        startBtn.setOnClickListener {
            running = !running
            startBtn.text = if (running) "Stop Timer" else "Start Timer"
        }

        // RESET
        resetBtn.setOnClickListener {
            running = false
            seconds = 0
            startBtn.text = "Start / Stop"
            updateTime()
        }

        // MUSIC TOGGLE (Plays on loop)
        musicBtn.setOnClickListener {
            toggleMusic()
        }

        // SAVE SESSION (System Date and Time)
        saveBtn.setOnClickListener {
            if (seconds == 0) {
                Toast.makeText(this, "No session to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentDateTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            val duration = formatTime(seconds)
            val entry = "$currentDateTime - $duration"

            dataList.add(0, entry)
            saveData()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Session Saved!", Toast.LENGTH_SHORT).show()
        }

        // DELETE (LONG PRESS)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            dataList.removeAt(position)
            saveData()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            true
        }

        // Timer implementation
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

    private fun toggleMusic() {
        if (isPlayingMusic) {
            mediaPlayer?.pause()
            musicBtn.text = "Play Music"
            isPlayingMusic = false
        } else {
            try {
                if (mediaPlayer == null) {
                    // Look for meditation_music.mp3 in res/raw
                    val resId = resources.getIdentifier("meditation_music", "raw", packageName)
                    if (resId != 0) {
                        mediaPlayer = MediaPlayer.create(this, resId)
                        mediaPlayer?.isLooping = true
                    } else {
                        Toast.makeText(this, "Please add 'meditation_music.mp3' to res/raw folder", Toast.LENGTH_LONG).show()
                        return
                    }
                }
                mediaPlayer?.start()
                musicBtn.text = "Pause Music"
                isPlayingMusic = true
            } catch (e: Exception) {
                Toast.makeText(this, "Error playing music", Toast.LENGTH_SHORT).show()
            }
        }
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
        val prefs = getSharedPreferences("MeditationData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("data", dataList.joinToString("|")).apply()
    }

    private fun loadData() {
        val prefs = getSharedPreferences("MeditationData_$userId", Context.MODE_PRIVATE)
        val saved = prefs.getString("data", "")
        if (!saved.isNullOrEmpty()) {
            dataList.clear()
            dataList.addAll(saved.split("|"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}