package com.jarvis.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.R
import com.jarvis.assistant.model.ChatMessage

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val userContainer: View   = v.findViewById(R.id.userContainer)
        private val jarvisContainer: View = v.findViewById(R.id.jarvisContainer)
        private val tvUser: TextView      = v.findViewById(R.id.tvUserMessage)
        private val tvJarvis: TextView    = v.findViewById(R.id.tvJarvisMessage)

        fun bind(msg: ChatMessage) {
            if (msg.role == "user") {
                userContainer.visibility   = View.VISIBLE
                jarvisContainer.visibility = View.GONE
                tvUser.text = msg.content
            } else {
                userContainer.visibility   = View.GONE
                jarvisContainer.visibility = View.VISIBLE
                tvJarvis.text = msg.content
            }
        }
    }

    private class Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.timestamp == b.timestamp
        override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
    }
}
