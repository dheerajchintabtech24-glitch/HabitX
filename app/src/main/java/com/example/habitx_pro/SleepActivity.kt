package com.example.habitx_pro

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SleepActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val userId: String
        get() = auth.currentUser?.uid ?: "default_user"

    private var startDateStr = ""
    private var endDateStr = ""

    private var startHourPicker: NumberPicker? = null
    private var startMinPicker: NumberPicker? = null
    private var startAmPmPicker: NumberPicker? = null
    private var endHourPicker: NumberPicker? = null
    private var endMinPicker: NumberPicker? = null
    private var endAmPmPicker: NumberPicker? = null
    private var resultText: TextView? = null

    private var dataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_sleep)
            auth = FirebaseAuth.getInstance()

            val startDateBtn = findViewById<MaterialButton>(R.id.startDateBtn)
            val endDateBtn = findViewById<MaterialButton>(R.id.endDateBtn)
            val saveBtn = findViewById<MaterialButton>(R.id.saveSleepBtn)
            resultText = findViewById(R.id.resultText)
            val listView = findViewById<ListView>(R.id.sleepList)

            startHourPicker = findViewById(R.id.startHourPicker)
            startMinPicker = findViewById(R.id.startMinPicker)
            startAmPmPicker = findViewById(R.id.startAmPmPicker)
            endHourPicker = findViewById(R.id.endHourPicker)
            endMinPicker = findViewById(R.id.endMinPicker)
            endAmPmPicker = findViewById(R.id.endAmPmPicker)

            // Safely initialize pickers
            startHourPicker?.let { setupPicker(it, 1, 12, 10) }
            startMinPicker?.let { setupPicker(it, 0, 59, 0) }
            startAmPmPicker?.let { setupAmPmPicker(it, 1) } // Default PM

            endHourPicker?.let { setupPicker(it, 1, 12, 7) }
            endMinPicker?.let { setupPicker(it, 0, 59, 0) }
            endAmPmPicker?.let { setupAmPmPicker(it, 0) } // Default AM

            val prefs = getSharedPreferences("SleepData_$userId", Context.MODE_PRIVATE)
            val saved = prefs.getString("history", "")
            if (!saved.isNullOrEmpty()) {
                dataList.addAll(saved.split("|").filter { it.isNotEmpty() })
            }

            adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val text = view.findViewById<TextView>(android.R.id.text1)
                    text.setTextColor(Color.WHITE)
                    text.textSize = 14f
                    return view
                }
            }
            listView?.adapter = adapter

            val calendar = Calendar.getInstance()
            startDateStr = formatDate(calendar)
            endDateStr = startDateStr
            startDateBtn?.text = startDateStr
            endDateBtn?.text = endDateStr

            startDateBtn?.setOnClickListener {
                showDatePicker { date ->
                    startDateStr = date
                    startDateBtn.text = date
                    updateLiveDuration()
                }
            }

            endDateBtn?.setOnClickListener {
                showDatePicker { date ->
                    endDateStr = date
                    endDateBtn.text = date
                    updateLiveDuration()
                }
            }

            val pickerListener = NumberPicker.OnValueChangeListener { _, _, _ -> updateLiveDuration() }
            startHourPicker?.setOnValueChangedListener(pickerListener)
            startMinPicker?.setOnValueChangedListener(pickerListener)
            startAmPmPicker?.setOnValueChangedListener(pickerListener)
            endHourPicker?.setOnValueChangedListener(pickerListener)
            endMinPicker?.setOnValueChangedListener(pickerListener)
            endAmPmPicker?.setOnValueChangedListener(pickerListener)

            saveBtn?.setOnClickListener {
                val detail = calculateDurationDetail()
                if (detail.startsWith("Error") || detail.startsWith("End time")) {
                    Toast.makeText(this, detail, Toast.LENGTH_SHORT).show()
                } else {
                    dataList.add(0, detail)
                    adapter.notifyDataSetChanged()
                    saveHistory()
                    Toast.makeText(this, "Sleep record saved!", Toast.LENGTH_SHORT).show()
                }
            }

            listView?.setOnItemLongClickListener { _, _, position, _ ->
                dataList.removeAt(position)
                adapter.notifyDataSetChanged()
                saveHistory()
                true
            }

            updateLiveDuration()

        } catch (e: Exception) {
            // Log the error to Toast to help the user see what's wrong
            Toast.makeText(this, "Sleep Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupPicker(picker: NumberPicker, min: Int, max: Int, default: Int) {
        picker.minValue = min
        picker.maxValue = max
        picker.value = default
        picker.setFormatter { i -> String.format("%02d", i) }
        picker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun setupAmPmPicker(picker: NumberPicker, default: Int) {
        picker.minValue = 0
        picker.maxValue = 1
        picker.value = default
        picker.displayedValues = arrayOf("AM", "PM")
        picker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val sel = Calendar.getInstance()
            sel.set(y, m, d)
            onDateSelected(formatDate(sel))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun formatDate(calendar: Calendar): String {
        return String.format("%02d/%02d/%04d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }

    private fun updateLiveDuration() {
        val detail = calculateDurationDetail()
        if (detail.contains("Duration:")) {
            val duration = detail.substringAfter("Duration: ").removeSuffix(")")
            resultText?.text = "Duration: $duration"
            resultText?.setTextColor(Color.GREEN)
        } else if (detail.startsWith("End time")) {
            resultText?.text = detail
            resultText?.setTextColor(Color.RED)
        } else {
            resultText?.text = ""
        }
    }

    private fun calculateDurationDetail(): String {
        try {
            val sH = startHourPicker?.value ?: 10
            val sM = startMinPicker?.value ?: 0
            val sAP = if (startAmPmPicker?.value == 0) "AM" else "PM"
            
            val eH = endHourPicker?.value ?: 7
            val eM = endMinPicker?.value ?: 0
            val eAP = if (endAmPmPicker?.value == 0) "AM" else "PM"

            val startTime = String.format("%02d:%02d %s", sH, sM, sAP)
            val endTime = String.format("%02d:%02d %s", eH, eM, eAP)

            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)
            val d1 = sdf.parse("$startDateStr $startTime")
            val d2 = sdf.parse("$endDateStr $endTime")

            if (d1 == null || d2 == null) return "Error parsing dates"

            val diff = d2.time - d1.time
            if (diff < 0) return "End time is before start time"

            val h = TimeUnit.MILLISECONDS.toHours(diff)
            val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

            return "Started on $startDateStr at $startTime to $endDateStr at $endTime (Duration: ${h}h ${m}m)"
        } catch (e: Exception) {
            return "Error calculating duration"
        }
    }

    private fun saveHistory() {
        val prefs = getSharedPreferences("SleepData_$userId", Context.MODE_PRIVATE)
        prefs.edit().putString("history", dataList.joinToString("|")).apply()
    }
}
