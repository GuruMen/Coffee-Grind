package com.example

import com.example.data.local.CoffeeBeanEntity
import com.example.data.model.CoffeeOriginsHelper
import com.example.data.model.Grinder
import com.example.data.model.GrinderCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExampleUnitTest {

    private fun createDummyGrinder(
        fmt: String = "num",
        step: Float = 1f,
        minUm: Int = 0,
        maxUm: Int = 1000,
        minPos: Float = 0f,
        maxPos: Float = 40f
    ): Grinder {
        return Grinder(
            idx = 0,
            brand = "TestBrand",
            model = "TestModel",
            url = "",
            unit = "Clicks",
            note = "",
            minUm = minUm,
            maxUm = maxUm,
            fmt = fmt,
            step = step,
            keepZero = false,
            minPos = minPos,
            maxPos = maxPos,
            minScale = "0",
            maxScale = "40",
            anchors = listOf(0 to 0f, 1000 to 40f),
            methods = emptyMap(),
            methodRanges = emptyMap()
        )
    }

    @Test
    fun testMicronsToPositionInterpolation() {
        val g = createDummyGrinder()
        assertEquals(0f, GrinderCalculator.micronsToPosition(0, g), 0.01f)
        assertEquals(20f, GrinderCalculator.micronsToPosition(500, g), 0.01f)
        assertEquals(40f, GrinderCalculator.micronsToPosition(1000, g), 0.01f)
    }

    @Test
    fun testPositionToMicronsInterpolation() {
        val g = createDummyGrinder()
        assertEquals(0, GrinderCalculator.positionToMicrons(0f, g))
        assertEquals(500, GrinderCalculator.positionToMicrons(20f, g))
        assertEquals(1000, GrinderCalculator.positionToMicrons(40f, g))
    }

    @Test
    fun test1ZpressoFormat() {
        val g = createDummyGrinder(fmt = "1z_10_3") // 30 clicks/rev
        assertEquals("0.0.0", GrinderCalculator.formatPosition(0f, g))
        assertEquals("1.0.0", GrinderCalculator.formatPosition(30f, g))
        assertEquals("1.1.2", GrinderCalculator.formatPosition(35f, g))
    }

    @Test
    fun testBaratzaFormat() {
        val g = createDummyGrinder(fmt = "baratza_az") // 26 steps
        assertEquals("1A", GrinderCalculator.formatPosition(0f, g))
        assertEquals("1B", GrinderCalculator.formatPosition(1f, g))
        assertEquals("2A", GrinderCalculator.formatPosition(26f, g))
    }

    @Test
    fun testZeroOffset() {
        val g = createDummyGrinder(fmt = "num")
        assertEquals("22", GrinderCalculator.formatPosition(20f, g, zeroOffset = 2f))
        assertEquals("18", GrinderCalculator.formatPosition(20f, g, zeroOffset = -2f))
    }

    @Test
    fun testCoffeeBeanDataModelAndOrigins() {
        val bean = CoffeeBeanEntity(
            origin = "Эфиопия",
            region = "Гуджи, Урага",
            name = "Эфиопия Гуджи",
            roaster = "Tasty Coffee",
            roastDate = "20.10.2026",
            process = "Натуральная",
            variety = "Heirloom",
            altitude = "2100 м",
            roastLevel = "Светлая",
            rating = 5.0f,
            tastingNotes = "Персик, Бергамот, Жасмин",
            detailedReview = "Превосходная сладкая чашка",
            acidityScore = 4,
            sweetnessScore = 5,
            bodyScore = 4
        )

        assertEquals("Эфиопия", bean.origin)
        assertEquals(5.0f, bean.rating, 0.01f)
        assertTrue(bean.tastingNotes.contains("Персик"))

        val country = CoffeeOriginsHelper.COUNTRIES.find { it.nameRu == "Эфиопия" }
        assertNotNull(country)
        assertEquals("🇪🇹", country?.flag)
    }

    @Test
    fun testRoastFreshnessCalculation() {
        val todayStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val fresh = CoffeeOriginsHelper.calculateFreshness(todayStr)
        assertEquals(0, fresh.daysOffRoast)
        assertTrue(fresh.statusText.contains("0 дн"))
    }
}
