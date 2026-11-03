package com.example.data.model

enum class TasteProfile(val title: String, val subtitle: String) {
    SOUR("🍋 Кислый / Резкий", "Агрессивная кислотность, не хватает сладости"),
    WATERY("💧 Водянистый / Пустой", "Слабое тело, плоский и ненасыщенный вкус"),
    BITTER("🍫 Горький / Тяжёлый", "Навязчивая горечь, жжёные или древесные ноты"),
    ASTRINGENT("🍂 Вяжущий / Сухой финиш", "Сушит язык и нёбо в послевкусии"),
    CHANNELING("⚡ И кислит, и горчит сразу", "Каналообразование (неравномерная экстракция)"),
    BALANCED("✨ Сбалансированный и сладкий", "Чистая кислотность, плотное тело и сладкий финиш")
}

enum class BrewTimeProfile(val label: String) {
    FAST("⚡ Быстрее нормы (< 2:10)"),
    NORMAL("⏱ В пределах нормы (2:15–3:00)"),
    SLOW("🐢 Дольше нормы / затык (> 3:15)")
}

data class ExtractionDiagnosis(
    val title: String,
    val explanation: String,
    val deltaUm: Int,
    val deltaTemp: Int,
    val extraAdvice: String
)

object ExtractionCompassCalculator {
    fun diagnose(
        taste: TasteProfile,
        time: BrewTimeProfile,
        actualTimeStr: String,
        grinder: Grinder,
        currentMicrons: Int
    ): ExtractionDiagnosis {
        val timeDisplay = actualTimeStr.ifBlank { "2:35" }
        return when (taste) {
            TasteProfile.BALANCED -> ExtractionDiagnosis(
                title = "✅ Отличная экстракция (Sweet Spot)",
                explanation = "Чашка сбалансирована при времени $timeDisplay. Текущая настройка идеально подходит под данное зерно.",
                deltaUm = 0,
                deltaTemp = 0,
                extraAdvice = "Зафиксируйте этот рецепт во вкладке «Дневник», чтобы не потерять удачную настройку."
            )
            TasteProfile.SOUR -> when (time) {
                BrewTimeProfile.FAST -> ExtractionDiagnosis(
                    title = "📉 Недоэкстракция + слишком быстрый пролив",
                    explanation = "Вода протекает слишком быстро ($timeDisplay), не успевая извлечь сахара и сладость.",
                    deltaUm = -45,
                    deltaTemp = 2,
                    extraAdvice = "Уменьшите помол (мельче), увеличьте дозировку кофе на 1 г или сделайте swirl на блуминге."
                )
                BrewTimeProfile.NORMAL -> ExtractionDiagnosis(
                    title = "📉 Недоэкстракция при нормальном времени",
                    explanation = "При времени $timeDisplay из зерна вымылись органические кислоты, но не хватило энергии для экстракции сахаров.",
                    deltaUm = -30,
                    deltaTemp = 2,
                    extraAdvice = "Для светлой обжарки увеличьте время блуминга до 45–60 секунд и сделайте помол чуть мельче."
                )
                BrewTimeProfile.SLOW -> ExtractionDiagnosis(
                    title = "⚠️ Кислинка при долгом проливе (забивание фильтра)",
                    explanation = "Слишком мелкий помол или пыль забили фильтр ($timeDisplay), вода пошла по каналам, оставив часть кофе недоэкстрагированной.",
                    deltaUm = 25,
                    deltaTemp = 2,
                    extraAdvice = "Укрупните помол, чтобы выровнять проток, но поднимите температуру воды на +2 °C."
                )
            }
            TasteProfile.WATERY -> ExtractionDiagnosis(
                title = "💧 Слабая концентрация (низкий TDS)",
                explanation = "Напитку не хватает плотности и сладости (время $timeDisplay).",
                deltaUm = if (time == BrewTimeProfile.FAST) -40 else -25,
                deltaTemp = 1,
                extraAdvice = "Уменьшите помол и попробуйте сократить пропорцию воды (например, с 1:16.5 до 1:15)."
            )
            TasteProfile.BITTER -> ExtractionDiagnosis(
                title = if (time == BrewTimeProfile.SLOW) "📈 Переэкстракция + затянутый пролив" else "📈 Переэкстракция (горечь)",
                explanation = "В чашку перешли тяжёлые горькие фенолы и алкалоиды (время $timeDisplay).",
                deltaUm = if (time == BrewTimeProfile.SLOW) 45 else 30,
                deltaTemp = -2,
                extraAdvice = "Сделайте помол крупнее и вливайте воду более плавной струёй ближе к центру."
            )
            TasteProfile.ASTRINGENT -> ExtractionDiagnosis(
                title = "🍂 Сухой вяжущий финиш (микроканалы)",
                explanation = "Сухость на нёбе при времени $timeDisplay вызывают танины, вымываемые из мелкой пыли или при избыточной турбулентности.",
                deltaUm = 35,
                deltaTemp = -2,
                extraAdvice = "Укрупните помол и избегайте вливания воды непосредственно на стенки фильтра."
            )
            TasteProfile.CHANNELING -> ExtractionDiagnosis(
                title = "⚡ Каналообразование (Channeling)",
                explanation = "Вода промыла отдельные каналы: вдоль каналов кофе переэкстрагировался (горечь), а комки остались сухими (кислота).",
                deltaUm = 25,
                deltaTemp = 0,
                extraAdvice = "Тщательно смочите весь кофе на этапе блуминга (сделайте аккуратный swirl) и чуть укрупните помол."
            )
        }
    }
}
