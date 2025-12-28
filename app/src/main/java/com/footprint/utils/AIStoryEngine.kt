package com.footprint.utils

import com.footprint.data.model.Mood

object AIStoryEngine {
    fun generateDraft(location: String, mood: Mood, tags: List<String>): String {
        val tagStr = tags.joinToString("、")
        return when(mood) {
            Mood.EXCITED -> "在${location}的一次疯狂冒险！这里的${tagStr}让我感到无比兴奋，仿佛世界都在为我欢呼。"
            Mood.RELAXED -> "慢下来，在${location}感受宁静。${tagStr}点缀着这段慢时光，是治愈心灵的良药。"
            Mood.CURIOUS -> "对${location}的好奇心得到了极大满足。每一个${tagStr}背后似乎都藏着一个未被发现的秘密。"
            else -> "记录在${location}的足迹：${tagStr}。生活就是一场不断的抵达。"
        }
    }
}
