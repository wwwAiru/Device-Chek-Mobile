package com.example.deviceinspectionapp.preferences

import android.content.Context

object AppPreferences {

    private const val PREF_NAME = "app_preferences"
    private const val ICONS_IN_ROW = "icons_in_row"
    private const val ICONS_IN_ROW_COUNT = 3   // 3 иконки в строке


    // Загружаем количество иконок в строке
    fun getIconsInRow(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(ICONS_IN_ROW, ICONS_IN_ROW_COUNT)
    }
}
