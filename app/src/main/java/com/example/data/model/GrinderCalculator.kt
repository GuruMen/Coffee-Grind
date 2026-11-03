package com.example.data.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GrinderCalculator {

    fun micronsToPosition(um: Int, grinder: Grinder): Float {
        val pts = grinder.anchors
        if (pts.isEmpty()) return 0f
        if (um <= pts.first().first) return pts.first().second
        if (um >= pts.last().first) return pts.last().second

        for (i in 0 until pts.size - 1) {
            val (u0, p0) = pts[i]
            val (u1, p1) = pts[i + 1]
            if (um in u0..u1) {
                if (u1 == u0) return p0
                return p0 + ((um - u0).toFloat() / (u1 - u0)) * (p1 - p0)
            }
        }
        return pts.last().second
    }

    fun positionToMicrons(pos: Float, grinder: Grinder): Int {
        val pts = grinder.anchors
        if (pts.isEmpty()) return grinder.minUm
        val rev = pts.first().second > pts.last().second

        if (!rev) {
            if (pos <= pts.first().second) return pts.first().first
            if (pos >= pts.last().second) return pts.last().first
        } else {
            if (pos >= pts.first().second) return pts.first().first
            if (pos <= pts.last().second) return pts.last().first
        }

        for (i in 0 until pts.size - 1) {
            val (u0, p0) = pts[i]
            val (u1, p1) = pts[i + 1]
            val match = if (!rev) pos in p0..p1 else (pos <= p0 && pos >= p1)
            if (match) {
                if (p1 == p0) return u0
                return (u0 + ((pos - p0) / (p1 - p0)) * (u1 - u0)).roundToInt()
            }
        }
        return pts.last().first
    }

    fun formatPosition(pos: Float, grinder: Grinder, zeroOffset: Float = 0f): String {
        val lo = min(grinder.minPos, grinder.maxPos)
        val hi = max(grinder.minPos, grinder.maxPos)
        val adjustedPos = (pos + zeroOffset).coerceIn(lo, hi)

        val fmt = grinder.fmt
        val step = if (grinder.step > 0f) grinder.step else 1f
        val keepZero = grinder.keepZero

        when (fmt) {
            "bodum_dots" -> {
                val c = adjustedPos.roundToInt()
                return when {
                    c <= 1 -> "FINE"
                    c == 6 -> "MEDIUM"
                    c >= 12 -> "COARSE"
                    else -> c.toString()
                }
            }
            "lido_og" -> {
                val u = ((adjustedPos / 5f).roundToInt() * 5)
                val macro = ((u / 100f).roundToInt() * 100)
                val diff = u - macro
                return when {
                    diff == 0 -> macro.toString()
                    diff > 0 -> "$macro+$diff"
                    else -> "$macro$diff"
                }
            }
            "niche_90" -> {
                val c = adjustedPos.roundToInt()
                val rot = c / 90
                val rem = c % 90
                return "$rot.${rem.toString().padStart(2, '0')}"
            }
            "num_3tick" -> {
                val c = adjustedPos.roundToInt()
                val w = c / 3
                val t = c % 3
                return if (t == 0 && !keepZero) "$w" else "$w.$t"
            }
            "num_4tick" -> {
                val c = adjustedPos.roundToInt()
                val w = c / 4
                val t = c % 4
                return if (t == 0) (if (keepZero) "$w.0" else "$w") else "$w.$t"
            }
            "mhw_12_5" -> {
                val c = adjustedPos.roundToInt()
                val rot = c / 60
                val rem = c % 60
                val num = rem / 5
                val tick = rem % 5
                val numStr = if (tick == 0) "$num" else "$num.$tick"
                return if (rot > 0) "$rot+$numStr" else numStr
            }
            "acaia_10" -> {
                val value = (adjustedPos * 10f).roundToInt() / 10f
                val rot = floor(value / 10f + 1e-9f).toInt()
                val rem = max(0f, value - rot * 10f)
                val remStr = String.format(Locale.US, "%.1f", rem)
                return if (rot > 0) "$rot+$remStr" else remStr
            }
            "eureka_6" -> {
                val value = (adjustedPos * 2f).roundToInt() / 2f
                val rot = floor(value / 6f + 1e-9f).toInt()
                val rem = max(0f, value - rot * 6f)
                val remStr = if (abs(rem - rem.roundToInt()) < 1e-6) rem.roundToInt().toString() else String.format(Locale.US, "%.1f", rem)
                return if (rot > 0) "$rot+$remStr" else remStr
            }
            "cafflano_8" -> {
                val c = max(1, adjustedPos.roundToInt())
                val rot = (c - 1) / 8
                val rem = ((c - 1) % 8) + 1
                return if (rot > 0) "$rot+$rem" else rem.toString()
            }
            "1z_10_3", "1z_10_4", "1z_10_5", "1z_9_10", "1z_10_10", "1z_6_10", "1z_5_10" -> {
                val (nums, dots) = when (fmt) {
                    "1z_10_3" -> 10 to 3
                    "1z_10_4" -> 10 to 4
                    "1z_10_5" -> 10 to 5
                    "1z_9_10" -> 9 to 10
                    "1z_10_10" -> 10 to 10
                    "1z_6_10" -> 6 to 10
                    "1z_5_10" -> 5 to 10
                    else -> 10 to 10
                }
                val cpr = nums * dots
                val c = adjustedPos.roundToInt()
                val rot = c / cpr
                val rem = c % cpr
                val num = rem / dots
                val dot = rem % dots
                return "$rot.$num.$dot"
            }
            "1z_6_10_2part" -> {
                val c = adjustedPos.roundToInt()
                val rot = c / 60
                val rem = c % 60
                return "$rot.${rem.toString().padStart(2, '0')}"
            }
            "plus_70" -> {
                val c = adjustedPos.roundToInt()
                val rot = c / 70
                val rem = c % 70
                return if (rot > 0) "$rot+$rem" else rem.toString()
            }
            "slash_60" -> {
                val c = adjustedPos.roundToInt()
                val rot = c / 60
                val rem = c % 60
                return "$rot/$rem"
            }
            "baratza_az", "baratza_aw", "baratza_av", "baratza_ak" -> {
                val st = when (fmt) {
                    "baratza_az" -> 26
                    "baratza_aw" -> 23
                    "baratza_av" -> 22
                    "baratza_ak" -> 11
                    else -> 26
                }
                val c = max(0, adjustedPos.roundToInt())
                val macro = (c / st) + 1
                val micro = ('A'.code + (c % st)).toChar()
                return "$macro$micro"
            }
            "knock_12", "knock_14" -> {
                val cpr = if (fmt == "knock_14") 14 else 12
                val c = adjustedPos.roundToInt()
                val rot = c / cpr
                val num = c % cpr
                return "$rot.$num"
            }
            "knock_12_3", "helor_12_10" -> {
                val sub = if (fmt == "helor_12_10") 10 else 3
                val c = adjustedPos.roundToInt()
                val rot = c / (12 * sub)
                val rem = c % (12 * sub)
                val num = rem / sub
                val tick = rem % sub
                return "$rot.$num.$tick"
            }
            "weber_key" -> {
                val c = (adjustedPos / 2f).roundToInt() * 2
                val rot = c / 150
                val rem = c % 150
                val num = rem / 10
                val sub = rem % 10
                return "$rot.$num.$sub"
            }
            "num_plus" -> {
                val value = (adjustedPos / step).roundToInt() * step
                val s = if (abs(value - value.roundToInt()) < 1e-6) value.roundToInt().toString() else String.format(Locale.US, "%.1f", value)
                return "+$s"
            }
            "rev_8", "rev_70" -> {
                return adjustedPos.roundToInt().toString()
            }
            else -> {
                val value = (adjustedPos / step).roundToInt() * step
                return if (step >= 1f) {
                    value.roundToInt().toString()
                } else if (abs(value - value.roundToInt()) < 1e-6) {
                    if (keepZero) "${value.roundToInt()}.0" else value.roundToInt().toString()
                } else {
                    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
                }
            }
        }
    }

    fun formatRangeWithOffset(mr: MethodRange?, grinder: Grinder, zeroOffset: Float = 0f): String {
        if (mr == null) return "Н/Д"
        if (abs(zeroOffset) < 1e-6f) return mr.str
        val sLow = formatPosition(mr.lowPos, grinder, zeroOffset)
        val sHigh = formatPosition(mr.highPos, grinder, zeroOffset)
        return if (sLow == sHigh) sLow else "$sLow – $sHigh"
    }

    fun formatSweetSpotWithOffset(mr: MethodRange?, grinder: Grinder, zeroOffset: Float = 0f): String {
        if (mr == null) return "Н/Д"
        if (abs(zeroOffset) < 1e-6f) return mr.midStr
        return formatPosition(mr.midPos, grinder, zeroOffset)
    }
}
