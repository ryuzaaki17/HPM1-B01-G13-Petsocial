package com.example.petsocial.models

import android.net.Uri

data class Post(
    val id: String,
    val imageUrl: Uri,
    val content: String,
    val author: String,
    var likes: Int,
    val comments: MutableList<Comment> = mutableListOf()
)
