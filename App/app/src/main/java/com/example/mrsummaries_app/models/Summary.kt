package com.example.mrsummaries_app.models

import java.io.Serializable

data class Summary(
    val id: Int,
    val title: String,
    val content: String,
    val subject: String,
    val dateCreated: String,
    val imageUrl: String? = null
) : Serializable