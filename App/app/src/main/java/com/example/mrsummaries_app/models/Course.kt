package com.example.mrsummaries_app.models

import androidx.annotation.DrawableRes
import java.io.Serializable

data class Course(
    val id: String,
    val name: String,
    val courseCode: String,
    val description: String = "",
    val summariesCount: Int = 0,
    @DrawableRes val iconResId: Int = 0
) : Serializable