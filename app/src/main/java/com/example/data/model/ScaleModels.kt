package com.example.data.model

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.pow

enum class ScaleBrandProtocol(
    val id: String,
    val title: String,
    val subtitle: String,
    val deviceNamePrefixes: List<String>,
    val serviceUuid: String,
    val notifyCharUuid: String,
    val writeCharUuid: String?,
    val tareCommand: ByteArray?
) {
    DECENT(
        id = "decent",
        title = "Decent Scale / Half Decent",
        subtitle = "Open API • Auto-tare, 10 Hz поток",
        deviceNamePrefixes = listOf("Decent Scale", "DECENT"),
        serviceUuid = "0000fff0-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "0000fff4-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "000036f5-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0x03, 0x0F, 0x00, 0x00, 0x00, 0x00, 0x0C)
    ),
    ACAIA(
        id = "acaia",
        title = "Acaia (Lunar / Pearl / Pearl S / Pyxis)",
        subtitle = "Протокол Acaia IPS / PRO • Высокая точность 0.1 г",
        deviceNamePrefixes = listOf("ACAIA", "PROCHBT", "LUNAR", "PEARL", "PYXIS"),
        serviceUuid = "00001820-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "00002a80-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "00002a80-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0xEF.toByte(), 0xDD.toByte(), 0x04, 0x00, 0x00, 0x00)
    ),
    BOOKOO(
        id = "bookoo",
        title = "Bookoo (Themis Mini / Ultra Scale)",
        subtitle = "Ультракомпактные эспрессо-весы Bookoo",
        deviceNamePrefixes = listOf("BOOKOO", "THEMIS"),
        serviceUuid = "00000ffe-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "0000ff11-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "0000ff12-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0x03, 0x0A, 0x01, 0x00, 0x00, 0x08)
    ),
    TIMEMORE(
        id = "timemore",
        title = "Timemore (Black Mirror / Nano / Basic Pro / Dot)",
        subtitle = "Серия весов Timemore Black Mirror BLE",
        deviceNamePrefixes = listOf("Timemore", "TM", "Black Mirror"),
        serviceUuid = "0000181d-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "00002a9d-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "00002a9e-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0x00)
    ),
    DIFLUID(
        id = "difluid",
        title = "DiFluid (Microbalance / Microbalance Ti)",
        subtitle = "Интеграция весов DiFluid Microbalance",
        deviceNamePrefixes = listOf("DiFluid", "Microbalance", "DF-"),
        serviceUuid = "000000ee-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "0000aa01-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "0000aa02-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0xDF.toByte(), 0xDF.toByte(), 0x01, 0x02, 0x01, 0x01)
    ),
    FELICITA(
        id = "felicita",
        title = "Felicita (Arc / Incline / Parallel)",
        subtitle = "Профессиональные кофейные весы Felicita",
        deviceNamePrefixes = listOf("FELICITA"),
        serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "0000ffe1-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "0000ffe1-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0x54) // 'T'
    ),
    EUREKA_VARIA(
        id = "eureka_varia",
        title = "Eureka Precisa / Varia AKU / Hiroia Jimmy",
        subtitle = "Компактные эспрессо-весы BLE",
        deviceNamePrefixes = listOf("CFS-9002", "Eureka", "Varia", "AKU", "HIROIA"),
        serviceUuid = "0000fff0-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "0000fff1-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "0000fff2-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0xAA.toByte(), 0x02, 0x31, 0x31)
    ),
    FUTULA_KOFEZAVR(
        id = "futula_ck811",
        title = "Futula Scale 3 (LFScale CK811) / KP2048B / HOTO",
        subtitle = "Народные весы из экосистемы BeanZen / Кофезавр",
        deviceNamePrefixes = listOf("LFScale", "CK811", "KP2048", "HOTO", "Electronic Scale", "CoffeeScale"),
        serviceUuid = "0000ffb0-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "0000ffb2-0000-1000-8000-00805f9b34fb",
        writeCharUuid = "0000ffb1-0000-1000-8000-00805f9b34fb",
        tareCommand = byteArrayOf(0xFD.toByte(), 0x32, 0x00, 0x00, 0x00, 0x00, 0xCF.toByte())
    ),
    STANDARD_GATT(
        id = "standard_wss",
        title = "Стандартные BLE весы (GATT 0x181D) / DIY ESP32",
        subtitle = "Bluetooth SIG Weight Scale Service (Open-source ESP32 HX711)",
        deviceNamePrefixes = listOf("ESP32", "OpenScale", "BLE Scale", "SmartScale"),
        serviceUuid = "0000181d-0000-1000-8000-00805f9b34fb",
        notifyCharUuid = "00002a9d-0000-1000-8000-00805f9b34fb",
        writeCharUuid = null,
        tareCommand = null
    );

    companion object {
        fun detectFromDeviceName(name: String?): ScaleBrandProtocol {
            if (name.isNullOrBlank()) return FUTULA_KOFEZAVR
            return values().firstOrNull { proto ->
                proto.deviceNamePrefixes.any { prefix ->
                    name.contains(prefix, ignoreCase = true)
                }
            } ?: FUTULA_KOFEZAVR
        }
    }
}

