package com.example.mrsummaries_app.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME, Context.MODE_PRIVATE
    )

    fun saveUserSession(userId: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Constants.KEY_USER_ID, userId)
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(Constants.KEY_USER_ID, null)
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
    }

    fun clearUserSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun saveDrawingMode(isDrawingMode: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_drawing_mode", isDrawingMode)
        editor.apply()
    }

    fun isDrawingMode(): Boolean {
        return sharedPreferences.getBoolean("is_drawing_mode", false)
    }

    fun savePenSize(size: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat("pen_size", size)
        editor.apply()
    }

    fun getPenSize(): Float {
        return sharedPreferences.getFloat("pen_size", Constants.DEFAULT_PEN_SIZE)
    }

    fun saveHighlighterSize(size: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat("highlighter_size", size)
        editor.apply()
    }

    fun getHighlighterSize(): Float {
        return sharedPreferences.getFloat("highlighter_size", Constants.DEFAULT_HIGHLIGHTER_SIZE)
    }

    fun saveEraserSize(size: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat("eraser_size", size)
        editor.apply()
    }

    fun getEraserSize(): Float {
        return sharedPreferences.getFloat("eraser_size", Constants.DEFAULT_ERASER_SIZE)
    }
}