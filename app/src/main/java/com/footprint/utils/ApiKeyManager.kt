package com.footprint.utils

import android.content.Context
import androidx.core.content.edit

object ApiKeyManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_AMAP_API_KEY = "amap_api_key"

    fun getApiKey(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AMAP_API_KEY, null)
    }

    fun setApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_AMAP_API_KEY, apiKey)
        }
    }
    
    fun hasApiKey(context: Context): Boolean {
        return !getApiKey(context).isNullOrBlank()
    }
}
