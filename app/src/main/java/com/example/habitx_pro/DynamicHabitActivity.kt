package com.example.habitx_pro

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DynamicHabitActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var habitId: String
    private lateinit var habitTitle: String
    private lateinit var habitType: String
    private lateinit var auth: FirebaseAuth
    private val gson = Gson()

    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    // UI Elements
    private var titleText: TextView? = null
    private var tallyUi: LinearLayout? = null
    private var timerUi: LinearLayout? = null
    private var goalTimerUi: LinearLayout? = null
    private var audioUi: LinearLayout? = null
    private var timeRangeUi: LinearLayout? = null
    private var textUi: LinearLayout? = null
    private var pedometerUi: LinearLayout? = null
    private var imagesUi: LinearLayout? = null
    private var historyList: ListView? = null
    private var saveBtn: Button? = null
    private var rangeDurationText: TextView? = null

    // Tally State
    private var tallyCount = 0
    private var tallyCountText: TextView? = null

    // Timer State
    private var timerSeconds = 0
    private var timerRunning = false
    private var timerText: TextView? = null
    private var timerActionBtn: Button? = null
    private val timerHandler = Handler(Looper.getMainLooper())

    // Goal Timer State
    private var goalRemainingSeconds = 0
    private var goalTotalSeconds = 0
    private var goalRunning = false
    private var goalTimerDisplay: TextView? = null
    private val goalHandler = Handler(Looper.getMainLooper())

    // Audio State
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecordingAudio = false
    private var audioFile: File? = null
    private var audioSecondsElapsed = 0
    private var audioStatusText: TextView? = null
    private var audioTimerText: TextView? = null
    private var audioRecordBtn: ImageButton? = null
    private var audioRecordingsListView: ListView? = null
    private val audioRecordingsList = mutableListOf<String>()
    private lateinit var audioAdapter: ArrayAdapter<String>
    
    private var nowPlayingBar: MaterialCardView? = null
    private var playingProgressBar: ProgressBar? = null
    private var nowPlayingTitle: TextView? = null
    private var playPauseBtn: ImageButton? = null
    private var musicIcon: ImageView? = null
    private var pulseAnimator: AnimatorSet? = null

    private val audioHandler = Handler(Looper.getMainLooper())
    private val audioTimerRunnable = object : Runnable {
        override fun run() {
            audioSecondsElapsed++
            val m = audioSecondsElapsed / 60
            val s = audioSecondsElapsed % 60
            audioTimerText?.text = String.format("%02d:%02d", m, s)
            audioHandler.postDelayed(this, 1000)
        }
    }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val updateProgressAction = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    playingProgressBar?.progress = it.currentPosition
                    progressHandler.postDelayed(this, 100)
                }
            }
        }
    }

    // Time Range State
    private var startRangeDate = ""
    private var endRangeDate = ""
    private var startHourPicker: NumberPicker? = null
    private var startMinPicker: NumberPicker? = null
    private var startAmPmPicker: NumberPicker? = null
    private var endHourPicker: NumberPicker? = null
    private var endMinPicker: NumberPicker? = null
    private var endAmPmPicker: NumberPicker? = null
    private var startDateBtn: Button? = null
    private var endDateBtn: Button? = null

    // Pedometer State
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var initialSteps = -1
    private var currentSessionSteps = 0
    private var stepCountText: TextView? = null

    // Images State
    private var selectedImageUri: Uri? = null
    private var imagePreview: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val persistentPath = copyUriToFile(uri)
                if (persistentPath != null) {
                    selectedImageUri = Uri.fromFile(File(persistentPath))
                    imagePreview?.setImageURI(selectedImageUri)
                    imagePreview?.setColorFilter(0)
                }
            }
        }
    }

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
        titleText?.text = habitTitle
        
        rangeDurationText = findViewById(R.id.rangeDurationText)

        tallyUi = findViewById(R.id.tallyUi)
        timerUi = findViewById(R.id.timerUi)
        goalTimerUi = findViewById(R.id.goalTimerUi)
        audioUi = findViewById(R.id.audioUi)
        timeRangeUi = findViewById(R.id.timeRangeUi)
        textUi = findViewById(R.id.textUi)
        pedometerUi = findViewById(R.id.pedometerUi)
        imagesUi = findViewById(R.id.imagesUi)
        historyList = findViewById(R.id.dynamicHistoryList)
        saveBtn = findViewById(R.id.saveSessionBtn)

        nowPlayingBar = findViewById(R.id.nowPlayingBar)
        playingProgressBar = findViewById(R.id.playingProgressBar)
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        musicIcon = findViewById(R.id.musicIcon)

        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.WHITE)
                
                val item = dataList[position]
                if (item.startsWith("IMG|")) {
                    val parts = item.split("|")
                    if (parts.size >= 5) {
                        text.text = "${parts[2]} (on ${parts[3]} at ${parts[4]})"
                    }
                } else {
                    text.text = item
                }
                return view
            }
        }
        historyList?.adapter = adapter

        historyList?.setOnItemClickListener { _, _, position, _ ->
            val item = dataList[position]
            if (item.startsWith("IMG|")) {
                val parts = item.split("|")
                if (parts.size >= 2) {
                    showImagePopup(parts[1])
                }
            }
        }

        historyList?.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteHistoryDialog(position)
            true
        }

        saveBtn?.setOnClickListener { saveSession() }

        playPauseBtn?.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    playPauseBtn?.setImageResource(android.R.drawable.ic_media_play)
                    stopPulseAnimation()
                } else {
                    it.start()
                    playPauseBtn?.setImageResource(android.R.drawable.ic_media_pause)
                    startPulseAnimation()
                    progressHandler.post(updateProgressAction)
                }
            }
        }
    }

    private fun showImagePopup(path: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_viewer, null)
        val popupImg = dialogView.findViewById<ImageView>(R.id.popupImageView)
        val closeBtn = dialogView.findViewById<ImageButton>(R.id.closePopupBtn)

        val file = File(path)
        if (file.exists()) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(path, options)
                
                options.inSampleSize = calculateInSampleSize(options, 1080, 1920)
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    popupImg.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
        }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(dialogView)
            .create()

        closeBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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
                tallyUi?.visibility = View.VISIBLE
                tallyCountText = findViewById(R.id.tallyCountText)
                findViewById<Button>(R.id.incrementBtn).setOnClickListener {
                    tallyCount++
                    tallyCountText?.text = tallyCount.toString()
                }
                findViewById<Button>(R.id.decrementBtn).setOnClickListener {
                    if (tallyCount > 0) tallyCount--
                    tallyCountText?.text = tallyCount.toString()
                }
            }
            "Timer" -> {
                timerUi?.visibility = View.VISIBLE
                timerText = findViewById(R.id.timerText)
                timerActionBtn = findViewById(R.id.timerActionBtn)
                timerActionBtn?.setOnClickListener {
                    timerRunning = !timerRunning
                    timerActionBtn?.text = if (timerRunning) "Pause Timer" else "Resume Timer"
                }
                startTimerLoop()
            }
            "GoalTimer" -> {
                goalTimerUi?.visibility = View.VISIBLE
                saveBtn?.visibility = View.GONE 
                
                goalTimerDisplay = findViewById(R.id.goalTimerDisplay)
                val setGoalBtn = findViewById<Button>(R.id.setGoalBtn)
                val startBtn = findViewById<Button>(R.id.goalStartBtn)
                val pauseBtn = findViewById<Button>(R.id.goalPauseBtn)
                val resetBtn = findViewById<Button>(R.id.goalResetBtn)
                
                val hourPicker = findViewById<NumberPicker>(R.id.hourPicker)
                val minutePicker = findViewById<NumberPicker>(R.id.minutePicker)
                val secondPicker = findViewById<NumberPicker>(R.id.secondPicker)
                
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
                    
                    goalTimerDisplay?.text = String.format("%02d:%02d:%02d", h, m, s)
                    setGoalBtn.text = "Goal Confirmed: ${h}h ${m}m ${s}s"
                    saveBtn?.visibility = View.GONE 
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
                    saveBtn?.visibility = View.GONE 
                    goalRemainingSeconds = goalTotalSeconds
                    val h = goalRemainingSeconds / 3600
                    val m = (goalRemainingSeconds % 3600) / 60
                    val s = goalRemainingSeconds % 60
                    goalTimerDisplay?.text = String.format("%02d:%02d:%02d", h, m, s)
                }
            }
            "Audio" -> {
                audioUi?.visibility = View.VISIBLE
                audioStatusText = findViewById(R.id.audioStatusText)
                audioTimerText = findViewById(R.id.audioTimerText)
                audioRecordBtn = findViewById(R.id.audioRecordBtn)
                audioRecordingsListView = findViewById(R.id.audioRecordingsListView)
                
                loadAudioRecordings()
                
                audioAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, audioRecordingsList) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        val text = view.findViewById<TextView>(android.R.id.text1)
                        text.setTextColor(Color.WHITE)
                        return view
                    }
                }
                audioRecordingsListView?.adapter = audioAdapter
                
                audioRecordBtn?.setOnClickListener {
                    if (isRecordingAudio) {
                        stopAudioRecording()
                    } else {
                        if (checkAudioPermissions()) {
                            startAudioRecording()
                        } else {
                            requestAudioPermissions()
                        }
                    }
                }

                audioRecordingsListView?.setOnItemClickListener { _, _, position, _ ->
                    playRecording(audioRecordingsList[position])
                }

                audioRecordingsListView?.setOnItemLongClickListener { _, _, position, _ ->
                    showDeleteAudioDialog(position)
                    true
                }
            }
            "Images" -> {
                imagesUi?.visibility = View.VISIBLE
                imagePreview = findViewById(R.id.imagePreview)
                val galleryBtn = findViewById<Button>(R.id.galleryBtn)

                galleryBtn.setOnClickListener {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImageLauncher.launch(intent)
                }
            }
            "TimeRange" -> {
                timeRangeUi?.visibility = View.VISIBLE
                startDateBtn = findViewById(R.id.startDateBtn)
                endDateBtn = findViewById(R.id.endDateBtn)
                startHourPicker = findViewById(R.id.startHourPickerRange)
                startMinPicker = findViewById(R.id.startMinPickerRange)
                startAmPmPicker = findViewById(R.id.startAmPmPickerRange)
                endHourPicker = findViewById(R.id.endHourPickerRange)
                endMinPicker = findViewById(R.id.endMinPickerRange)
                endAmPmPicker = findViewById(R.id.endAmPmPickerRange)

                startHourPicker?.let { hp -> startMinPicker?.let { mp -> startAmPmPicker?.let { ap -> setupPickers(hp, mp, ap) } } }
                endHourPicker?.let { hp -> endMinPicker?.let { mp -> endAmPmPicker?.let { ap -> setupPickers(hp, mp, ap) } } }

                val c = Calendar.getInstance()
                startRangeDate = String.format("%02d/%02d/%04d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
                endRangeDate = startRangeDate
                startDateBtn?.text = startRangeDate
                endDateBtn?.text = endRangeDate

                startDateBtn?.setOnClickListener {
                    DatePickerDialog(this, { _, y, m, d ->
                        startRangeDate = String.format("%02d/%02d/%04d", d, m + 1, y)
                        startDateBtn?.text = startRangeDate
                        updateLiveDuration()
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }

                endDateBtn?.setOnClickListener {
                    DatePickerDialog(this, { _, y, m, d ->
                        endRangeDate = String.format("%02d/%02d/%04d", d, m + 1, y)
                        endDateBtn?.text = endRangeDate
                        updateLiveDuration()
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }

                val pickerListener = NumberPicker.OnValueChangeListener { _, _, _ -> updateLiveDuration() }
                startHourPicker?.setOnValueChangedListener(pickerListener)
                startMinPicker?.setOnValueChangedListener(pickerListener)
                startAmPmPicker?.setOnValueChangedListener(pickerListener)
                endHourPicker?.setOnValueChangedListener(pickerListener)
                endMinPicker?.setOnValueChangedListener(pickerListener)
                endAmPmPicker?.setOnValueChangedListener(pickerListener)

                updateLiveDuration()
            }
            "Text" -> {
                textUi?.visibility = View.VISIBLE
            }
            "Pedometer" -> {
                pedometerUi?.visibility = View.VISIBLE
                stepCountText = findViewById(R.id.stepCountText)
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                
                findViewById<Button>(R.id.resetPedometerBtn).setOnClickListener {
                    initialSteps = -1
                    currentSessionSteps = 0
                    stepCountText?.text = "0"
                }
            }
        }
    }

    private fun setupPickers(hp: NumberPicker, mp: NumberPicker, ap: NumberPicker) {
        hp.minValue = 1
        hp.maxValue = 12
        mp.minValue = 0
        mp.maxValue = 59
        mp.setFormatter { i -> String.format("%02d", i) }
        ap.minValue = 0
        ap.maxValue = 1
        ap.displayedValues = arrayOf("AM", "PM")
    }

    private fun checkAudioPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 201)
    }

    private fun startAudioRecording() {
        val fileName = "AUD_${System.currentTimeMillis()}.3gp"
        audioFile = File(getExternalFilesDir(null), fileName)

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile?.absolutePath)
            prepare()
            start()
        }

        isRecordingAudio = true
        audioRecordBtn?.setImageResource(android.R.drawable.ic_media_pause)
        audioStatusText?.text = "Recording Audio..."
        audioTimerText?.visibility = View.VISIBLE
        audioSecondsElapsed = 0
        audioHandler.post(audioTimerRunnable)
        
        nowPlayingBar?.visibility = View.GONE
        mediaPlayer?.stop()
        stopPulseAnimation()
    }

    private fun stopAudioRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        mediaRecorder = null
        isRecordingAudio = false
        audioRecordBtn?.setImageResource(android.R.drawable.ic_btn_speak_now)
        audioStatusText?.text = "Tap to start recording"
        audioTimerText?.visibility = View.INVISIBLE
        audioHandler.removeCallbacks(audioTimerRunnable)
        
        showAudioNamingDialog()
    }

    private fun showAudioNamingDialog() {
        val input = EditText(this)
        input.hint = "Recording Name"
        
        AlertDialog.Builder(this)
            .setTitle("Save Recording")
            .setMessage("Give your recording a name:")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifBlank { "New Recording" }
                saveAudioRecording(name)
            }
            .setNegativeButton("Discard") { _, _ ->
                audioFile?.delete()
            }
            .show()
    }

    private fun saveAudioRecording(name: String) {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val entry = "$name ($date)"
        
        audioRecordingsList.add(0, entry)
        
        val prefs = getSharedPreferences("AudioHabitFiles_${userId}_$habitId", Context.MODE_PRIVATE)
        prefs.edit().putString(entry, audioFile?.absolutePath).apply()
        
        saveAudioRecordingsList()
        audioAdapter.notifyDataSetChanged()
        
        Toast.makeText(this, "Recording Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun playRecording(entry: String) {
        val prefs = getSharedPreferences("AudioHabitFiles_${userId}_$habitId", Context.MODE_PRIVATE)
        val path = prefs.getString(entry, null)

        if (path != null && File(path).exists()) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnPreparedListener {
                    playingProgressBar?.max = it.duration
                    it.start()
                    
                    nowPlayingBar?.visibility = View.VISIBLE
                    nowPlayingTitle?.text = entry
                    nowPlayingTitle?.isSelected = true 
                    playPauseBtn?.setImageResource(android.R.drawable.ic_media_pause)
                    
                    startPulseAnimation()
                    progressHandler.post(updateProgressAction)
                }
                setOnCompletionListener {
                    nowPlayingBar?.visibility = View.GONE
                    stopPulseAnimation()
                    progressHandler.removeCallbacks(updateProgressAction)
                }
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPulseAnimation() {
        val icon = musicIcon ?: return
        if (pulseAnimator == null) {
            val scaleX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.2f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            val scaleY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.2f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            pulseAnimator = AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                start()
            }
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        musicIcon?.scaleX = 1f
        musicIcon?.scaleY = 1f
    }

    private fun showDeleteAudioDialog(position: Int) {
        val entry = audioRecordingsList[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete '$entry'?")
            .setPositiveButton("Delete") { _, _ ->
                val prefs = getSharedPreferences("AudioHabitFiles_${userId}_$habitId", Context.MODE_PRIVATE)
                val path = prefs.getString(entry, null)
                if (path != null) File(path).delete()
                prefs.edit().remove(entry).apply()
                
                audioRecordingsList.removeAt(position)
                saveAudioRecordingsList()
                audioAdapter.notifyDataSetChanged()
                
                if (nowPlayingTitle?.text == entry) {
                    mediaPlayer?.stop()
                    nowPlayingBar?.visibility = View.GONE
                    stopPulseAnimation()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAudioRecordingsList() {
        val prefs = getSharedPreferences("AudioHabitData_${userId}_$habitId", Context.MODE_PRIVATE)
        val json = gson.toJson(audioRecordingsList)
        prefs.edit().putString("list", json).apply()
    }

    private fun loadAudioRecordings() {
        val prefs = getSharedPreferences("AudioHabitData_${userId}_$habitId", Context.MODE_PRIVATE)
        val json = prefs.getString("list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            val savedList: MutableList<String>? = gson.fromJson(json, type)
            if (savedList != null) {
                audioRecordingsList.clear()
                audioRecordingsList.addAll(savedList)
            }
        }
    }

    private fun updateLiveDuration() {
        val detail = calculateTimeRangeDetail()
        if (detail.isNotEmpty() && detail != "Incomplete" && !detail.startsWith("End time")) {
            val durationOnly = detail.substringAfter("Duration: ").removeSuffix(")")
            rangeDurationText?.text = "Total Duration: $durationOnly"
            rangeDurationText?.setTextColor(Color.GREEN)
        } else if (detail.startsWith("End time")) {
            rangeDurationText?.text = detail
            rangeDurationText?.setTextColor(Color.RED)
        } else {
            rangeDurationText?.text = ""
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
                    timerText?.text = String.format("%02d:%02d:%02d", h, m, s)
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
                    goalTimerDisplay?.text = String.format("%02d:%02d:%02d", h, m, s)
                    
                    if (goalRemainingSeconds == 0) {
                        goalRunning = false
                        saveBtn?.visibility = View.VISIBLE 
                        vibratePhone()
                        playAlarmSound()
                        Toast.makeText(this@DynamicHabitActivity, "Goal Reached!", Toast.LENGTH_LONG).show()
                    } else {
                        goalHandler.postDelayed(this, 1000)
                    }
                }
            }
        })
    }

    private fun playAlarmSound() {
        try {
            val alarmPlayer = MediaPlayer.create(this, R.raw.alarm_music)
            alarmPlayer?.start()
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (alarmPlayer?.isPlaying == true) {
                        alarmPlayer.stop()
                    }
                    alarmPlayer?.release()
                } catch (e: Exception) { e.printStackTrace() }
            }, 5000)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
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
            stepCountText?.text = currentSessionSteps.toString()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveSession() {
        val now = Date()
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

        if (habitType == "Images") {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
                return
            }
            showImageNamingDialog(dateStr, timeStr)
            return
        }
        
        val detail = when (habitType) {
            "Tally" -> "Count: $tallyCount"
            "Timer" -> "Duration: ${timerText?.text ?: "00:00:00"}"
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
            "Audio" -> "Latest Audio session logged"
            "TimeRange" -> calculateTimeRangeDetail()
            "Text" -> findViewById<EditText>(R.id.textDetailsEdit).text.toString()
            "Pedometer" -> "Steps: $currentSessionSteps"
            else -> ""
        }

        if (detail.isEmpty() || detail == "Incomplete" || detail.startsWith("End time")) {
            Toast.makeText(this, if (detail.startsWith("End time")) detail else "Please select all dates and times", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = if (habitType == "TimeRange") detail else "on $dateStr at $timeStr - $detail"
        
        dataList.add(0, entry)
        adapter.notifyDataSetChanged()
        saveHistory()
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        resetAfterSave()
    }

    private fun showImageNamingDialog(dateStr: String, timeStr: String) {
        val input = EditText(this)
        input.hint = "Image Name (e.g. My Art)"
        
        AlertDialog.Builder(this)
            .setTitle("Save Image")
            .setMessage("Enter a name for this image:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifBlank { "New Image" }
                saveImageSession(name, dateStr, timeStr)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveImageSession(name: String, dateStr: String, timeStr: String) {
        val path = selectedImageUri?.path ?: ""
        if (path.isEmpty()) {
            Toast.makeText(this, "Failed to get image path", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = "IMG|$path|$name|$dateStr|$timeStr"
        dataList.add(0, entry)
        adapter.notifyDataSetChanged()
        saveHistory()
        Toast.makeText(this, "Image Saved!", Toast.LENGTH_SHORT).show()
        resetAfterSave()
    }

    private fun copyUriToFile(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun resetAfterSave() {
        when (habitType) {
            "Tally" -> {
                tallyCount = 0
                tallyCountText?.text = "0"
            }
            "Pedometer" -> {
                initialSteps = -1
                currentSessionSteps = 0
                stepCountText?.text = "0"
            }
            "TimeRange" -> {
                startRangeDate = ""
                endRangeDate = ""
                startDateBtn?.text = "Select Date"
                endDateBtn?.text = "Select Date"
                rangeDurationText?.text = ""
            }
            "Text" -> {
                findViewById<EditText>(R.id.textDetailsEdit).setText("")
            }
            "Audio" -> {
                audioStatusText?.text = "Tap to start recording"
                audioFile = null
            }
            "Images" -> {
                selectedImageUri = null
                imagePreview?.setImageResource(android.R.drawable.ic_menu_camera)
                imagePreview?.setColorFilter(Color.parseColor("#555555"))
            }
            "GoalTimer" -> {
                goalRunning = false
                saveBtn?.visibility = View.GONE 
                goalRemainingSeconds = goalTotalSeconds
                val h = goalRemainingSeconds / 3600
                val m = (goalRemainingSeconds % 3600) / 60
                val s = goalRemainingSeconds % 60
                goalTimerDisplay?.text = String.format("%02d:%02d:%02d", h, m, s)
            }
        }
    }

    private fun calculateTimeRangeDetail(): String {
        if (startRangeDate.isEmpty() || endRangeDate.isEmpty()) return "Incomplete"
        return try {
            val startH = startHourPicker!!.value
            val startM = startMinPicker!!.value
            val startAmPm = if (startAmPmPicker!!.value == 0) "AM" else "PM"
            val endH = endHourPicker!!.value
            val endM = endMinPicker!!.value
            val endAmPm = if (endAmPmPicker!!.value == 0) "AM" else "PM"
            val startTimeStr = String.format("%02d:%02d %s", startH, startM, startAmPm)
            val endTimeStr = String.format("%02d:%02d %s", endH, endM, endAmPm)
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            val date1 = sdf.parse("$startRangeDate $startTimeStr")
            val date2 = sdf.parse("$endRangeDate $endTimeStr")
            val diff = (date2?.time ?: 0) - (date1?.time ?: 0)
            if (diff < 0) return "End time cannot be before start time"
            val h = TimeUnit.MILLISECONDS.toHours(diff)
            val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            "Started on $startRangeDate at $startTimeStr to $endRangeDate at $endTimeStr (Duration: ${h}h ${m}m)"
        } catch (e: Exception) { "Incomplete" }
    }

    private fun saveHistory() {
        val prefs = getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE)
        val json = gson.toJson(dataList)
        prefs.edit().putString("data_$habitId", json).apply()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("DynamicHabitData_$userId", Context.MODE_PRIVATE)
        val json = prefs.getString("data_$habitId", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            try {
                val savedList: MutableList<String>? = gson.fromJson(json, type)
                if (savedList != null) {
                    dataList.clear()
                    dataList.addAll(savedList)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                // Fallback for old data if necessary, though it likely corrupted itself
                val oldData = json.split("|")
                dataList.clear()
                dataList.addAll(oldData)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (habitType == "Pedometer") {
            stepCounterSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun onPause() {
        super.onPause()
        if (habitType == "Pedometer") { sensorManager?.unregisterListener(this) }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null)
        goalHandler.removeCallbacksAndMessages(null)
        audioHandler.removeCallbacksAndMessages(null)
        mediaRecorder?.release()
        mediaPlayer?.release()
        stopPulseAnimation()
        progressHandler.removeCallbacks(updateProgressAction)
    }
}
