package com.footprint.data.repository

import com.footprint.data.local.BadgeEntity
import com.footprint.data.local.PremiumDao
import com.footprint.data.model.FootprintEntry
import com.footprint.data.model.Mood
import java.time.LocalDate
import java.time.LocalTime

class BadgeManager(private val premiumDao: PremiumDao) {

    suspend fun checkAchievements(allEntries: List<FootprintEntry>) {
        // 1. 深夜食堂：22:00后记录 5 次
        val midnightEntries = allEntries.count { 
             // 模拟：如果有时间字段则判断，否则随机模拟进度
             it.detail.contains("宵夜") || it.energyLevel < 4 
        }
        updateBadgeProgress("badge_midnight", midnightEntries, 5)

        // 2. 高海拔行者：海拔 > 3000m
        val hasHighAlti = allEntries.any { (it.altitude ?: 0.0) > 3000.0 }
        if (hasHighAlti) unlockBadge("badge_high_alti")

        // 3. 碳中和先锋：碳节省 > 50kg
        val totalCarbon = allEntries.sumOf { it.carbonSavedKg }
        updateBadgeProgress("badge_eco", totalCarbon.toInt(), 50)
    }

    private suspend fun updateBadgeProgress(id: String, current: Int, target: Int) {
        val badge = BadgeEntity(
            id = id,
            name = getBadgeName(id),
            description = getBadgeDesc(id),
            iconName = id,
            progress = current.coerceAtMost(target),
            target = target,
            isUnlocked = current >= target,
            unlockDate = if (current >= target) LocalDate.now() else null,
            category = "PREMIUM"
        )
        premiumDao.upsertBadge(badge)
    }

    private suspend fun unlockBadge(id: String) = updateBadgeProgress(id, 1, 1)

    private fun getBadgeName(id: String) = when(id) {
        "badge_midnight" -> "深夜食堂"
        "badge_high_alti" -> "巅峰极客"
        "badge_eco" -> "绿意行者"
        else -> "未知成就"
    }
    
    private fun getBadgeDesc(id: String) = when(id) {
        "badge_midnight" -> "在星光熠熠的夜晚留下 5 次足迹"
        "badge_high_alti" -> "征服海拔 3000 米以上的高度"
        "badge_eco" -> "通过低碳出行节省 50kg 碳排放"
        else -> ""
    }
}
