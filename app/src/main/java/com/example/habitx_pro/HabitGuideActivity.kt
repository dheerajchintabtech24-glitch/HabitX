package com.example.habitx_pro

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class HabitGuideActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var categoryLayout: LinearLayout
    private lateinit var questionsLayout: LinearLayout
    private lateinit var questionsTitle: TextView
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private val guideData = mapOf(
        "Walking" to listOf(
            "How many steps should I aim for daily?" to "Aim for at least 7,000 to 10,000 steps a day for optimal heart health and weight management.",
            "What is the best time for a walk?" to "Morning walks boost energy and metabolism, while evening walks can help with digestion and stress relief.",
            "How can I stay consistent with walking?" to "Try habit stacking: walk right after lunch or listen to your favorite podcast only while walking.",
            "What are the main health benefits?" to "Walking improves cardiovascular fitness, strengthens bones, reduces body fat, and boosts muscle power.",
            "What gear do I need for walking?" to "A supportive pair of walking shoes is essential. Moisture-wicking socks and a step tracker also help!"
        ),
        "Meditation" to listOf(
            "How do I start as a beginner?" to "Start with just 2-5 minutes of focusing on your breath. Sit comfortably and notice your inhalation and exhalation.",
            "When is the best time to meditate?" to "First thing in the morning sets a calm tone for the day, but before bed helps improve sleep quality.",
            "What if my mind keeps wandering?" to "That's normal! When you notice your mind wandering, gently bring your focus back to your breath without judgment.",
            "Can meditation help mental health?" to "Yes, it reduces stress, controls anxiety, promotes emotional health, and enhances self-awareness.",
            "How long should each session be?" to "Quality over quantity. 5-10 minutes of consistent daily practice is better than one long session per week."
        ),
        "Cooking" to listOf(
            "What are some easy healthy recipes?" to "Start with simple stir-fries, roasted vegetables with chicken, or overnight oats for breakfast.",
            "How can I start meal prepping?" to "Pick one day (like Sunday) to cook grains, proteins, and chop veggies for the entire week.",
            "What essential tools do I need?" to "A good chef's knife, a large cutting board, a non-stick pan, and a few glass storage containers.",
            "How to increase veggie intake?" to "Try 'sneaking' them into smoothies, sauces, or omelets. Aim for half your plate to be vegetables.",
            "How can I save time in the kitchen?" to "Clean as you go, use frozen pre-cut veggies, and master one-pot or sheet-pan meals."
        ),
        "Sleep" to listOf(
            "How can I improve sleep quality?" to "Keep your bedroom cool, dark, and quiet. Stick to a consistent sleep schedule even on weekends.",
            "How many hours is ideal?" to "Most adults need between 7 to 9 hours of quality sleep per night to function at their best.",
            "What's a good night routine?" to "Dim the lights 1 hour before bed, read a physical book, or take a warm bath to signal your body to rest.",
            "How to deal with insomnia?" to "Avoid caffeine late in the day, try relaxation techniques, and avoid napping for more than 20 minutes.",
            "Should I avoid screens?" to "Yes, blue light from phones and TVs interferes with melatonin production. Try to unplug 30-60 mins before bed."
        ),
        "Yoga" to listOf(
            "What are best beginner poses?" to "Start with Mountain Pose, Child's Pose, and Cat-Cow to build basic flexibility and awareness.",
            "Can yoga improve flexibility?" to "Absolutely! Consistent practice gently stretches muscles and connective tissues, increasing your range of motion.",
            "Is morning or evening yoga better?" to "Morning yoga wakes up the body, while evening yoga focuses on deep stretching and relaxation.",
            "Do I need special equipment?" to "A non-slip yoga mat is most important. Blocks and a strap can help if you're not yet very flexible.",
            "How does yoga help breathing?" to "Yoga emphasizes 'Pranayama' (breath control), which increases lung capacity and helps calm the nervous system."
        ),
        "Swimming" to listOf(
            "How can I build stamina?" to "Use interval training: swim one lap fast, then one lap slow. Gradually increase the number of fast laps.",
            "Which stroke is best for weight loss?" to "Butterfly is the most intense, but Freestyle (Front Crawl) is excellent for long-term calorie burning.",
            "Is swimming better than the gym?" to "It's different! Swimming is a low-impact full-body workout that's great for joints and cardio.",
            "What safety tips should I follow?" to "Never swim alone, stay hydrated, and know your limits. Always warm up before starting intense laps.",
            "What gear do I need?" to "A well-fitting swimsuit, goggles that don't leak, and a swim cap to protect your hair and reduce drag."
        ),
        "Gym" to listOf(
            "What's a good beginner routine?" to "Start with full-body workouts 3 times a week, focusing on compound movements like squats and rows.",
            "How many rest days do I need?" to "At least 1-2 days per week to allow your muscles to recover and grow. Listen to your body!",
            "What should I eat before/after?" to "Carbs for energy before, and protein + carbs for muscle recovery after your workout.",
            "How to stay motivated?" to "Find a workout buddy, set specific goals, and remember that showing up is the hardest part.",
            "How to track progress?" to "Use a fitness app or notebook to log your weights, reps, and how you feel during each session."
        ),
        "Singing" to listOf(
            "How to expand vocal range?" to "Practice scales daily and use gentle 'siren' exercises to connect your low and high registers.",
            "What are daily warm-ups?" to "Lip trills, humming, and 'Mah-May-Mee-Moh-Moo' scales are great for waking up your vocal cords.",
            "How to take care of my voice?" to "Stay hydrated, avoid screaming, and rest your voice if it feels strained or scratchy.",
            "How to overcome stage fright?" to "Prepare thoroughly, practice performing for friends first, and focus on the message of the song.",
            "How to choose the right song?" to "Pick songs that feel comfortable for your voice type and range, then gradually challenge yourself."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_guide)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        categoryLayout = findViewById(R.id.categoryLayout)
        questionsLayout = findViewById(R.id.questionsLayout)
        questionsTitle = findViewById(R.id.questionsTitle)

        adapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }

        setupCategories()

        if (messages.isEmpty()) {
            addMessage("Welcome! I'm your Habit Expert Guide. Select a category above to get started with expert advice.", false)
        }
    }

    private fun setupCategories() {
        guideData.keys.forEach { category ->
            val button = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            button.layoutParams = params
            button.text = category
            button.setOnClickListener {
                updateCategoryUI(it as MaterialButton)
                showQuestions(category)
            }
            categoryLayout.addView(button)
        }
    }

    private fun updateCategoryUI(selectedButton: MaterialButton) {
        for (i in 0 until categoryLayout.childCount) {
            val btn = categoryLayout.getChildAt(i) as? MaterialButton
            if (btn == selectedButton) {
                // Highlight selected category
                btn.backgroundTintList = ColorStateList.valueOf(getColor(R.color.purple_500))
                btn.setTextColor(Color.WHITE)
                btn.strokeColor = ColorStateList.valueOf(getColor(R.color.purple_500))
            } else {
                // Reset others to outlined style
                btn?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                btn?.setTextColor(Color.WHITE)
                btn?.strokeColor = ColorStateList.valueOf(getColor(R.color.purple_500))
            }
        }
    }

    private fun showQuestions(category: String) {
        questionsLayout.removeAllViews()
        questionsTitle.text = "Expert Questions for $category:"
        
        guideData[category]?.forEach { (question, answer) ->
            val button = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            button.layoutParams = params
            button.text = question
            button.textSize = 10f
            button.setOnClickListener {
                addMessage(question, true)
                addMessage(answer, false)
            }
            questionsLayout.addView(button)
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
    }
}
