package com.example.petsocial.ui.post

import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petsocial.R
import com.example.petsocial.models.PostAdapter
import com.example.petsocial.models.Comment
import com.example.petsocial.models.Post
import com.example.petsocial.models.PostViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class PostFragment : Fragment() {

    private lateinit var viewModel: PostViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var recyclerViewPosts: RecyclerView
    private lateinit var fabAddPost: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(PostViewModel::class.java)

        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts)
        fabAddPost = view.findViewById(R.id.fabAddPost)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            mutableListOf(),
            onLikeClickListener = { post ->
                viewModel.addLike(post.id)
            },
            onCommentClickListener = { post ->
                showAddCommentDialog(post)
            }
        )
        recyclerViewPosts.layoutManager = LinearLayoutManager(context)
        recyclerViewPosts.adapter = postAdapter
    }

    private fun setupFab() {
        fabAddPost.setOnClickListener {
            showAddPostDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.updatePosts(posts)
        }
    }

    private fun showAddPostDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nueva Publicación")

        val inputLayout = LinearLayout(requireContext())
        inputLayout.orientation = LinearLayout.VERTICAL
        inputLayout.setPadding(50, 20, 50, 20)

        val inputAuthor = TextInputEditText(requireContext())
        inputAuthor.hint = "Tu nombre"
        inputLayout.addView(inputAuthor)

        val inputContent = TextInputEditText(requireContext())
        inputContent.hint = "Contenido de la publicación"
        inputLayout.addView(inputContent)

        val inputImageUrl = TextInputEditText(requireContext())
        inputImageUrl.hint = "URL de la imagen (ej: https://picsum.photos/400/200)"
        inputLayout.addView(inputImageUrl)

        builder.setView(inputLayout)

        builder.setPositiveButton("Publicar") { dialog, _ ->
            val author = inputAuthor.text.toString().trim()
            val content = inputContent.text.toString().trim()
            val imageUrlText = inputImageUrl.text.toString().trim()

            if (author.isNotEmpty() && content.isNotEmpty()) {
                val imageUrl = if (imageUrlText.isNotEmpty()) {
                    Uri.parse(imageUrlText)
                } else {
                    // Imagen de ejemplo por defecto si no se proporciona URL
                    Uri.parse("android.resource://${requireContext().packageName}/drawable/placeholder_image")
                }
                val newPost = Post(
                    id = UUID.randomUUID().toString(),
                    imageUrl = imageUrl,
                    content = content,
                    author = author,
                    likes = 0
                )
                viewModel.addPost(newPost)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showAddCommentDialog(post: Post) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Añadir Comentario")

        val inputLayout = LinearLayout(requireContext())
        inputLayout.orientation = LinearLayout.VERTICAL
        inputLayout.setPadding(50, 20, 50, 20)

        val inputAuthor = TextInputEditText(requireContext())
        inputAuthor.hint = "Tu nombre"
        inputLayout.addView(inputAuthor)

        val inputComment = TextInputEditText(requireContext())
        inputComment.hint = "Escribe tu comentario"
        inputLayout.addView(inputComment)

        builder.setView(inputLayout)

        builder.setPositiveButton("Comentar") { dialog, _ ->
            val author = inputAuthor.text.toString().trim()
            val commentText = inputComment.text.toString().trim()

            if (author.isNotEmpty() && commentText.isNotEmpty()) {
                val newComment = Comment(
                    id = UUID.randomUUID().toString(),
                    author = author,
                    text = commentText
                )
                viewModel.addComment(post.id, newComment)
                // Es importante notificar al adapter que los datos han cambiado para que se refresque la vista
                postAdapter.notifyDataSetChanged() // Se podría optimizar notificando solo el item del post
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Por favor, escribe tu nombre y tu comentario", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
