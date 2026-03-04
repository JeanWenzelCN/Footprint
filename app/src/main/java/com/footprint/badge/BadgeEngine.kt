package com.footprint.badge

import android.content.Context
import android.util.Log
import com.footprint.data.local.UserBadgeEntity
import com.footprint.data.local.UserBadgesDao
import com.footprint.data.local.UserStatsDao
import com.footprint.data.model.FootprintEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class BadgeEvent(
    val badgeId: String,
    val title: String,
    val description: String,
    val icon: String
)

data class BadgeRule(
    val badgeId: String,
    val title: String,
    val description: String,
    val category: String,
    val conditionKey: String,
    val targetValue: String,
    val icon: String
)

class BadgeEngine(
    private val context: Context,
    private val userBadgesDao: UserBadgesDao,
    private val userStatsDao: UserStatsDao
) {
    // SharedFlow allows for replay = 1 to retain the event if UI is currently re-creating
    private val _badgeEvents = MutableSharedFlow<BadgeEvent>(replay = 1)
    val badgeEvents: SharedFlow<BadgeEvent> get() = _badgeEvents

    private val rules = mutableListOf<BadgeRule>()
    private val engineScope = CoroutineScope(Dispatchers.IO)

    init {
        loadRules()
    }

    private fun loadRules() {
        try {
            val jsonString = context.assets.open("badges_config.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val visualMeta = obj.getJSONObject("visual_meta")
                rules.add(
                    BadgeRule(
                        badgeId = obj.getString("badge_id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        category = obj.getString("category"),
                        conditionKey = obj.getString("condition_key"),
                        targetValue = obj.getString("target_value"),
                        icon = visualMeta.getString("icon")
                    )
                )
            }
            Log.d("BadgeEngine", "Loaded ${rules.size} badge rules from dictionary.")
        } catch (e: Exception) {
            Log.e("BadgeEngine", "Failed to load badges dictionary: ${e.message}")
        }
    }

    /**
     * The Hot Path - real-time validation via O(1) or swift partial tracking
     * Triggered every time a user saves a footprint or receives a notable tracking event.
     */
    fun evaluateHotPath(entry: FootprintEntry, entryAdcode: String? = null) {
        engineScope.launch {
            try {
                val unlockedIds = userBadgesDao.getAllUnlockedBadgeIds().toSet()
                val stats = userStatsDao.getUserStats() ?: return@launch

                for (rule in rules) {
                    // Skip if already unlocked
                    if (unlockedIds.contains(rule.badgeId)) continue

                    var achieved = false
                    
                    // The core referee checking logic
                    when (rule.conditionKey) {
                        "total_mileage" -> {
                            // Check if current total mileage satisfies target
                            if (stats.totalMileage >= rule.targetValue.toDouble()) {
                                achieved = true
                            }
                        }
                        "adcode" -> {
                            // Offline geographic check
                            if (entryAdcode == rule.targetValue) {
                                achieved = true
                            }
                        }
                    }

                    if (achieved) {
                        Log.d("BadgeEngine", "HotPath Event unlocked badge: ${rule.badgeId}")
                        
                        // Snapshot isolation: freezing time, space, and environment
                        val newBadge = UserBadgeEntity(
                            badgeId = rule.badgeId,
                            unlockDate = System.currentTimeMillis(),
                            unlockLat = entry.latitude,
                            unlockLng = entry.longitude,
                            unlockWeather = entry.weather,
                            unlockMileage = stats.totalMileage
                        )
                        userBadgesDao.insertBadge(newBadge)
                        
                        // Emit the joy!
                        _badgeEvents.emit(
                            BadgeEvent(
                                badgeId = rule.badgeId,
                                title = rule.title,
                                description = rule.description,
                                icon = rule.icon
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("BadgeEngine", "Error executing hot path evaluation: ${e.message}")
            }
        }
    }
}
