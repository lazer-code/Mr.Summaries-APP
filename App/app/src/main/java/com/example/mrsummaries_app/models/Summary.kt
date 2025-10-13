package com.example.mrsummaries_app.models

data class Summary(
    val id: String,
    val title: String,
    val author: String,
    val filePath: String,
    val timestamp: Long
)