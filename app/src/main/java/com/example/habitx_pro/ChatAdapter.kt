package com.example.habitx_pro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userCard: CardView = view.findViewById(R.id.userMessageCard)
        val botCard: CardView = view.findViewById(R.id.botMessageCard)
        val userText: TextView = view.findViewById(R.id.userText)
        val botText: TextView = view.findViewById(R.id.botText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.userCard.visibility = View.VISIBLE
            holder.botCard.visibility = View.GONE
            holder.userText.text = message.text
        } else {
            holder.botCard.visibility = View.VISIBLE
            holder.userCard.visibility = View.GONE
            holder.botText.text = message.text
        }
    }

    override fun getItemCount() = messages.size
}