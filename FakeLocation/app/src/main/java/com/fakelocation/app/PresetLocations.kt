package com.fakelocation.app

/**
 * 预设地点。
 */
data class PresetLocation(
    val name: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double,
    val description: String
)

/**
 * 常用预设地点列表。
 */
object PresetLocations {
    val presets = listOf(
        PresetLocation(
            "北京·天安门",
            "Tiananmen Square",
            39.9087,
            116.3975,
            "天安门广场，北京中心"
        ),
        PresetLocation(
            "上海·外滩",
            "The Bund",
            31.2397,
            121.4908,
            "上海外滩，黄浦江畔"
        ),
        PresetLocation(
            "纽约·时代广场",
            "Times Square",
            40.7580,
            -73.9855,
            "纽约时代广场"
        ),
        PresetLocation(
            "东京塔",
            "Tokyo Tower",
            35.6586,
            139.7454,
            "东京塔"
        ),
        PresetLocation(
            "悉尼歌剧院",
            "Sydney Opera House",
            -33.8568,
            151.2153,
            "澳大利亚悉尼歌剧院"
        ),
        PresetLocation(
            "巴黎·埃菲尔铁塔",
            "Eiffel Tower",
            48.8584,
            2.2945,
            "法国巴黎埃菲尔铁塔"
        ),
        PresetLocation(
            "伦敦·大本钟",
            "Big Ben",
            51.5007,
            -0.1246,
            "英国伦敦大本钟"
        ),
        PresetLocation(
            "迪拜·哈利法塔",
            "Burj Khalifa",
            25.1972,
            55.2744,
            "阿联酋迪拜哈利法塔"
        )
    )
}
