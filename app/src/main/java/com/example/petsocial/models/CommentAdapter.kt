package com.example.petsocial.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petsocial.R
import com.example.petsocial.models.Comment

class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAuthor: TextView = itemView.findViewById(R.id.textViewCommentAuthor)
        val textViewText: TextView = itemView.findViewById(R.id.textViewCommentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.textViewAuthor.text = comment.author
        holder.textViewText.text = comment.text
    }

    override fun getItemCount(): Int = comments.size
}