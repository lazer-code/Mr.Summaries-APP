package com.example.mrsummaries_app.util

object Constants {
    // Shared Preferences
    const val PREF_NAME = "mr_summaries_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"

    // Navigation
    const val NAV_HOME = "home"
    const val NAV_PROFILE = "profile"
    const val NAV_SUMMARY = "summary"
    const val NAV_NOTE_EDITOR = "note_editor"

    // Subjects
    val SUBJECTS = arrayOf(
        "Math",
        "Physics",
        "Computer Science",
        "Chemistry",
        "Biology",
        "History",
        "Literature"
    )

    // Drawing
    const val MIN_STROKE_WIDTH = 1f
    const val DEFAULT_PEN_SIZE = 5f
    const val DEFAULT_HIGHLIGHTER_SIZE = 10f
    const val DEFAULT_ERASER_SIZE = 20f
}