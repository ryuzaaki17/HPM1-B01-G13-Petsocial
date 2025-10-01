package com.example.petsocial.models

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petsocial.models.Comment
import com.example.petsocial.models.Post
import java.util.UUID

class PostViewModel : ViewModel() {

    private val _posts = MutableLiveData<MutableList<Post>>()
    val posts: LiveData<MutableList<Post>> = _posts

    init {
        // Datos de ejemplo
        // Datos de ejemplo
        val initialPosts = mutableListOf(
            Post(
                id = UUID.randomUUID().toString(),
                imageUrl = Uri.parse("android.resource://com.example.petsocial/drawable/golden"),
                content = "Hoy Max aprendió un nuevo truco, ¡ya da la patita!",
                author = "Luna (Golden Retriever)",
                likes = 18,
                comments = mutableListOf(
                    Comment(UUID.randomUUID().toString(), "Simba (Gato)", "¡Qué inteligente ese perrito!"),
                    Comment(UUID.randomUUID().toString(), "Coco (Poodle)", "Yo también quiero aprender eso.")
                )
            ),
            Post(
                id = UUID.randomUUID().toString(),
                imageUrl = Uri.parse("android.resource://com.example.petsocial/drawable/siames"),
                content = "Mia se quedó dormida en la ventana con el sol de la tarde ☀️🐱",
                author = "Mia (Siamesa)",
                likes = 25,
                comments = mutableListOf(
                    Comment(UUID.randomUUID().toString(), "Rocky (Bulldog)", "¡Qué tierna se ve!"),
                    Comment(UUID.randomUUID().toString(), "Nala (Persa)", "Ese es mi lugar favorito también.")
                )
            ),
            Post(
                id = UUID.randomUUID().toString(),
                imageUrl = Uri.parse("android.resource://com.example.petsocial/drawable/pastoraleman"),
                content = "Firulais estrenando su nuevo juguete, no lo suelta ni para dormir 😂",
                author = "Firulais (Pastor Alemán)",
                likes = 33,
                comments = mutableListOf(
                    Comment(UUID.randomUUID().toString(), "Tommy (Beagle)", "Ese juguete se ve genial."),
                    Comment(UUID.randomUUID().toString(), "Lola (Gatita)", "Yo rompería ese muñeco en 5 minutos 😼")
                )
            )
        )
        _posts.value = initialPosts
    }

    fun addPost(post: Post) {
        val currentPosts = _posts.value ?: mutableListOf()
        currentPosts.add(0, post) // Añadir al principio
        _posts.value = currentPosts // Disparar la actualización
    }

    fun addLike(postId: String) {
        val currentPosts = _posts.value ?: return
        val post = currentPosts.find { it.id == postId }
        post?.let {
            it.likes++
            _posts.value = currentPosts // Disparar la actualización
        }
    }

    fun addComment(postId: String, comment: Comment) {
        val currentPosts = _posts.value ?: return
        val post = currentPosts.find { it.id == postId }
        post?.let {
            it.comments.add(comment)
            _posts.value = currentPosts // Disparar la actualización
        }
    }
}
