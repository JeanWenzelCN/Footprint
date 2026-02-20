package com.footprint.ui.components.weather

import androidx.compose.ui.graphics.Color

enum class WeatherCategory(val title: String) {
    CLEAR_CLOUDY("🌤️ 晴云"),
    RAIN("🌧️ 降雨"),
    SNOW_ICE("❄️ 降雪"),
    OBSCURATION("🌫️ 视障"),
    EXTREME("⚡ 极端")
}

enum class BaseShader {
    SUNNY,
    CLOUDY,
    RAIN,
    THUNDERSTORM,
    SNOW,
    FOG
}

enum class WeatherType(
        val category: WeatherCategory,
        val color: Color,
        val label: String,
        val baseShader: BaseShader
) {
    // 🌤️ 第一类：晴与云系 (Clear & Cloudy)
    CLEAR(WeatherCategory.CLEAR_CLOUDY, Color(0xFFFFCA28), "晴空", BaseShader.SUNNY),
    FEW_CLOUDS(WeatherCategory.CLEAR_CLOUDY, Color(0xFFFFB300), "少云", BaseShader.SUNNY),
    PARTLY_CLOUDY(WeatherCategory.CLEAR_CLOUDY, Color(0xFFB0BEC5), "多云", BaseShader.CLOUDY),
    OVERCAST(WeatherCategory.CLEAR_CLOUDY, Color(0xFF78909C), "阴天", BaseShader.CLOUDY),

    // 🌧️ 第二类：降雨系 (Rain)
    DRIZZLE(WeatherCategory.RAIN, Color(0xFF90CAF9), "毛毛雨", BaseShader.RAIN),
    SHOWERS(WeatherCategory.RAIN, Color(0xFF64B5F6), "阵雨", BaseShader.RAIN),
    LIGHT_RAIN(WeatherCategory.RAIN, Color(0xFF42A5F5), "小雨", BaseShader.RAIN),
    MODERATE_RAIN(WeatherCategory.RAIN, Color(0xFF1E88E5), "中雨", BaseShader.RAIN),
    HEAVY_RAIN(WeatherCategory.RAIN, Color(0xFF1976D2), "大雨", BaseShader.RAIN),
    RAINSTORM(WeatherCategory.RAIN, Color(0xFF0D47A1), "暴雨", BaseShader.RAIN),
    FREEZING_RAIN(WeatherCategory.RAIN, Color(0xFF00ACC1), "冻雨", BaseShader.RAIN),

    // ❄️ 第三类：降雪与冰相系 (Snow & Ice)
    SNOW_FLURRIES(WeatherCategory.SNOW_ICE, Color(0xFFE1F5FE), "阵雪", BaseShader.SNOW),
    LIGHT_SNOW(WeatherCategory.SNOW_ICE, Color(0xFFB3E5FC), "小雪", BaseShader.SNOW),
    MODERATE_SNOW(WeatherCategory.SNOW_ICE, Color(0xFF81D4FA), "中雪", BaseShader.SNOW),
    HEAVY_SNOW(WeatherCategory.SNOW_ICE, Color(0xFF29B6F6), "大雪", BaseShader.SNOW),
    BLIZZARD(WeatherCategory.SNOW_ICE, Color(0xFF0288D1), "暴雪", BaseShader.SNOW),
    SLEET(WeatherCategory.SNOW_ICE, Color(0xFF4FC3F7), "雨夹雪", BaseShader.SNOW),
    HAIL(WeatherCategory.SNOW_ICE, Color(0xFF0097A7), "冰雹", BaseShader.SNOW),

    // 🌫️ 第四类：视程障碍系 (Atmospheric Obscurations)
    MIST(WeatherCategory.OBSCURATION, Color(0xFFCFD8DC), "薄雾", BaseShader.FOG),
    FOG(WeatherCategory.OBSCURATION, Color(0xFF90A4AE), "大雾", BaseShader.FOG),
    HAZE(WeatherCategory.OBSCURATION, Color(0xFFA1887F), "雾霾", BaseShader.FOG),
    DUST(WeatherCategory.OBSCURATION, Color(0xFFBCAAA4), "浮尘", BaseShader.FOG),
    BLOWING_DUST(WeatherCategory.OBSCURATION, Color(0xFF8D6E63), "扬沙", BaseShader.FOG),
    SANDSTORM(WeatherCategory.OBSCURATION, Color(0xFF4E342E), "沙尘暴", BaseShader.FOG),

    // ⚡ 第五类：强对流与极端天气 (Extreme & Storms)
    THUNDERSTORM(WeatherCategory.EXTREME, Color(0xFF7E57C2), "雷阵雨", BaseShader.THUNDERSTORM),
    SEVERE_THUNDERSTORM(WeatherCategory.EXTREME, Color(0xFF4527A0), "强雷暴", BaseShader.THUNDERSTORM),
    GALE(WeatherCategory.EXTREME, Color(0xFF546E7A), "狂风", BaseShader.FOG),
    TYPHOON(WeatherCategory.EXTREME, Color(0xFF37474F), "台风", BaseShader.THUNDERSTORM),
    TORNADO(WeatherCategory.EXTREME, Color(0xFF263238), "龙卷风", BaseShader.THUNDERSTORM)
}