enum class ScaleBrewMode(
    val title: String,
    val subtitle: String,
    val defaultDoseG: Float,
    val defaultTargetWeightG: Float,
    val defaultTargetFlowGps: Float
) {
    POUR_OVER(
        title = "☕ Пуровер / Фильтр",
        subtitle = "Контроль вливаний, скорости струи (4–8 г/с) и коэффициента 1:15–1:17",
        defaultDoseG = 16.0f,
        defaultTargetWeightG = 250.0f,
        defaultTargetFlowGps = 5.5f
    ),
    ESPRESSO(
        title = "⚡ Эспрессо (Shot Profiling)",
        subtitle = "Авто-старт от первой капли, авто-стоп и расчёт Espresso Brew Ratio (1:2–1:2.5)",
        defaultDoseG = 18.0f,
        defaultTargetWeightG = 38.0f,
        defaultTargetFlowGps = 1.6f
    ),
    DRIP_BAG(
        title = "🥡 Дрип-пакет (Drip Bag)",
        subtitle = "3–4 дробных вливания по 45–50 г для дрип-пакета (11 г → 180 г)",
        defaultDoseG = 11.0f,
        defaultTargetWeightG = 180.0f,
        defaultTargetFlowGps = 4.0f
    )
}

data class DiscoveredBleScale(
    val name: String,
    val address: String,
    val rssi: Int,
    val detectedProtocol: ScaleBrandProtocol
)

data class TelemetryPoint(
    val timeSec: Float,
    val weightGrams: Float,
    val flowRateGps: Float
)

object BleScalePacketParser {

    /**
     * Multi-protocol weight packet parser supporting:
     * - Decent Scale (7 or 10 byte packet starting with 0x03, type 0xCE/0xCA)
     * - Bookoo / Acaia / DiFluid / Felicita / Futula CK811 / KP2048B / Standard BLE 0x2A9D
     * - ASCII string weight packets (e.g., "+00124.5g" on Felicita / DIY ESP32 scales)
     */
    fun parseWeightGrams(bytes: ByteArray, protocol: ScaleBrandProtocol): Float? {
        if (bytes.isEmpty()) return null

        // 1. Check ASCII text representation (used by Felicita, DIY ESP32, serial bridges)
        val asciiCandidate = try {
            val str = String(bytes, Charsets.US_ASCII)
            val regex = Regex("""([+-]?\d+\.\d+|[+-]?\d{3,5})""")
            val match = regex.find(str)
            if (match != null && str.any { it.isDigit() } && str.all { it.code in 10..126 }) {
                val raw = match.value.toFloatOrNull()
                if (raw != null) {
                    if (!match.value.contains(".") && raw > 500f) raw / 10f else raw
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
        if (asciiCandidate != null && asciiCandidate in -500f..5000f) {
            return asciiCandidate
        }

        // 2. Decent Scale packet (0x03, 0xCE/0xCA, weightHigh, weightLow, ...)
        if (bytes.size >= 4 && bytes[0] == 0x03.toByte() &&
            (bytes[1] == 0xCE.toByte() || bytes[1] == 0xCA.toByte())
        ) {
            val rawShort = ByteBuffer.wrap(bytes, 2, 2).order(ByteOrder.BIG_ENDIAN).short.toInt()
            return rawShort / 10f
        }

        // 3. Bookoo Scale packet (20 bytes, weight in bytes 7..9, sign in byte 6)
        if (protocol == ScaleBrandProtocol.BOOKOO && bytes.size >= 10) {
            val sign = if (bytes[6].toInt() == 45) -1f else 1f
            val w = ((bytes[7].toInt() and 0xFF) shl 16) or
                ((bytes[8].toInt() and 0xFF) shl 8) or
                (bytes[9].toInt() and 0xFF)
            return sign * (w / 100f)
        }

        // 4. Acaia Lunar / Pearl weight event packet (0xEF, 0xDD, 0x0C, ...)
        if (bytes.size >= 10 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xDD.toByte()) {
            val value = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
            val unitPow = (bytes[9].toInt() and 0xFF).coerceIn(0, 4)
            val divisor = 10.0.pow(unitPow.toDouble()).toFloat()
            val sign = if ((bytes[10.coerceAtMost(bytes.lastIndex)].toInt() and 0x02) != 0) -1f else 1f
            return sign * (value / divisor)
        }

        // 5. Futula Kitchen Scale 3 (CK811) / KP2048B / Generic Chinese BLE kitchen scale
        if (bytes.size >= 6 && (bytes[0] == 0xCF.toByte() || bytes[0] == 0xFD.toByte() || bytes[0] == 0xAA.toByte())) {
            val high = bytes[3].toInt() and 0xFF
            val low = bytes[4].toInt() and 0xFF
            val raw = (high shl 8) or low
            return raw / 10f
        }

        // 6. Standard Bluetooth SIG Weight Measurement (0x2A9D) or fallback 16-bit integer
        if (bytes.size >= 3) {
            val flags = bytes[0].toInt() and 0xFF
            val isImperial = (flags and 0x01) != 0
            val raw = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
            // Standard WSS resolution for SI is 0.005 kg (5 g) or custom 0.1 g on coffee scales
            val grams = if (isImperial) raw * 4.53592f else raw / 10f
            if (grams in -500f..5000f) return grams
        }

        return null
    }
}
