package com.footprint.utils

import android.content.Context
import androidx.core.content.edit

object ApiKeyManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_AMAP_API_KEY = "amap_api_key"
    private const val KEY_GOOGLE_API_KEY = "google_api_key"
    private const val KEY_SELECTED_MAP_TYPE = "selected_map_type"

    fun getApiKey(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AMAP_API_KEY, null)
    }

    fun setApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_AMAP_API_KEY, apiKey)
        }
    }

    fun getGoogleApiKey(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GOOGLE_API_KEY, null)
    }

    fun setGoogleApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_GOOGLE_API_KEY, apiKey)
        }
    }

    fun getSelectedMapType(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_MAP_TYPE, "AMAP") ?: "AMAP"
    }

    fun setSelectedMapType(context: Context, type: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_SELECTED_MAP_TYPE, type)
        }
    }
    
    fun hasApiKey(context: Context): Boolean {
        val selected = getSelectedMapType(context)
        return if (selected == "GOOGLE") {
            !getGoogleApiKey(context).isNullOrBlank()
        } else {
            !getApiKey(context).isNullOrBlank()
        }
    }
}
