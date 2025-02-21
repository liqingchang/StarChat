package com.alibaba.mnnllm.android.prompt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.mnnllm.android.R

class PromptAdapter(
    private val cardItems: List<PromptItem>,
    private val onItemClick: (PromptItem) -> Unit // 点击事件回调
) : RecyclerView.Adapter<PromptAdapter.CardViewHolder>() {

    // ViewHolder
    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        fun bind(cardItem:PromptItem) {
            titleTextView.text = cardItem.act
            contentTextView.text = cardItem.prompt
            itemView.setOnClickListener {
                onItemClick(cardItem) // 触发点击事件
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prompt, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cardItems[position])
    }

    override fun getItemCount(): Int {
        return cardItems.size
    }
}