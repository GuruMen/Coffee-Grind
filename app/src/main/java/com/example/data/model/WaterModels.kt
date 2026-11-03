package com.example.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class WaterProfilePreset(
    val id: String,
    val name: String,
    val gh: Int,
    val mgRatio: Int,
    val kh: Int
)

data class WaterMineralResult(
    val totalTds: Int,
    val mgPpm: Int,
    val caPpm: Int,
    val lotusMgDrops: Float,
    val lotusCaDrops: Float,
    val lotusKhDrops: Float,
    val stockMgMl: Float,
    val stockCaMl: Float,
    val stockKhMl: Float,
    val inScaWindow: Boolean
)

data class WaterBlendResult(
    val volA: Int,
    val volB: Int,
    val pctA: Int,
    val pctB: Int,
    val isValid: Boolean,
    val message: String = ""
)

object WaterCalculator {
    val PRESETS = listOf(
        WaterProfilePreset("sca_balanced", "Стандарт SCA (GH 68 / KH 40 • Сбалансированный)", 68, 60, 40),
        WaterProfilePreset("light_bright", "Light & Bright (GH 60 / KH 25 • Яркая кислотность)", 60, 75, 25),
        WaterProfilePreset("rao_perger", "Rao / Perger (GH 85 / KH 40 • Универсальный фильтр)", 85, 65, 40),
        WaterProfilePreset("sweet_switch", "Hario Switch Sweet (GH 75 / KH 45 • Плотный и сладкий)", 75, 45, 45),
        WaterProfilePreset("espresso_safe", "Espresso Pavlis / Langelier (GH 55 / KH 45 • Без накипи)", 55, 50, 45)
    )

    fun calculateMinerals(volMl: Float, gh: Int, mgRatio: Int, kh: Int): WaterMineralResult {
        val totalTds = gh + kh
        val mgPpm = (gh * (mgRatio / 100f)).roundToInt()
        val caPpm = max(0, gh - mgPpm)
        val factorL = volMl / 1000f

        val lotusMgDrops = (mgPpm / 5f) * (volMl / 450f)
        val lotusCaDrops = (caPpm / 5f) * (volMl / 450f)
        val lotusKhDrops = (kh / 5f) * (volMl / 450f)

        val stockMgMl = mgPpm * factorL
        val stockCaMl = caPpm * factorL
        val stockKhMl = kh * factorL

        val inSca = gh in 50..175 && kh in 35..75

        return WaterMineralResult(
            totalTds = totalTds,
            mgPpm = mgPpm,
            caPpm = caPpm,
            lotusMgDrops = (lotusMgDrops * 10f).roundToInt() / 10f,
            lotusCaDrops = (lotusCaDrops * 10f).roundToInt() / 10f,
            lotusKhDrops = (lotusKhDrops * 10f).roundToInt() / 10f,
            stockMgMl = (stockMgMl * 10f).roundToInt() / 10f,
            stockCaMl = (stockCaMl * 10f).roundToInt() / 10f,
            stockKhMl = (stockKhMl * 10f).roundToInt() / 10f,
            inScaWindow = inSca
        )
    }

    fun calculateBlend(tdsA: Float, tdsB: Float, targetTds: Float, totalVol: Float): WaterBlendResult {
        val lo = min(tdsA, tdsB)
        val hi = max(tdsA, tdsB)
        if (abs(tdsB - tdsA) < 1e-4f || targetTds < lo || targetTds > hi) {
            return WaterBlendResult(
                volA = 0,
                volB = 0,
                pctA = 0,
                pctB = 0,
                isValid = false,
                message = "Целевой TDS (${targetTds.roundToInt()} мг/л) должен находиться между водой А (${tdsA.roundToInt()}) и водой Б (${tdsB.roundToInt()})"
            )
        }
        val ratioB = (targetTds - tdsA) / (tdsB - tdsA)
        val volB = (totalVol * ratioB).roundToInt()
        val volA = (totalVol - volB).roundToInt()
        val pctB = (ratioB * 100f).roundToInt()
        val pctA = 100 - pctB

        return WaterBlendResult(
            volA = volA,
            volB = volB,
            pctA = pctA,
            pctB = pctB,
            isValid = true
        )
    }
}
