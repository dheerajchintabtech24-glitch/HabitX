package com.example.habitx_pro

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SingingActivity : AppCompatActivity() {

    private lateinit var recordBtn: ImageButton
    private lateinit var statusText: TextView
    private lateinit var timerText: TextView
    private lateinit var listView: ListView
    private lateinit var auth: FirebaseAuth

    // Spotify-style UI elements
    private lateinit var nowPlayingBar: MaterialCardView
    private lateinit var playingProgressBar: ProgressBar
    private lateinit var nowPlayingTitle: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var musicIcon: ImageView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var recordingFile: File? = null

    private var pulseAnimator: ObjectAnimator? = null

    private var secondsElapsed = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            val m = secondsElapsed / 60
            val s = secondsElapsed % 60
            timerText.text = String.format("%02d:%02d", m, s)
            handler.postDelayed(this, 1000)
        }
    }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val updateProgressAction = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    playingProgressBar.progress = it.currentPosition
                    progressHandler.postDelayed(this, 100)
                }
            }
        }
    }

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    private var recordingsList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singing)
        auth = FirebaseAuth.getInstance()

        recordBtn = findViewById(R.id.recordBtn)
        statusText = findViewById(R.id.recordingStatusText)
        timerText = findViewById(R.id.timerText)
        listView = findViewById(R.id.recordingsListView)

        // Init Spotify UI
        nowPlayingBar = findViewById(R.id.nowPlayingBar)
        playingProgressBar = findViewById(R.id.playingProgressBar)
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        musicIcon = findViewById(R.id.musicIcon)

        loadRecordings()

        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, recordingsList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.WHITE)
                return view
            }
        }
        listView.adapter = adapter

        recordBtn.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                if (checkPermissions()) {
                    startRecording()
                } else {
                    requestPermissions()
                }
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            playRecording(recordingsList[position])
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteHistoryDialog(position)
            true
        }

        playPauseBtn.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                    stopPulseAnimation()
                } else {
                    it.start()
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                    startPulseAnimation()
                    progressHandler.post(updateProgressAction)
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
    }

    private fun startRecording() {
        val fileName = "REC_${System.currentTimeMillis()}.3gp"
        recordingFile = File(getExternalFilesDir(null), fileName)

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(recordingFile?.absolutePath)
            prepare()
            start()
        }

        isRecording = true
        recordBtn.setImageResource(android.R.drawable.ic_media_pause)
        statusText.text = "Recording..."
        timerText.visibility = View.VISIBLE
        secondsElapsed = 0
        handler.post(timerRunnable)
        
        // Hide playing bar when recording starts
        nowPlayingBar.visibility = View.GONE
        mediaPlayer?.stop()
        stopPulseAnimation()
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        mediaRecorder = null
        isRecording = false
        recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now)
        statusText.text = "Tap to start recording"
        timerText.visibility = View.INVISIBLE
        handler.removeCallbacks(timerRunnable)

        showNamingDialog()
    }

    private fun showNamingDialog() {
        val input = EditText(this)
        input.hint = "Song Name"
        
        AlertDialog.Builder(this)
            .setTitle("Save Recording")
            .setMessage("Give your recording a name:")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifBlank { "My Song" }
                saveRecording(name)
            }
            .setNegativeButton("Discard") { _, _ ->
                recordingFile?.delete()
            }
            .show()
    }

    private fun saveRecording(name: String) {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val entry = "$name ($date)"
        
        recordingsList.add(0, entry)
        
        val prefs = getSharedPreferences("SingingFiles_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString(entry, recordingFile?.absolutePath).apply()
        
        saveRecordingsList()
        updateHistory(date)
        adapter.notifyDataSetChanged()
        
        Toast.makeText(this, "Recording Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun playRecording(entry: String) {
        val prefs = getSharedPreferences("SingingFiles_$userId", Context.MODE_PRIVATE)
        val path = prefs.getString(entry, null)

        if (path != null && File(path).exists()) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnPreparedListener {
                    playingProgressBar.max = it.duration
                    it.start()
                    
                    nowPlayingBar.visibility = View.VISIBLE
                    nowPlayingTitle.text = entry
                    nowPlayingTitle.isSelected = true // Enable marquee
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                    
                    startPulseAnimation()
                    progressHandler.post(updateProgressAction)
                }
                setOnCompletionListener {
                    nowPlayingBar.visibility = View.GONE
                    stopPulseAnimation()
                    progressHandler.removeCallbacks(updateProgressAction)
                }
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPulseAnimation() {
        if (pulseAnimator == null) {
            pulseAnimator = ObjectAnimator.ofFloat(musicIcon, "scaleX", 1f, 1.2f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            ObjectAnimator.ofFloat(musicIcon, "scaleY", 1f, 1.2f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
            pulseAnimator?.start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        musicIcon.scaleX = 1f
        musicIcon.scaleY = 1f
    }

    private fun showDeleteHistoryDialog(position: Int) {
        val entry = recordingsList[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete '$entry'?")
            .setPositiveButton("Delete") { _, _ ->
                val prefs = getSharedPreferences("SingingFiles_$userId", Context.MODE_PRIVATE)
                val path = prefs.getString(entry, null)
                if (path != null) File(path).delete()
                prefs.edit().remove(entry).apply()
                
                recordingsList.removeAt(position)
                saveRecordingsList()
                adapter.notifyDataSetChanged()
                
                // Hide player if we delete what's playing
                if (nowPlayingTitle.text == entry) {
                    mediaPlayer?.stop()
                    nowPlayingBar.visibility = View.GONE
                    stopPulseAnimation()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateHistory(date: String) {
        val prefs = getSharedPreferences("SingingData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("last_recorded", date).apply()
    }

    private fun saveRecordingsList() {
        val prefs = getSharedPreferences("SingingData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("list", recordingsList.joinToString("|")).apply()
    }

    private fun loadRecordings() {
        val prefs = getSharedPreferences("SingingData_$userId", Context.MODE_PRIVATE)
        val saved = prefs.getString("list", "")
        if (!saved.isNullOrEmpty()) {
            recordingsList.clear()
            recordingsList.addAll(saved.split("|"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
        stopPulseAnimation()
        progressHandler.removeCallbacks(updateProgressAction)
    }
}
