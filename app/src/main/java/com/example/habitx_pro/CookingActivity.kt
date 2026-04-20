package com.example.habitx_pro

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CookingActivity : AppCompatActivity() {

    private lateinit var dishNameInput: EditText
    private lateinit var recipeInput: EditText
    private lateinit var caloriesInput: EditText
    private lateinit var saveRecipeBtn: Button
    private lateinit var cookingHistoryList: ListView

    private var cookingDataList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooking)

        dishNameInput = findViewById(R.id.dishNameInput)
        recipeInput = findViewById(R.id.recipeInput)
        caloriesInput = findViewById(R.id.caloriesInput)
        saveRecipeBtn = findViewById(R.id.saveRecipeBtn)
        cookingHistoryList = findViewById(R.id.cookingHistoryList)

        loadCookingData()

        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, cookingDataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }
        cookingHistoryList.adapter = adapter

        saveRecipeBtn.setOnClickListener {
            saveNewRecipe()
        }

        cookingHistoryList.setOnItemLongClickListener { _, _, position, _ ->
            cookingDataList.removeAt(position)
            saveCookingData()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Recipe Removed", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun saveNewRecipe() {
        val dishName = dishNameInput.text.toString().trim()
        val recipe = recipeInput.text.toString().trim()
        val calories = caloriesInput.text.toString().trim()

        if (dishName.isEmpty() || recipe.isEmpty()) {
            Toast.makeText(this, "Please enter dish name and recipe", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDateTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val calorieText = if (calories.isNotEmpty()) " ($calories kcal)" else ""
        
        val entry = "$dishName - $currentDateTime\n$recipe$calorieText"

        cookingDataList.add(0, entry)
        saveCookingData()
        adapter.notifyDataSetChanged()

        // Clear inputs
        dishNameInput.text.clear()
        recipeInput.text.clear()
        caloriesInput.text.clear()
        
        Toast.makeText(this, "Recipe Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun saveCookingData() {
        val prefs = getSharedPreferences("CookingData", MODE_PRIVATE)
        prefs.edit().putString("recipes", cookingDataList.joinToString("|||")).apply()
    }

    private fun loadCookingData() {
        val prefs = getSharedPreferences("CookingData", MODE_PRIVATE)
        val saved = prefs.getString("recipes", "")
        if (!saved.isNullOrEmpty()) {
            cookingDataList = saved.split("|||").toMutableList()
        }
    }
}
