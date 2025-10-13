package com.example.mrsummaries_app.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val university: String,
    val profileImageUrl: String? = null,
    val favoriteSubjects: List<String> = emptyList()
)