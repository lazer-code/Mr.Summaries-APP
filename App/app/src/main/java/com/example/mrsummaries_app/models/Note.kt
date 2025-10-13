package com.example.mrsummaries_app.models

import java.io.Serializable
import java.util.*

data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var content: String = "",
    var dateCreated: Date = Date(),
    var dateModified: Date = Date(),
    var subject: String = "",
    var drawingData: String? = null,
    var isFavorite: Boolean = false
) : Serializable