package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class OriginCountry(
    val code: String,
    val nameRu: String,
    val flag: String,
    val defaultRegions: List<String>,
    val typicalNotes: List<String>
)

data class RoastFreshness(
    val daysOffRoast: Int?,
    val statusText: String,
    val tip: String
)

object CoffeeOriginsHelper {

    val COUNTRIES = listOf(
        OriginCountry("ET", "Эфиопия", "🇪🇹", listOf("Гуджи", "Иргачефф", "Сидама", "Джимма"), listOf("Бергамот", "Жасмин", "Персик", "Чёрный чай")),
        OriginCountry("CO", "Колумбия", "🇨🇴", listOf("Уила", "Нариньо", "Каука", "Антьокия"), listOf("Красное яблоко", "Карамель", "Цитрус", "Тропические фрукты")),
        OriginCountry("KE", "Кения", "🇰🇪", listOf("Ньери", "Кириньяга", "Эмбу", "Киямбу"), listOf("Чёрная смородина", "Грейпфрут", "Ежевика", "Томаты")),
        OriginCountry("BR", "Бразилия", "🇧🇷", listOf("Серрадо", "Сул-ди-Минас", "Можиана", "Эспириту-Санту"), listOf("Шоколад", "Фундук", "Ореховая паста", "Карамель")),
        OriginCountry("CR", "Коста-Рика", "🇨🇷", listOf("Тарразу", "Центральная Долина", "Брунка"), listOf("Спелое яблоко", "Мёд", "Красные ягоды", "Тропики")),
        OriginCountry("GT", "Гватемала", "🇬🇹", listOf("Антигуа", "Уэуэтенанаго", "Кобан", "Фрайханес"), listOf("Тёмный шоколад", "Какао", "Апельсин", "Пряности")),
        OriginCountry("RW", "Руанда", "🇷🇼", listOf("Ньямашеке", "Западная провинция", "Русизи"), listOf("Красный чай", "Инжир", "Красная смородина", "Цитрус")),
        OriginCountry("PA", "Панама", "🇵🇦", listOf("Бокете", "Вулкан Бару", "Ренасимьенто"), listOf("Жасмин", "Лемонграсс", "Манго", "Папайя")),
        OriginCountry("ID", "Индонезия", "🇮🇩", listOf("Суматра Ачех", "Бали Кинтамани", "Ява"), listOf("Тёмный шоколад", "Пряности", "Кедр", "Гвоздика")),
        OriginCountry("SV", "Сальвадор", "🇸🇻", listOf("Апанека", "Санта-Ана", "Чалатенанго"), listOf("Слива", "Молочный шоколад", "Миндаль", "Ириска")),
        OriginCountry("OTHER", "Другое происхождение", "🌍", emptyList(), listOf("Шоколад", "Карамель", "Фрукты"))
    )

    val TASTING_NOTE_GROUPS = mapOf(
        "🌸 Цветочные" to listOf("Жасмин", "Бергамот", "Чайная роза", "Лемонграсс", "Цветы апельсина"),
        "🍑 Фруктовые" to listOf("Персик", "Абрикос", "Красное яблоко", "Груша", "Слива", "Белый виноград"),
        "🫐 Ягодные" to listOf("Черника", "Клубника", "Малина", "Чёрная смородина", "Вишня", "Клюква"),
        "🍊 Цитрусовые" to listOf("Грейпфрут", "Лайм", "Апельсин", "Красный апельсин", "Юдзу", "Мандарин"),
        "🥭 Тропические" to listOf("Манго", "Маракуйя", "Ананас", "Папайя", "Личи", "Гуава"),
        "🍯 Сладкие" to listOf("Карамель", "Тростниковый сахар", "Мёд", "Ваниль", "Ириска", "Кленовый сироп"),
        "🍫 Шоколад и орехи" to listOf("Тёмный шоколад", "Какао", "Фундук", "Миндаль", "Грецкий орех")
    )

    fun calculateFreshness(roastDateStr: String): RoastFreshness {
        if (roastDateStr.isBlank()) {
            return RoastFreshness(null, "Дата обжарки не указана", "Укажите дату, чтобы отслеживать пик вкуса")
        }

        val formats = listOf(
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )

        var parsedDate: Date? = null
        for (fmt in formats) {
            try {
                parsedDate = fmt.parse(roastDateStr.trim())
                if (parsedDate != null) break
            } catch (e: Exception) {
                // try next format
            }
        }

        if (parsedDate == null) {
            return RoastFreshness(null, "Неверный формат даты", "Используйте ДД.ММ.ГГГГ")
        }

        val diffMillis = System.currentTimeMillis() - parsedDate.time
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

        return when {
            days < 0 -> RoastFreshness(0, "Будущая дата", "Проверьте дату")
            days in 0..3 -> RoastFreshness(
                days,
                "Свежее • $days дн. (Активная дегазация)",
                "Много CO₂: дайте зерну полежать или увеличьте время блуминга до 60+ сек"
            )
            days in 4..7 -> RoastFreshness(
                days,
                "Дегазация • $days дн.",
                "Отлично для фильтра с хорошим перемешиванием, для эспрессо лучше подождать ещё 5–7 дней"
            )
            days in 8..30 -> RoastFreshness(
                days,
                "🔥 Пик вкуса • $days дн.",
                "Идеальный баланс сладости, чистоты и комплексной кислотности!"
            )
            days in 31..60 -> RoastFreshness(
                days,
                "Зрелое • $days дн.",
                "Хорошо раскрывается в иммерсии (Switch, French Press) и эспрессо"
            )
            else -> RoastFreshness(
                days,
                "Старение • $days дн.",
                "Рекомендуется использовать быстрее, чтобы не потерять яркие дескрипторы"
            )
        }
    }
}
