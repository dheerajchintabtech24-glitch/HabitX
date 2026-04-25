package com.example.habitx_pro

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HabitAdapter(
    private val habits: List<Habit>,
    private val onItemClick: (Habit) -> Unit,
    private val onItemLongClick: (Habit) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_HEADER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (habits[position].id == "HEADER_ADDED") VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val habitImage: ImageView = view.findViewById(R.id.habitImage)
        val habitTitle: TextView = view.findViewById(R.id.habitTitle)
        val habitSubtitle: TextView = view.findViewById(R.id.habitSubtitle)
        val cardRoot: MaterialCardView = view.findViewById(R.id.cardRoot)
        val overlay: View = view.findViewById(R.id.gradientOverlay)
        val textContainer: LinearLayout = habitTitle.parent as LinearLayout
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTitle: TextView = view.findViewById(R.id.headerTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit_card, parent, false)
            HabitViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val habit = habits[position]

        if (holder is HeaderViewHolder) {
            holder.headerTitle.text = habit.title
        } else if (holder is HabitViewHolder) {
            holder.habitTitle.text = habit.title
            holder.habitSubtitle.text = habit.subtitle

            val context = holder.itemView.context
            val isCustom = habit.id.startsWith("custom_") || (habit.id.length >= 5 && habit.id != "walking" && habit.id != "yoga" && habit.id != "sleep" && habit.id != "meditation" && habit.id != "water" && habit.id != "reading")

            if (isCustom && habit.id != "HEADER_ADDED") {
                // Style for Custom Habits: Darker silky purple background, white text, top-left alignment
                holder.cardRoot.setCardBackgroundColor(Color.parseColor("#311B92")) // Deep Midnight Purple
                holder.habitImage.setImageDrawable(null)
                holder.habitImage.visibility = View.GONE
                holder.overlay.visibility = View.GONE
                
                holder.habitTitle.setTextColor(Color.WHITE)
                holder.habitSubtitle.setTextColor(Color.parseColor("#E0E0E0"))
                
                // Position text at Top Left for custom cards
                val params = holder.textContainer.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
                holder.textContainer.layoutParams = params
                
                holder.textContainer.gravity = Gravity.START or Gravity.TOP
                holder.habitTitle.gravity = Gravity.START
                
                if (habit.subtitle.isEmpty()) {
                    holder.habitSubtitle.visibility = View.GONE
                } else {
                    holder.habitSubtitle.visibility = View.VISIBLE
                    holder.habitSubtitle.gravity = Gravity.START
                }
            } else {
                // Style for Default Habits: Original Image background
                holder.cardRoot.setCardBackgroundColor(Color.WHITE)
                // Remove overlay to retain original image color
                holder.overlay.visibility = View.GONE 
                holder.habitImage.visibility = View.VISIBLE
                holder.habitImage.alpha = 1.0f
                holder.habitImage.colorFilter = null
                holder.habitImage.scaleType = ImageView.ScaleType.CENTER_CROP
                
                // Add text shadow for readability since overlay is gone
                holder.habitTitle.setTextColor(Color.WHITE)
                holder.habitTitle.setShadowLayer(8f, 0f, 0f, Color.BLACK)
                
                holder.habitSubtitle.setTextColor(Color.WHITE)
                holder.habitSubtitle.setShadowLayer(4f, 0f, 0f, Color.BLACK)
                holder.habitSubtitle.visibility = View.VISIBLE

                // Position text at Bottom Left for default cards
                val params = holder.textContainer.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
                holder.textContainer.layoutParams = params
                
                holder.textContainer.gravity = Gravity.START or Gravity.BOTTOM
                holder.habitTitle.gravity = Gravity.START
                holder.habitSubtitle.gravity = Gravity.START

                val resourceId = context.resources.getIdentifier(habit.imageResName, "drawable", context.packageName)
                if (resourceId != 0) {
                    holder.habitImage.setImageResource(resourceId)
                }
            }

            holder.cardRoot.setOnClickListener {
                onItemClick(habit)
            }

            holder.cardRoot.setOnLongClickListener {
                onItemLongClick(habit)
                true
            }
        }
    }

    override fun getItemCount() = habits.size
}
