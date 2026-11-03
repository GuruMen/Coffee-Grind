package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CoffeeBeanEntity
import com.example.data.model.CoffeeOriginsHelper
import com.example.data.model.GrinderCalculator
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGold
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeePurple
import com.example.ui.theme.CoffeeRose
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.CoffeeGrindViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class JournalViewTab(val title: String) {
    BEANS("🫘 Зёрна и происхождение"),
    BREWS("📋 Сессии заваривания")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(JournalViewTab.BEANS) }
    val calcState by viewModel.calcState.collectAsState()
    val beans by viewModel.coffeeBeans.collectAsState()
    val entries by viewModel.journalEntries.collectAsState()
    val beanSearchQuery by viewModel.beanSearchQuery.collectAsState()
    val originFilter by viewModel.originFilter.collectAsState()
    val inStockOnly by viewModel.inStockOnly.collectAsState()
    val favoritesOnly by viewModel.favoritesOnly.collectAsState()

    var showAddBeanForm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab switcher: Beans vs Brew Logs
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CoffeeDarkElevated,
            contentColor = CoffeeAmberLight,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CoffeeDarkBorder, RoundedCornerShape(10.dp))
        ) {
            JournalViewTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                val countTag = if (tab == JournalViewTab.BEANS) " (${beans.size})" else " (${entries.size})"
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            text = "${tab.title}$countTag",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    }
                )
            }
        }

        if (activeSubTab == JournalViewTab.BEANS) {
            // === COFFEE BEANS & ORIGINS SECTION ===

            // Search and Filter Bar
            Card(
                modifier = Modifier.fillMaxWidth().testTag("bean_search_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Дневник зерна (${beans.size} лотов)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { showAddBeanForm = !showAddBeanForm },
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp).testTag("add_bean_btn")
                        ) {
                            Icon(
                                imageVector = if (showAddBeanForm) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAddBeanForm) "Закрыть" else "Добавить лот", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = beanSearchQuery,
                        onValueChange = { viewModel.setBeanSearch(it) },
                        placeholder = { Text("Поиск: Эфиопия, жасмин, Tasty Coffee, гейша...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (beanSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setBeanSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Origin country chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = originFilter == "ALL",
                            onClick = { viewModel.setOriginFilter("ALL") },
                            label = { Text("Все страны", fontSize = 12.sp) }
                        )
                        CoffeeOriginsHelper.COUNTRIES.filter { it.code != "OTHER" }.forEach { country ->
                            val isSel = originFilter.equals(country.nameRu, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setOriginFilter(if (isSel) "ALL" else country.nameRu) },
                                label = { Text("${country.flag} ${country.nameRu}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CoffeeAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = CoffeeAmberLight
                                )
                            )
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = inStockOnly,
                            onClick = { viewModel.toggleInStockFilter() },
                            label = { Text("📦 В наличии", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { viewModel.toggleFavoritesFilter() },
                            label = { Text("★ Избранное", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoffeeGold.copy(alpha = 0.2f),
                                selectedLabelColor = CoffeeGold
                            )
                        )
                    }
                }
            }

            // Expandable Add/Edit Bean Form
            AnimatedVisibility(visible = showAddBeanForm) {
                AddCoffeeBeanForm(
                    onSave = { bean ->
                        viewModel.saveCoffeeBean(bean)
                        showAddBeanForm = false
                    },
                    onCancel = { showAddBeanForm = false }
                )
            }

            // Coffee Beans List
            if (beans.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🫘", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Лотов зерна не найдено",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Нажмите «Добавить лот» выше, чтобы записать происхождение, дату обжарки и дескрипторы вкуса.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                beans.forEach { bean ->
                    CoffeeBeanCard(
                        bean = bean,
                        onDelete = { viewModel.deleteCoffeeBean(bean.id) },
                        onToggleFavorite = { viewModel.toggleBeanFavorite(bean.id, bean.isFavorite) },
                        onToggleInStock = { viewModel.toggleBeanInStock(bean.id, bean.inStock) },
                        onApplyToCalc = { viewModel.applyBeanToCalculator(bean) }
                    )
                }
            }
        } else {
            // === HISTORICAL BREW SESSIONS / LOGS ===
            BrewLogsSection(
                viewModel = viewModel,
                entries = entries
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCoffeeBeanForm(
    onSave: (CoffeeBeanEntity) -> Unit,
    onCancel: () -> Unit
) {
    var selectedCountry by remember { mutableStateOf(CoffeeOriginsHelper.COUNTRIES.first()) }
    var region by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var roaster by remember { mutableStateOf("") }
    var roastDate by remember {
        val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        mutableStateOf(today)
    }
    var process by remember { mutableStateOf("Мытая") }
    var variety by remember { mutableStateOf("") }
    var altitude by remember { mutableStateOf("") }
    var roastLevel by remember { mutableStateOf("Светлая (Фильтр)") }
    var rating by remember { mutableStateOf(5.0f) }
    var tastingNotesList by remember { mutableStateOf(mutableListOf<String>()) }
    var customTag by remember { mutableStateOf("") }
    var detailedReview by remember { mutableStateOf("") }
    var acidity by remember { mutableStateOf(4) }
    var sweetness by remember { mutableStateOf(5) }
    var body by remember { mutableStateOf(4) }
    var bestGrinderSetting by remember { mutableStateOf("") }

    val freshness = remember(roastDate) {
        CoffeeOriginsHelper.calculateFreshness(roastDate)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("add_bean_form"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CoffeeAmber),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "✨ Карточка кофейного лота",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CoffeeAmberLight)
            )

            // Origin Country dropdown
            var countryExpanded by remember { mutableStateOf(false) }
            Column {
                Text("Страна происхождения (Origin):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { countryExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("${selectedCountry.flag} ${selectedCountry.nameRu}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                        CoffeeOriginsHelper.COUNTRIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.flag} ${c.nameRu}") },
                                onClick = {
                                    selectedCountry = c
                                    if (name.isBlank() || CoffeeOriginsHelper.COUNTRIES.any { name.startsWith(it.nameRu) }) {
                                        name = c.nameRu + (if (region.isNotBlank()) " $region" else "")
                                    }
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Region & Lot Name
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = region,
                    onValueChange = {
                        region = it
                        if (name.isBlank()) name = "${selectedCountry.nameRu} $it"
                    },
                    label = { Text("Регион / Ферма") },
                    placeholder = { Text("Гуджи, Урага") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название лота") },
                    placeholder = { Text("Эфиопия Гуджи") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Roaster & Roast Date
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roaster,
                    onValueChange = { roaster = it },
                    label = { Text("Обжарщик") },
                    placeholder = { Text("Tasty Coffee") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = roastDate,
                    onValueChange = { roastDate = it },
                    label = { Text("Дата обжарки") },
                    placeholder = { Text("ДД.ММ.ГГГГ") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Freshness Banner based on Roast Date
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CoffeeTeal.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeTeal.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📅 ${freshness.statusText}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = CoffeeTeal
                    )
                    Text(
                        text = freshness.tip,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Process, Variety, Altitude
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var procExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { procExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(process, maxLines = 1, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = procExpanded, onDismissRequest = { procExpanded = false }) {
                        listOf("Мытая", "Натуральная", "Анаэробная", "Хани", "Инфьюз", "Декаф").forEach { p ->
                            DropdownMenuItem(text = { Text(p) }, onClick = { process = p; procExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = variety,
                    onValueChange = { variety = it },
                    label = { Text("Разновидность") },
                    placeholder = { Text("Heirloom, Bourbon") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = altitude,
                    onValueChange = { altitude = it },
                    label = { Text("Высота (м)") },
                    placeholder = { Text("1900–2200 м") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                var roastExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { roastExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(roastLevel, maxLines = 1, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = roastExpanded, onDismissRequest = { roastExpanded = false }) {
                        listOf("Светлая (Фильтр)", "Средняя (Омни)", "Тёмная (Эспрессо)").forEach { r ->
                            DropdownMenuItem(text = { Text(r) }, onClick = { roastLevel = r; roastExpanded = false })
                        }
                    }
                }
            }

            // Rating Stars (1 - 5)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Оценка лота (Rating):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${rating.roundToInt()} из 5 ★", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CoffeeGold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star.toFloat() }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star звезд",
                                tint = if (star <= rating) CoffeeGold else CoffeeDarkBorder,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Tasting Notes & Flavor Tags Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Дескрипторы вкуса (Tasting Notes):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Current selected tags
                if (tastingNotesList.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tastingNotesList.forEach { tag ->
                            Surface(
                                color = CoffeeAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber),
                                modifier = Modifier.clickable {
                                    tastingNotesList = tastingNotesList.filter { it != tag }.toMutableList()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tag, fontSize = 12.sp, color = CoffeeAmberLight, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("✕", fontSize = 10.sp, color = CoffeeAmberLight)
                                }
                            }
                        }
                    }
                }

                // Quick add preset tag categories
                CoffeeOriginsHelper.TASTING_NOTE_GROUPS.forEach { (catName, tags) ->
                    Text(catName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            val isAdded = tastingNotesList.contains(tag)
                            FilterChip(
                                selected = isAdded,
                                onClick = {
                                    tastingNotesList = if (isAdded) {
                                        tastingNotesList.filter { it != tag }.toMutableList()
                                    } else {
                                        (tastingNotesList + tag).toMutableList()
                                    }
                                },
                                label = { Text(tag, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                // Custom write-in tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        placeholder = { Text("Свой дескриптор...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            if (customTag.isNotBlank() && !tastingNotesList.contains(customTag.trim())) {
                                tastingNotesList = (tastingNotesList + customTag.trim()).toMutableList()
                                customTag = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeDarkElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Добавить")
                    }
                }
            }

            // Detailed Cupping Impressions
            OutlinedTextField(
                value = detailedReview,
                onValueChange = { detailedReview = it },
                label = { Text("Подробное описание и впечатления от чашки") },
                placeholder = { Text("Сочный персик на первом глотке, шелковистое тело, на остывании раскрывается чайная роза...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(8.dp)
            )

            // Cupping Attributes Sliders
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CoffeeDarkElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Баланс чашки (Cupping Profile):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Кислотность (Acidity): $acidity/5", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = acidity.toFloat(),
                            onValueChange = { acidity = it.roundToInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.width(160.dp),
                            colors = SliderDefaults.colors(thumbColor = CoffeeTeal, activeTrackColor = CoffeeTeal)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Сладость (Sweetness): $sweetness/5", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = sweetness.toFloat(),
                            onValueChange = { sweetness = it.roundToInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.width(160.dp),
                            colors = SliderDefaults.colors(thumbColor = CoffeeAmber, activeTrackColor = CoffeeAmber)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Тело / Плотность (Body): $body/5", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = body.toFloat(),
                            onValueChange = { body = it.roundToInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.width(160.dp),
                            colors = SliderDefaults.colors(thumbColor = CoffeeGreen, activeTrackColor = CoffeeGreen)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = bestGrinderSetting,
                onValueChange = { bestGrinderSetting = it },
                label = { Text("Идеальный помол для этого зерна") },
                placeholder = { Text("Например: Comandante C40 — 22 клика (600 μm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            // Submit & Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = {
                        val bean = CoffeeBeanEntity(
                            origin = selectedCountry.nameRu,
                            region = region.trim(),
                            name = name.ifBlank { "${selectedCountry.nameRu} ${region.trim()}" }.trim(),
                            roaster = roaster.trim(),
                            roastDate = roastDate.trim(),
                            process = process,
                            variety = variety.trim(),
                            altitude = altitude.trim(),
                            roastLevel = roastLevel,
                            rating = rating,
                            tastingNotes = tastingNotesList.joinToString(", "),
                            detailedReview = detailedReview.trim(),
                            acidityScore = acidity,
                            sweetnessScore = sweetness,
                            bodyScore = body,
                            bestGrinderSetting = bestGrinderSetting.trim(),
                            inStock = true,
                            isFavorite = true
                        )
                        onSave(bean)
                    },
                    modifier = Modifier.weight(1.5f).height(46.dp).testTag("save_coffee_bean_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("💾 Сохранить лот зерна", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CoffeeBeanCard(
    bean: CoffeeBeanEntity,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleInStock: () -> Unit,
    onApplyToCalc: () -> Unit
) {
    val country = CoffeeOriginsHelper.COUNTRIES.find { it.nameRu.equals(bean.origin, ignoreCase = true) }
    val flag = country?.flag ?: "🌍"
    val freshness = remember(bean.roastDate) {
        CoffeeOriginsHelper.calculateFreshness(bean.roastDate)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (bean.isFavorite) CoffeeAmber else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Flag, Name, Origin, Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(flag, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bean.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val originDetails = listOfNotNull(
                        bean.origin.takeIf { it.isNotBlank() },
                        bean.region.takeIf { it.isNotBlank() },
                        bean.altitude.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")

                    if (originDetails.isNotBlank()) {
                        Text(
                            text = originDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoffeeTeal
                        )
                    }

                    val farmInfo = listOfNotNull(
                        bean.roaster.takeIf { it.isNotBlank() }?.let { "Обжарщик: $it" },
                        bean.variety.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")

                    if (farmInfo.isNotBlank()) {
                        Text(
                            text = farmInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (bean.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (bean.isFavorite) CoffeeRose else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Badges row: Process, Roast Level, Rating
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Text(
                        text = "${bean.process} • ${bean.roastLevel}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Rating badge
                Surface(
                    color = CoffeeGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeGold.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${bean.rating} ★",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CoffeeGold
                    )
                }

                // In stock badge
                Surface(
                    modifier = Modifier.clickable { onToggleInStock() },
                    color = if (bean.inStock) CoffeeGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (bean.inStock) "📦 В наличии" else " Закончился",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bean.inStock) CoffeeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Roast Date & Freshness indicator
            if (bean.roastDate.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Обжарка: ${bean.roastDate}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = freshness.statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeTeal
                        )
                    }
                }
            }

            // Tasting Notes Tags
            if (bean.tastingNotes.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bean.tastingNotes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                        Surface(
                            color = CoffeeAmber.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CoffeeAmberLight
                            )
                        }
                    }
                }
            }

            // Detailed review
            if (bean.detailedReview.isNotBlank()) {
                Text(
                    text = "💬 ${bean.detailedReview}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Best grinder setting tag if recorded
            if (bean.bestGrinderSetting.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯 Помол: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(bean.bestGrinderSetting, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CoffeeGreen)
                    }
                }
            }

            // Actions row: Quick Brew & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onApplyToCalc,
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("☕ Заварить этот лот", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CoffeeRose, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun BrewLogsSection(
    viewModel: CoffeeGrindViewModel,
    entries: List<com.example.data.local.JournalEntryEntity>
) {
    val searchQuery by viewModel.journalSearchQuery.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Сессии заваривания (${entries.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setJournalSearch(it) },
                placeholder = { Text("Поиск сессий...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setJournalSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            if (entries.isEmpty()) {
                Text(
                    text = "Записей заваривания пока нет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                entries.forEach { entry ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CoffeeDarkElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.coffeeName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${entry.roaster} • ${entry.process} • ${entry.roastLevel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    repeat(entry.rating) {
                                        Text("★", color = CoffeeGold, fontSize = 14.sp)
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(entry.methodName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(entry.grinder, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(entry.setting, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CoffeeGreen)
                                        Text("${entry.dose.roundToInt()} г : ${entry.water.roundToInt()} г • ${entry.temp}°C • ${entry.time}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                    }
                                }
                            }

                            if (entry.notes.isNotBlank()) {
                                Text("💬 ${entry.notes}", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadJournalEntryToCalculator(entry) },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("↗ В калькулятор", fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteJournalEntry(entry.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CoffeeRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
