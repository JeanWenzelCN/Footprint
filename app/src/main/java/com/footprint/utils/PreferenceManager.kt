package com.footprint.utils

import android.content.Context
import android.content.SharedPreferences
import com.footprint.ui.theme.ThemeMode

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
            context.getSharedPreferences("footprint_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString("theme_mode", value.name).apply()
        }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()

    var nickname: String
        get() = prefs.getString("user_nickname", "旅行者") ?: "旅行者"
        set(value) = prefs.edit().putString("user_nickname", value).apply()

    var avatarId: String
        get() = prefs.getString("user_avatar_id", "avatar_1") ?: "avatar_1"
        set(value) = prefs.edit().putString("user_avatar_id", value).apply()

    var themeStyle: com.footprint.ui.theme.AppThemeStyle
        get() {
            val name =
                    prefs.getString(
                            "theme_style",
                            com.footprint.ui.theme.AppThemeStyle.CLASSIC.name
                    )
            return try {
                com.footprint.ui.theme.AppThemeStyle.valueOf(
                        name ?: com.footprint.ui.theme.AppThemeStyle.CLASSIC.name
                )
            } catch (e: Exception) {
                com.footprint.ui.theme.AppThemeStyle.CLASSIC
            }
        }
        set(value) {
            prefs.edit().putString("theme_style", value.name).apply()
        }

    var hasSeededV5: Boolean
        get() = prefs.getBoolean("has_seeded_v5", false)
        set(value) = prefs.edit().putBoolean("has_seeded_v5", value).apply()

    var blurStrength: Float
        get() = prefs.getFloat("blur_strength", 16f)
        set(value) = prefs.edit().putFloat("blur_strength", value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    var artAuthorName: String
        get() = prefs.getString("art_author_name", "漂泊的灵魂") ?: "漂泊的灵魂"
        set(value) = prefs.edit().putString("art_author_name", value).apply()

    var artFontName: String
        get() = prefs.getString("art_font_name", "Default") ?: "Default"
        set(value) = prefs.edit().putString("art_font_name", value).apply()

    var artColorStyle: String
        get() = prefs.getString("art_color_style", "Neon Green") ?: "Neon Green"
        set(value) = prefs.edit().putString("art_color_style", value).apply()

    var artTextColor: String
        get() = prefs.getString("art_text_color", "White") ?: "White"
        set(value) = prefs.edit().putString("art_text_color", value).apply()

    var artTextItalic: Boolean
        get() = prefs.getBoolean("art_text_italic", false)
        set(value) = prefs.edit().putBoolean("art_text_italic", value).apply()

    var artTextBorder: Boolean
        get() = prefs.getBoolean("art_text_border", false)
        set(value) = prefs.edit().putBoolean("art_text_border", value).apply()

    var polaroidFrameStyle: String
        get() = prefs.getString("polaroid_frame_style", "CLASSIC_WHITE") ?: "CLASSIC_WHITE"
        set(value) = prefs.edit().putString("polaroid_frame_style", value).apply()

    var polaroidFramePadding: Float
        get() = prefs.getFloat("polaroid_frame_padding", 0.5f)
        set(value) = prefs.edit().putFloat("polaroid_frame_padding", value).apply()

    var polaroidInnerBorder: Float
        get() = prefs.getFloat("polaroid_inner_border", 1f)
        set(value) = prefs.edit().putFloat("polaroid_inner_border", value).apply()

    var woodType: com.footprint.ui.screens.art.WoodType
        get() {
            val name = prefs.getString("wood_type", com.footprint.ui.screens.art.WoodType.ASH.name)
            return try {
                com.footprint.ui.screens.art.WoodType.valueOf(
                        name ?: com.footprint.ui.screens.art.WoodType.ASH.name
                )
            } catch (e: Exception) {
                com.footprint.ui.screens.art.WoodType.ASH
            }
        }
        set(value) = prefs.edit().putString("wood_type", value.name).apply()

    var engravingDepth: Float
        get() = prefs.getFloat("engraving_depth", 0.5f)
        set(value) = prefs.edit().putFloat("engraving_depth", value).apply()

    var canvasGrain: Float
        get() = prefs.getFloat("canvas_grain", 0.3f)
        set(value) = prefs.edit().putFloat("canvas_grain", value).apply()

    var armorType: com.footprint.ui.screens.art.ArmorType
        get() {
            val name =
                    prefs.getString(
                            "armor_type",
                            com.footprint.ui.screens.art.ArmorType.GUNMETAL.name
                    )
            return try {
                com.footprint.ui.screens.art.ArmorType.valueOf(
                        name ?: com.footprint.ui.screens.art.ArmorType.GUNMETAL.name
                )
            } catch (e: Exception) {
                com.footprint.ui.screens.art.ArmorType.GUNMETAL
            }
        }
        set(value) = prefs.edit().putString("armor_type", value.name).apply()

    var mechanicalSeams: Float
        get() = prefs.getFloat("mechanical_seams", 0.5f)
        set(value) = prefs.edit().putFloat("mechanical_seams", value).apply()

    var hasHazardStriping: Boolean
        get() = prefs.getBoolean("has_hazard_striping", false)
        set(value) = prefs.edit().putBoolean("has_hazard_striping", value).apply()

    var frostRadius: Float
        get() = prefs.getFloat("frost_radius", 20f)
        set(value) = prefs.edit().putFloat("frost_radius", value).apply()

    var chromaticAberration: Float
        get() = prefs.getFloat("chromatic_aberration", 0.5f)
        set(value) = prefs.edit().putFloat("chromatic_aberration", value).apply()

    var glassTint: String
        get() = prefs.getString("glass_tint", "Clear") ?: "Clear"
        set(value) = prefs.edit().putString("glass_tint", value).apply()
}
