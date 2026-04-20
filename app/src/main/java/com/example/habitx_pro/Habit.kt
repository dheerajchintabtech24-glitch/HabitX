package com.example.habitx_pro

data class Habit(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageResName: String = "", // We can use this to map to drawable resources
    val activityClassName: String = "" // To know which activity to open
)
