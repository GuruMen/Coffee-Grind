package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CoffeeBeanEntity
import com.example.data.local.GarageSettingEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.model.BrewMethod
import com.example.data.model.Category
import com.example.data.model.Grinder
import com.example.data.model.MethodRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

class CoffeeGrindRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val coffeeBeanDao = db.coffeeBeanDao()
    private val journalDao = db.journalDao()
    private val garageDao = db.garageDao()

    var categories: List<Category> = emptyList()
        private set
    var brewMethods: List<BrewMethod> = emptyList()
        private set
    var grinders: List<Grinder> = emptyList()
        private set
    var brands: List<String> = emptyList()
        private set

    val quickPicks = listOf(
        "Comandante" to "C40 MK4",
        "1Zpresso" to "K-Ultra",
        "1Zpresso" to "ZP6 Special",
        "1Zpresso" to "Q2 (Heptagonal burrs)",
        "Fellow" to "Ode Brew Grinder Gen 2",
        "Timemore" to "C3",
        "KINGrinder" to "K6",
        "KINGrinder" to "K7",
        "Baratza" to "Encore ESP"
    )

    init {
        loadDataFromAssets()
        seedDefaultsIfNeeded()
    }

    private fun loadDataFromAssets() {
        try {
            val jsonString = context.assets.open("coffee_data.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            // 1. Categories
            val catsJson = root.getJSONArray("categories")
            val catList = mutableListOf<Category>()
            for (i in 0 until catsJson.length()) {
                val c = catsJson.getJSONObject(i)
                catList.add(
                    Category(
                        id = c.getString("id"),
                        name = c.getString("name"),
                        nameRu = c.getString("name_ru"),
                        minUm = c.getInt("min_um"),
                        maxUm = c.getInt("max_um")
                    )
                )
            }
            categories = catList

            // 2. Brew Methods
            val methodsJson = root.getJSONArray("brew_methods")
            val bmList = mutableListOf<BrewMethod>()
            val bmIds = mutableListOf<String>()
            for (i in 0 until methodsJson.length()) {
                val m = methodsJson.getJSONObject(i)
                val id = m.getString("id")
                bmIds.add(id)
                bmList.add(
                    BrewMethod(
                        id = id,
                        nameEn = m.getString("name_en"),
                        nameRu = m.getString("name_ru"),
                        minUm = m.getInt("min_um"),
                        maxUm = m.getInt("max_um"),
                        ratio = m.optDouble("ratio", 15.0).toFloat(),
                        dose = m.optDouble("dose", 15.0).toFloat(),
                        tip = m.optString("tip", ""),
                        star = m.optBoolean("star", false),
                        desc = m.optString("desc", "")
                    )
                )
            }
            brewMethods = bmList

            // 3. Grinders
            val grindersJson = root.getJSONArray("grinders_compact")
            val gList = mutableListOf<Grinder>()
            for (idx in 0 until grindersJson.length()) {
                val row = grindersJson.getJSONArray(idx)
                val brand = row.getString(0)
                val model = row.getString(1)
                val rawUrl = row.getString(2)
                val url = if (rawUrl.startsWith("@HCG/")) rawUrl.replace("@HCG/", "https://honestcoffeeguide.com/") else rawUrl
                val unit = row.getString(3)
                val note = row.optString(4, "")
                val minUm = row.getInt(5)
                val maxUm = row.getInt(6)
                val fmt = row.getString(7)
                val step = row.getDouble(8).toFloat()
                val keepZero = row.getInt(9) != 0
                val minPos = row.getDouble(10).toFloat()
                val maxPos = row.getDouble(11).toFloat()
                val minScale = row.optString(12, "0")
                val maxScale = row.optString(13, "0")

                // Anchors
                val anchorsJson = row.getJSONArray(14)
                val anchors = mutableListOf<Pair<Int, Float>>()
                for (a in 0 until anchorsJson.length()) {
                    val pair = anchorsJson.getJSONArray(a)
                    anchors.add(pair.getInt(0) to pair.getDouble(1).toFloat())
                }

                // Methods display array
                val mArr = row.getJSONArray(15)
                val methodsMap = mutableMapOf<String, String>()
                for (mIdx in 0 until mArr.length()) {
                    if (mIdx < bmIds.size) {
                        methodsMap[bmIds[mIdx]] = mArr.optString(mIdx, "N/A")
                    }
                }

                // Method ranges array
                val mrArr = row.optJSONArray(16)
                val methodRangesMap = mutableMapOf<String, MethodRange?>()
                if (mrArr != null) {
                    for (mrIdx in 0 until mrArr.length()) {
                        if (mrIdx < bmIds.size) {
                            val mid = bmIds[mrIdx]
                            val bm = brewMethods.find { it.id == mid }
                            if (!mrArr.isNull(mrIdx) && bm != null) {
                                val item = mrArr.getJSONArray(mrIdx)
                                val lowPos = item.getDouble(0).toFloat()
                                val highPos = item.getDouble(1).toFloat()
                                val midPos = item.getDouble(2).toFloat()
                                val midStr = item.getString(3)

                                var lowUm = maxOf(minUm, bm.minUm)
                                var highUm = minOf(maxUm, bm.maxUm)
                                if (lowUm > highUm) {
                                    lowUm = minUm
                                    highUm = minUm
                                }
                                val midUm = (lowUm + highUm) / 2

                                methodRangesMap[mid] = MethodRange(
                                    str = methodsMap[mid] ?: midStr,
                                    lowPos = lowPos,
                                    highPos = highPos,
                                    midPos = midPos,
                                    midStr = midStr,
                                    lowUm = lowUm,
                                    highUm = highUm,
                                    midUm = midUm
                                )
                            } else {
                                methodRangesMap[mid] = null
                            }
                        }
                    }
                }

                gList.add(
                    Grinder(
                        idx = idx,
                        brand = brand,
                        model = model,
                        url = url,
                        unit = unit,
                        note = note,
                        minUm = minUm,
                        maxUm = maxUm,
                        fmt = fmt,
                        step = step,
                        keepZero = keepZero,
                        minPos = minPos,
                        maxPos = maxPos,
                        minScale = minScale,
                        maxScale = maxScale,
                        anchors = anchors,
                        methods = methodsMap,
                        methodRanges = methodRangesMap
                    )
                )
            }
            grinders = gList
            brands = gList.map { it.brand }.distinct().sorted()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedDefaultsIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentGarage = garageDao.getAllSettings().first()
                if (currentGarage.isEmpty()) {
                    garageDao.upsertSetting(GarageSettingEntity("KINGrinder::K7", isFavorite = true, zeroOffset = 0f))
                    garageDao.upsertSetting(GarageSettingEntity("1Zpresso::ZP6 Special", isFavorite = true, zeroOffset = 0f))
                    garageDao.upsertSetting(GarageSettingEntity("Comandante::C40 MK4", isFavorite = true, zeroOffset = 0f))
                }

                val currentJournal = journalDao.getAllEntries().first()
                if (currentJournal.isEmpty()) {
                    journalDao.insertEntry(
                        JournalEntryEntity(
                            date = "15.10.2026",
                            coffeeName = "Эфиопия Гуджи Урага",
                            roaster = "Tasty Coffee",
                            process = "Натуральная",
                            roastLevel = "Светлая (Фильтр)",
                            methodId = "steep_and_release",
                            methodName = "Иммерсия с клапаном (Hario Switch / Clever)",
                            grinder = "KINGrinder K7",
                            setting = "82 клика (640 μm)",
                            microns = 640,
                            dose = 16f,
                            water = 250f,
                            temp = 93,
                            time = "2:40",
                            rating = 5,
                            notes = "Гибридный рецепт Касуя (40% открытый клапан + 60% иммерсия). Спелый персик, клубничный джем, плотное сиропистое тело."
                        )
                    )
                    journalDao.insertEntry(
                        JournalEntryEntity(
                            date = "16.10.2026",
                            coffeeName = "Колумбия Уила Розовый Бурбон",
                            roaster = "Сварщица Екатерина",
                            process = "Мытая",
                            roastLevel = "Светлая (Фильтр)",
                            methodId = "v60",
                            methodName = "Пуровер Hario V60",
                            grinder = "1Zpresso ZP6 Special",
                            setting = "4.5 (550 μm)",
                            microns = 550,
                            dose = 15f,
                            water = 250f,
                            temp = 95,
                            time = "2:30",
                            rating = 5,
                            notes = "Рецепт Хоффманна (5 вливаний по 50 г). Кристально чистая чашка, красный грейпфрут, жасмин и тростниковый сахар."
                        )
                    )
                }

                val currentBeans = coffeeBeanDao.getAllBeans().first()
                if (currentBeans.isEmpty()) {
                    coffeeBeanDao.insertBean(
                        CoffeeBeanEntity(
                            origin = "Эфиопия",
                            region = "Гуджи, Урага",
                            name = "Эфиопия Гуджи Урага",
                            roaster = "Tasty Coffee",
                            roastDate = "20.10.2026",
                            process = "Натуральная",
                            variety = "Эфиопское наследие (Heirloom)",
                            altitude = "2000–2250 м",
                            roastLevel = "Светлая (Фильтр)",
                            rating = 5.0f,
                            tastingNotes = "Спелый персик, Клубничный джем, Бергамот, Жасмин",
                            detailedReview = "Потрясающая сладкая чашка в Hario Switch по рецепту Касуя. Сочная сладость спелого персика и клубники с деликатным жасминовым послевкусием.",
                            acidityScore = 4,
                            sweetnessScore = 5,
                            bodyScore = 4,
                            inStock = true,
                            isFavorite = true,
                            bestGrinderSetting = "KINGrinder K7: 82 клика (640 μm)"
                        )
                    )
                    coffeeBeanDao.insertBean(
                        CoffeeBeanEntity(
                            origin = "Колумбия",
                            region = "Уила, Сан-Адольфо",
                            name = "Колумбия Уила Розовый Бурбон",
                            roaster = "Сварщица Екатерина",
                            roastDate = "24.10.2026",
                            process = "Мытая",
                            variety = "Розовый Бурбон (Pink Bourbon)",
                            altitude = "1850 м",
                            roastLevel = "Светлая (Фильтр)",
                            rating = 5.0f,
                            tastingNotes = "Жасмин, Красный грейпфрут, Тростниковый сахар, Чёрный чай",
                            detailedReview = "Кристальная чистота в V60 Хоффманна (5 вливаний). Выразительный цветочный букет, деликатная цитрусовая кислотность и сладкий чайный финиш.",
                            acidityScore = 5,
                            sweetnessScore = 5,
                            bodyScore = 3,
                            inStock = true,
                            isFavorite = true,
                            bestGrinderSetting = "1Zpresso ZP6: 4.5 (550 μm)"
                        )
                    )
                    coffeeBeanDao.insertBean(
                        CoffeeBeanEntity(
                            origin = "Кения",
                            region = "Кириньяга",
                            name = "Кения Кириньяга Каримику",
                            roaster = "The Barn",
                            roastDate = "28.10.2026",
                            process = "Мытая",
                            variety = "SL28, SL34",
                            altitude = "1750 м",
                            roastLevel = "Светлая (Фильтр)",
                            rating = 4.5f,
                            tastingNotes = "Чёрная смородина, Ежевика, Грейпфрут, Тёмный мёд",
                            detailedReview = "Мощная смородиновая кислотность и густое ягодное тело. Отлично заваривается в Kalita Wave.",
                            acidityScore = 5,
                            sweetnessScore = 4,
                            bodyScore = 4,
                            inStock = true,
                            isFavorite = false,
                            bestGrinderSetting = "Comandante C40: 23 клика (638 μm)"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCoffeeBeans(): Flow<List<CoffeeBeanEntity>> = coffeeBeanDao.getAllBeans()

    suspend fun saveCoffeeBean(bean: CoffeeBeanEntity): Long = coffeeBeanDao.insertBean(bean)

    suspend fun updateCoffeeBean(bean: CoffeeBeanEntity) = coffeeBeanDao.updateBean(bean)

    suspend fun deleteCoffeeBean(id: Long) = coffeeBeanDao.deleteBean(id)

    suspend fun toggleBeanFavorite(id: Long, isFav: Boolean) = coffeeBeanDao.setFavorite(id, isFav)

    suspend fun toggleBeanInStock(id: Long, inStock: Boolean) = coffeeBeanDao.setInStock(id, inStock)

    fun getJournalEntries(): Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()

    suspend fun saveJournalEntry(entry: JournalEntryEntity): Long = journalDao.insertEntry(entry)

    suspend fun deleteJournalEntry(id: Long) = journalDao.deleteEntry(id)

    fun getGarageSettings(): Flow<List<GarageSettingEntity>> = garageDao.getAllSettings()

    suspend fun toggleGarageFavorite(grinderKey: String, currentFavorite: Boolean) {
        val existing = garageDao.getSetting(grinderKey)
        garageDao.upsertSetting(
            GarageSettingEntity(
                grinderKey = grinderKey,
                isFavorite = !currentFavorite,
                zeroOffset = existing?.zeroOffset ?: 0f
            )
        )
    }

    suspend fun updateZeroOffset(grinderKey: String, newOffset: Float) {
        val existing = garageDao.getSetting(grinderKey)
        garageDao.upsertSetting(
            GarageSettingEntity(
                grinderKey = grinderKey,
                isFavorite = existing?.isFavorite ?: false,
                zeroOffset = newOffset
            )
        )
    }
}
