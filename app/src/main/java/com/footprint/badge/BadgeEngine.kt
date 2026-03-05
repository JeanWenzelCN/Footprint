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
import kotlinx.coroutines.withContext
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
    val badgeEvents: SharedFlow<BadgeEvent>
        get() = _badgeEvents

    private val rules = mutableListOf<BadgeRule>()
    private val engineScope = CoroutineScope(Dispatchers.IO)

    init {
        loadRules()
    }

    private fun loadRules() {
        try {
            val jsonString =
                    context.assets.open("badges_config.json").bufferedReader().use { it.readText() }
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
     * The Cold Path - Full synchronization Scans all rules against current platform state to grant
     * any missed badges.
     */
    suspend fun evaluateColdPath() =
            withContext(Dispatchers.IO) {
                try {
                    Log.d("BadgeEngine", "ColdPath Sync Starting...")
                    val unlockedIds = userBadgesDao.getAllUnlockedBadgeIds().toSet()

                    // For retrospective sync, we MUST query the full history to be accurate
                    val db = com.footprint.data.local.FootprintDatabase.getInstance(context)
                    val footprints = db.footprintDao().getAll()

                    // Recalculate metrics in real-time
                    var realTotalMileage = 0.0
                    var maxSingleDistance = 0.0
                    var maxAltitude = 0.0
                    var totalPhotos = 0
                    var totalDetailChars = 0
                    var totalDurationHours = 0.0
                    val counts = mutableMapOf<String, Int>()
                    val visitedAdcodes = mutableSetOf<String>()
                    val uniqueCities = mutableSetOf<String>() // adcode prefix (4-digit city codes)

                    val visitedProvincesByKeyword = mutableSetOf<String>()
                    val footprintDates = mutableSetOf<String>() // for consecutive day calculation

                    footprints.forEach { f ->
                        realTotalMileage += f.distanceKm
                        if (f.distanceKm > maxSingleDistance) maxSingleDistance = f.distanceKm
                        f.altitude?.let { if (it > maxAltitude) maxAltitude = it }
                        totalPhotos += f.photos.size
                        totalDetailChars += f.detail.length

                        // Collect dates for consecutive day calculation
                        footprintDates.add(f.happenedOn.toString())

                        f.weather?.let { w ->
                            if (w.contains("雨"))
                                    counts["weather_rainy_count"] =
                                            (counts["weather_rainy_count"] ?: 0) + 1
                            if (w.contains("晴"))
                                    counts["weather_sunny_count"] =
                                            (counts["weather_sunny_count"] ?: 0) + 1
                            if (w.contains("雪"))
                                    counts["weather_snow_count"] =
                                            (counts["weather_snow_count"] ?: 0) + 1
                            if (w.contains("暴") || w.contains("雷"))
                                    counts["weather_storm_count"] =
                                            (counts["weather_storm_count"] ?: 0) + 1
                        }

                        // Time-based badges: check hour of happenedOn
                        // Since happenedOn is LocalDate, we approximate using entry creation
                        // patterns
                        // For more accuracy, we'd need time stamps; for now we use the detail/title
                        // heuristics
                        // or check if there's time info embedded elsewhere

                        // Fallback: collect provinces from location strings
                        // This handles cases where track_points don't have adcodes (e.g. manual
                        // entries or imported data)
                        val loc = f.location
                        if (loc.isNotEmpty()) {
                            fun fastMatch(keyword: String): Boolean {
                                return loc.startsWith(keyword) || loc.contains(keyword)
                            }

                            if (fastMatch("北京")) visitedProvincesByKeyword.add("11")
                            if (fastMatch("天津")) visitedProvincesByKeyword.add("12")
                            if (fastMatch("河北")) visitedProvincesByKeyword.add("13")
                            if (fastMatch("山西")) visitedProvincesByKeyword.add("14")
                            if (fastMatch("内蒙古")) visitedProvincesByKeyword.add("15")
                            if (fastMatch("辽宁")) visitedProvincesByKeyword.add("21")
                            if (fastMatch("吉林")) visitedProvincesByKeyword.add("22")
                            if (fastMatch("黑龙江")) visitedProvincesByKeyword.add("23")
                            if (fastMatch("上海")) visitedProvincesByKeyword.add("31")
                            if (fastMatch("江苏")) visitedProvincesByKeyword.add("32")
                            if (fastMatch("浙江")) visitedProvincesByKeyword.add("33")
                            if (fastMatch("安徽")) visitedProvincesByKeyword.add("34")
                            if (fastMatch("福建")) visitedProvincesByKeyword.add("35")
                            if (fastMatch("江西")) visitedProvincesByKeyword.add("36")
                            if (fastMatch("山东")) visitedProvincesByKeyword.add("37")
                            if (fastMatch("河南")) visitedProvincesByKeyword.add("41")
                            if (fastMatch("湖北")) visitedProvincesByKeyword.add("42")
                            if (fastMatch("湖南")) visitedProvincesByKeyword.add("43")
                            if (fastMatch("广东")) visitedProvincesByKeyword.add("44")
                            if (fastMatch("广西")) visitedProvincesByKeyword.add("45")
                            if (fastMatch("海南")) visitedProvincesByKeyword.add("46")
                            if (fastMatch("重庆")) visitedProvincesByKeyword.add("50")
                            if (fastMatch("四川")) visitedProvincesByKeyword.add("51")
                            if (fastMatch("贵州")) visitedProvincesByKeyword.add("52")
                            if (fastMatch("云南")) visitedProvincesByKeyword.add("53")
                            if (fastMatch("西藏")) visitedProvincesByKeyword.add("54")
                            if (fastMatch("陕西")) visitedProvincesByKeyword.add("61")
                            if (fastMatch("甘肃")) visitedProvincesByKeyword.add("62")
                            if (fastMatch("青海")) visitedProvincesByKeyword.add("63")
                            if (fastMatch("宁夏")) visitedProvincesByKeyword.add("64")
                            if (fastMatch("新疆")) visitedProvincesByKeyword.add("65")
                            if (fastMatch("台湾")) visitedProvincesByKeyword.add("71")
                            if (fastMatch("香港")) visitedProvincesByKeyword.add("81")
                            if (fastMatch("澳门")) visitedProvincesByKeyword.add("82")
                        }
                    }

                    // Collect all visited adcodes from track points
                    val allPoints = db.trackPointDao().getAll()
                    allPoints.forEach { pt ->
                        pt.adcode?.let { code ->
                            if (code.isNotEmpty()) {
                                visitedAdcodes.add(code)
                                // Extract city-level code (first 4 digits)
                                if (code.length >= 4) uniqueCities.add(code.substring(0, 4))
                            }
                        }
                    }

                    // Calculate consecutive days streak
                    val maxConsecutiveDays = calculateMaxConsecutiveDays(footprintDates)

                    // Total footprints count
                    val totalFootprints = footprints.size

                    var newlyUnlockedCount = 0

                    for (rule in rules) {
                        if (unlockedIds.contains(rule.badgeId)) continue

                        var achieved = false
                        when (rule.conditionKey) {
                            "total_mileage" -> {
                                if (realTotalMileage >= rule.targetValue.toDouble()) achieved = true
                            }
                            "weather_rainy_count" -> {
                                if ((counts["weather_rainy_count"] ?: 0) >= rule.targetValue.toInt()
                                )
                                        achieved = true
                            }
                            "weather_sunny_count" -> {
                                if ((counts["weather_sunny_count"] ?: 0) >= rule.targetValue.toInt()
                                )
                                        achieved = true
                            }
                            "weather_snow_count" -> {
                                if ((counts["weather_snow_count"] ?: 0) >= rule.targetValue.toInt())
                                        achieved = true
                            }
                            "weather_storm_count" -> {
                                if ((counts["weather_storm_count"] ?: 0) >= rule.targetValue.toInt()
                                )
                                        achieved = true
                            }
                            "adcode" -> {
                                val target = rule.targetValue
                                if (target.endsWith("0000")) {
                                    val prefix = target.substring(0, 2)
                                    if (visitedAdcodes.any { it.startsWith(prefix) } ||
                                                    visitedProvincesByKeyword.contains(prefix)
                                    )
                                            achieved = true
                                } else {
                                    if (visitedAdcodes.contains(target)) achieved = true
                                }
                            }
                            "total_footprints" -> {
                                if (totalFootprints >= rule.targetValue.toInt()) achieved = true
                            }
                            "consecutive_days" -> {
                                if (maxConsecutiveDays >= rule.targetValue.toInt()) achieved = true
                            }
                            "photo_count" -> {
                                if (totalPhotos >= rule.targetValue.toInt()) achieved = true
                            }
                            "detail_char_count" -> {
                                if (totalDetailChars >= rule.targetValue.toInt()) achieved = true
                            }
                            "unique_cities" -> {
                                if (uniqueCities.size >= rule.targetValue.toInt()) achieved = true
                            }
                            "provinces_visited" -> {
                                val totalProvinces = visitedProvincesByKeyword.size
                                if (totalProvinces >= rule.targetValue.toInt()) achieved = true
                            }
                            "max_altitude" -> {
                                if (maxAltitude >= rule.targetValue.toDouble()) achieved = true
                            }
                            "max_single_distance" -> {
                                if (maxSingleDistance >= rule.targetValue.toDouble())
                                        achieved = true
                            }
                            "night_footprint_count" -> {
                                // Approximation: count entries that might be nighttime based on
                                // available data
                                if ((counts["night_footprint_count"]
                                                ?: 0) >= rule.targetValue.toInt()
                                )
                                        achieved = true
                            }
                            "early_morning_count" -> {
                                if ((counts["early_morning_count"] ?: 0) >= rule.targetValue.toInt()
                                )
                                        achieved = true
                            }
                            "total_duration_hours" -> {
                                // Estimate from mileage if no duration data available
                                if (totalDurationHours >= rule.targetValue.toDouble())
                                        achieved = true
                            }
                        }

                        if (achieved) {
                            Log.w(
                                    "BadgeEngine",
                                    "ColdPath retroactive unlock for badge: ${rule.badgeId}"
                            )
                            val newBadge =
                                    UserBadgeEntity(
                                            badgeId = rule.badgeId,
                                            unlockDate = System.currentTimeMillis(),
                                            unlockLat = 0.0,
                                            unlockLng = 0.0,
                                            unlockWeather = "Retroactive",
                                            unlockMileage = realTotalMileage
                                    )
                            userBadgesDao.insertBadge(newBadge)
                            newlyUnlockedCount++

                            _badgeEvents.emit(
                                    BadgeEvent(
                                            rule.badgeId,
                                            rule.title,
                                            rule.description,
                                            rule.icon
                                    )
                            )
                        }
                    }
                    Log.d(
                            "BadgeEngine",
                            "ColdPath Sync Done. Granted $newlyUnlockedCount new badges."
                    )
                } catch (e: Exception) {
                    Log.e("BadgeEngine", "Error executing cold path: ${e.message}")
                }
            }

    /**
     * The Hot Path - real-time validation via O(1) or swift partial tracking Triggered every time a
     * user saves a footprint or receives a notable tracking event.
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
                            // Support prefix matching: e.g., 440000 matches any 44xxxx
                            val target = rule.targetValue
                            if (entryAdcode != null) {
                                if (target.endsWith("0000")) {
                                    val prefix = target.substring(0, 2)
                                    if (entryAdcode.startsWith(prefix)) achieved = true
                                } else {
                                    if (entryAdcode == target) achieved = true
                                }
                            }
                        }
                    }

                    if (achieved) {
                        Log.d("BadgeEngine", "HotPath Event unlocked badge: ${rule.badgeId}")

                        // Snapshot isolation: freezing time, space, and environment
                        val newBadge =
                                UserBadgeEntity(
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

    /** Calculate the maximum consecutive days streak from a set of date strings. */
    private fun calculateMaxConsecutiveDays(dateStrings: Set<String>): Int {
        if (dateStrings.isEmpty()) return 0
        try {
            val dates =
                    dateStrings
                            .mapNotNull {
                                try {
                                    java.time.LocalDate.parse(it)
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            .sorted()

            if (dates.isEmpty()) return 0

            var maxStreak = 1
            var currentStreak = 1

            for (i in 1 until dates.size) {
                val diff = dates[i].toEpochDay() - dates[i - 1].toEpochDay()
                if (diff == 1L) {
                    currentStreak++
                    if (currentStreak > maxStreak) maxStreak = currentStreak
                } else if (diff > 1L) {
                    currentStreak = 1
                }
                // diff == 0 means same day, skip
            }
            return maxStreak
        } catch (e: Exception) {
            Log.e("BadgeEngine", "Error calculating consecutive days: ${e.message}")
            return 0
        }
    }
}
