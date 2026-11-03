package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coffee_beans")
data class CoffeeBeanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val origin: String, // e.g. "Эфиопия", "Колумбия", "Кения", "Бразилия"
    val region: String = "", // e.g. "Гуджи, Урага", "Уила", "Ньери"
    val name: String, // e.g. "Эфиопия Гуджи Урага", "Розовый Бурбон"
    val roaster: String = "", // e.g. "Tasty Coffee", "Сварщица Екатерина"
    val roastDate: String = "", // e.g. "2026-10-20" or "20.10.2026"
    val process: String = "Мытая", // Мытая, Натуральная, Анаэробная, Хани
    val variety: String = "", // Эфиопское наследие, Гейша, SL28, Катурра
    val altitude: String = "", // e.g. "1900–2200 м"
    val roastLevel: String = "Светлая (Фильтр)", // Светлая, Средняя, Тёмная
    val rating: Float = 5.0f, // 1.0 - 5.0
    val tastingNotes: String = "", // Comma-separated flavor tags
    val detailedReview: String = "", // Tasting review text
    val acidityScore: Int = 4, // 1-5
    val sweetnessScore: Int = 5, // 1-5
    val bodyScore: Int = 4, // 1-5
    val inStock: Boolean = true,
    val isFavorite: Boolean = false,
    val bestGrinderSetting: String = "", // e.g. "22 клика (600 μm)"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val coffeeName: String,
    val roaster: String,
    val process: String,
    val roastLevel: String,
    val methodId: String,
    val methodName: String,
    val grinder: String,
    val setting: String,
    val microns: Int,
    val dose: Float,
    val water: Float,
    val temp: Int,
    val time: String,
    val rating: Int,
    val notes: String
)

@Entity(tableName = "garage_settings")
data class GarageSettingEntity(
    @PrimaryKey
    val grinderKey: String, // e.g. "Comandante::C40 MK4"
    val isFavorite: Boolean = false,
    val zeroOffset: Float = 0f
)
