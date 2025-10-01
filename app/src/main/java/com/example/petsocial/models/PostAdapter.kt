package com.example.petsocial.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petsocial.R
import com.example.petsocial.models.Post
import com.google.android.material.button.MaterialButton

class PostAdapter(
    private val posts: MutableList<Post>,
    private val onLikeClickListener: (Post) -> Unit,
    private val onCommentClickListener: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPost: ImageView = itemView.findViewById(R.id.imageViewPost)
        val textViewAuthor: TextView = itemView.findViewById(R.id.textViewAuthor)
        val textViewContent: TextView = itemView.findViewById(R.id.textViewContent)
        val buttonLike: MaterialButton = itemView.findViewById(R.id.buttonLike)
        val buttonComment: MaterialButton = itemView.findViewById(R.id.buttonComment)
        val recyclerViewComments: RecyclerView = itemView.findViewById(R.id.recyclerViewComments)

        init {
            recyclerViewComments.layoutManager = LinearLayoutManager(itemView.context)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Cargar imagen (simulado para recursos locales)
        try {
            holder.imageViewPost.setImageURI(post.imageUrl)
        } catch (e: Exception) {
            // Manejar error si la URI no es válida o el recurso no existe
            holder.imageViewPost.setImageResource(R.drawable.image_not_found) // Un placeholder
        }

        holder.textViewAuthor.text = post.author
        holder.textViewContent.text = post.content
        holder.buttonLike.text = post.likes.toString()

        holder.buttonLike.setOnClickListener {
            onLikeClickListener(post)
            notifyItemChanged(position) // Actualizar solo este ítem
        }

        holder.buttonComment.setOnClickListener {
            onCommentClickListener(post)
        }

        // Configurar RecyclerView de comentarios
        val commentAdapter = CommentAdapter(post.comments)
        holder.recyclerViewComments.adapter = commentAdapter
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
