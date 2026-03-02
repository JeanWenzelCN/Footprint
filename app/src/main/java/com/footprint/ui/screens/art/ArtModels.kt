package com.footprint.ui.screens.art

enum class ArtMapStyle {
    DARK,
    LIGHT,
    SATELLITE,
    VOID
}

enum class ArtLayout {
    FULLCREEN_A24,
    POLAROID,
    GEEK_STATS
}

enum class PolaroidFrameStyle {
    CLASSIC_WHITE, // 极简纯白 + 微弱纸张噪点
    CLASSIC_BLACK, // 纯黑 (原暗房黑)
    ACOUSTIC_WOOD, // 原木刻录 (Acoustic Wood)
    HEAVY_MECHANICAL, // 重装机甲 (Heavy Mechanical)
    CYBER_GLITCH // 赛博霓虹 (Cyber Glitch)
}

enum class WoodType {
    ASH, // 白蜡木 (明亮)
    WALNUT, // 胡桃木 (深邃)
    VINTAGE_OAK // 做旧橡木 (裂纹)
}

enum class ArmorType {
    GUNMETAL, // 拉丝黑钛
    CARBON_FIBER, // 碳纤维网格
    WORN_OLIVE // 战损哑光绿
}
