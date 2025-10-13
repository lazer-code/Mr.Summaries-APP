package com.example.mrsummaries_app.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.APP_PREFERENCES,
        Context.MODE_PRIVATE
    )

    // User session
    private val KEY_IS_LOGGED_IN = "is_logged_in"
    private val KEY_USER_ID = "user_id"
    private val KEY_USERNAME = "username"
    private val KEY_EMAIL = "email"

    // App preferences
    private val KEY_DARK_MODE = "dark_mode"
    private val KEY_LAST_USED_PEN_COLOR = "last_used_pen_color"
    private val KEY_LAST_USED_PEN_SIZE = "last_used_pen_size"

    // User session methods
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun saveUserData(userId: String, username: String, email: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun clearUserSession() {
        sharedPreferences.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .apply()
    }

    // App preferences methods
    fun setDarkMode(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun saveLastUsedPenColor(colorInt: Int) {
        sharedPreferences.edit().putInt(KEY_LAST_USED_PEN_COLOR, colorInt).apply()
    }

    fun getLastUsedPenColor(defaultColor: Int): Int {
        return sharedPreferences.getInt(KEY_LAST_USED_PEN_COLOR, defaultColor)
    }

    fun saveLastUsedPenSize(size: Float) {
        sharedPreferences.edit().putFloat(KEY_LAST_USED_PEN_SIZE, size).apply()
    }

    fun getLastUsedPenSize(defaultSize: Float): Float {
        return sharedPreferences.getFloat(KEY_LAST_USED_PEN_SIZE, defaultSize)
    }
}