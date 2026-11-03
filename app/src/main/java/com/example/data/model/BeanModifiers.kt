package com.example.data.model

enum class BeanProcess(val label: String, val deltaUm: Int) {
    WASHED("Мытая (0 μm)", 0),
    NATURAL("Натуральная / Хани (+20 μm)", 20),
    ANAEROBIC("Анаэробная / Инфьюз (+40 μm)", 40)
}

enum class RoastLevel(val label: String, val deltaUm: Int) {
    LIGHT("Светлая (-35 μm)", -35),
    MEDIUM("Средняя (0 μm)", 0),
    DARK("Тёмная (+40 μm)", 40)
}

enum class Altitude(val label: String, val deltaUm: Int) {
    LOW("<1400 м (+15 μm)", 15),
    MEDIUM("1400–1800 м (0)", 0),
    HIGH(">1800 м SHG (-15 μm)", -15)
}

data class BeanModifiersState(
    val process: BeanProcess = BeanProcess.WASHED,
    val roast: RoastLevel = RoastLevel.MEDIUM,
    val altitude: Altitude = Altitude.MEDIUM,
    val isDecaf: Boolean = false
) {
    val totalMicronOffset: Int
        get() = process.deltaUm + roast.deltaUm + altitude.deltaUm + (if (isDecaf) 35 else 0)
}

data class WaterTempRecommendation(
    val minTemp: Int,
    val maxTemp: Int,
    val optimalTemp: Int,
    val displayLabel: String,
    val reason: String
)

object WaterTempCalculator {
    fun calculate(methodId: String, state: BeanModifiersState): WaterTempRecommendation {
        if (methodId == "cold_brew" || methodId == "cold_drip") {
            return WaterTempRecommendation(
                minTemp = 4,
                maxTemp = 20,
                optimalTemp = 15,
                displayLabel = "4–20 °C (холодная вода / лёд)",
                reason = "Холодная экстракция"
            )
        }

        var base = when (methodId) {
            "v60", "pour_over", "filter_machine", "siphon" -> 93
            "steep_and_release" -> 92
            "aeropress" -> 89
            "espresso" -> 93
            "french_press", "cupping" -> 94
            "turkish", "moka_pot" -> 90
            else -> 92
        }

        when (state.roast) {
            RoastLevel.LIGHT -> base += 2
            RoastLevel.DARK -> base -= 5
            RoastLevel.MEDIUM -> {}
        }

        when (state.process) {
            BeanProcess.ANAEROBIC -> base -= 2
            BeanProcess.NATURAL -> base -= 1
            BeanProcess.WASHED -> {}
        }

        if (state.isDecaf) base -= 3

        base = base.coerceIn(78, 98)
        val minT = (base - 1).coerceAtLeast(75)
        val maxT = (base + 1).coerceAtMost(99)

        val roastName = when (state.roast) {
            RoastLevel.LIGHT -> "Светлая обжарка"
            RoastLevel.MEDIUM -> "Средняя обжарка"
            RoastLevel.DARK -> "Тёмная обжарка"
        }
        val procName = when (state.process) {
            BeanProcess.WASHED -> "Мытая"
            BeanProcess.NATURAL -> "Натуральная / Хани"
            BeanProcess.ANAEROBIC -> "Анаэробная / Инфьюз"
        }
        val decafTag = if (state.isDecaf) " • Декаф" else ""

        return WaterTempRecommendation(
            minTemp = minT,
            maxTemp = maxT,
            optimalTemp = base,
            displayLabel = "$minT–$maxT °C (оптимум $base °C)",
            reason = "$roastName • $procName$decafTag"
        )
    }
}
