package com.example.habitx_pro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private val habits: List<Habit>,
    private val onItemClick: (Habit) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val habitImage: ImageView = view.findViewById(R.id.habitImage)
        val habitTitle: TextView = view.findViewById(R.id.habitTitle)
        val habitSubtitle: TextView = view.findViewById(R.id.habitSubtitle)
        val cardRoot: View = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_card, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.habitTitle.text = habit.title
        holder.habitSubtitle.text = habit.subtitle

        // Map resource name to ID
        val context = holder.itemView.context
        val resourceId = context.resources.getIdentifier(habit.imageResName, "drawable", context.packageName)
        if (resourceId != 0) {
            holder.habitImage.setImageResource(resourceId)
        }

        holder.cardRoot.setOnClickListener {
            onItemClick(habit)
        }
    }

    override fun getItemCount() = habits.size
}
