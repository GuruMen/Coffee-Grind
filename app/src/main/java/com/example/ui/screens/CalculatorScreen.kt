package com.example.ui.screens

import kotlin.math.roundToInt

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Altitude
import com.example.data.model.BeanProcess
import com.example.data.model.Grinder
import com.example.data.model.GrinderCalculator
import com.example.data.model.RoastLevel
import com.example.ui.components.GrinderVisualChart
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGold
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeePurple
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CoffeeGrindViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.calcState.collectAsState()
    val garageMap by viewModel.garageSettings.collectAsState()
    val scrollState = rememberScrollState()

    val grinder = state.currentGrinder ?: return
    val method = state.currentMethod ?: return

    val mr = grinder.methodRanges[method.id]
    val isSupported = mr != null && grinder.methods[method.id] != null && grinder.methods[method.id] != "N/A"
    val rangeText = if (isSupported) {
        GrinderCalculator.formatRangeWithOffset(mr, grinder, state.zeroOffset)
    } else {
        "Вне шкалы"
    }
    val sweetSpotText = if (isSupported && mr != null) {
        "${GrinderCalculator.formatSweetSpotWithOffset(mr, grinder, state.zeroOffset)} (${mr.midUm} μm)"
    } else {
        "Н/Д"
    }

    val currentDisplaySetting = GrinderCalculator.formatPosition(state.currentPosition, grinder, state.zeroOffset)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. GRINDER SELECTOR CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("grinder_selector_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Выбор кофемолки",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = CoffeeAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${viewModel.repository.grinders.size} модели",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CoffeeAmberLight
                        )
                    }
                }

                // Brand and Model selectors
                var brandExpanded by remember { mutableStateOf(false) }
                var modelExpanded by remember { mutableStateOf(false) }

                // Quick search textfield
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().testTag("search_grinder_input"),
                    placeholder = { Text("Поиск: Comandante, K-Ultra, Ode, C3, K7...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Brand dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { brandExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (state.brandFilter == "ALL") "Все бренды" else state.brandFilter,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Все бренды (${viewModel.repository.brands.size})") },
                                onClick = {
                                    viewModel.setBrandFilter("ALL")
                                    brandExpanded = false
                                }
                            )
                            viewModel.repository.brands.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = {
                                        viewModel.setBrandFilter(b)
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Model dropdown
                    val filteredModels = remember(state.brandFilter, state.searchQuery, garageMap) {
                        viewModel.repository.grinders.filter { g ->
                            val matchBrand = state.brandFilter == "ALL" || g.brand == state.brandFilter
                            val matchQuery = state.searchQuery.isBlank() ||
                                g.fullName.contains(state.searchQuery, ignoreCase = true) ||
                                g.unit.contains(state.searchQuery, ignoreCase = true)
                            matchBrand && matchQuery
                        }.sortedByDescending { garageMap[it.key]?.isFavorite == true }
                    }

                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedButton(
                            onClick = { modelExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val star = if (state.isGarageFavorite) "★ " else ""
                            Text(
                                text = "$star${grinder.model}",
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            filteredModels.take(60).forEach { g ->
                                val isFav = garageMap[g.key]?.isFavorite == true
                                val label = if (state.brandFilter == "ALL") "${g.brand} ${g.model}" else g.model
                                DropdownMenuItem(
                                    text = { Text(if (isFav) "★ $label" else label) },
                                    onClick = {
                                        viewModel.selectGrinder(g)
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Quick Chips
                Column {
                    Text(
                        text = "Популярные модели:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.quickPicks.forEach { (b, m) ->
                            val isSel = grinder.brand == b && grinder.model == m
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    val found = viewModel.repository.grinders.find { it.brand == b && it.model == m }
                                    if (found != null) viewModel.selectGrinder(found)
                                },
                                label = { Text("$b $m", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CoffeeAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = CoffeeAmberLight
                                )
                            )
                        }
                    }
                }

                // My Garage & Zero Calibration bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CoffeeDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Мой гараж и калибровка нуля",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { viewModel.toggleCurrentGrinderGarage() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isGarageFavorite) CoffeeGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (state.isGarageFavorite) CoffeeGold else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("garage_star_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (state.isGarageFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isGarageFavorite) "В гараже" else "В гараж",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Garage chips
                        val favorites = garageMap.values.filter { it.isFavorite }
                        if (favorites.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                favorites.forEach { fav ->
                                    val targetG = viewModel.repository.grinders.find { it.key == fav.grinderKey }
                                    if (targetG != null) {
                                        val isCurrent = targetG.idx == grinder.idx
                                        val offTag = if (fav.zeroOffset != 0f) " (${if (fav.zeroOffset > 0) "+" else ""}${fav.zeroOffset})" else ""
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = { viewModel.selectGrinder(targetG) },
                                            label = { Text("★ ${targetG.brand} ${targetG.model}$offTag", fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CoffeeAmber.copy(alpha = 0.25f),
                                                selectedLabelColor = CoffeeAmberLight
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Zero offset adjustment controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Смещение нуля (Zero Offset):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (state.zeroOffset != 0f) {
                                        "Поправка: ${if (state.zeroOffset > 0) "+" else ""}${state.zeroOffset} (${grinder.unit})"
                                    } else {
                                        "Калибровка: 0 (${grinder.unit})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.stepZeroOffset(-1) },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Минус")
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "${if (state.zeroOffset > 0) "+" else ""}${state.zeroOffset}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.stepZeroOffset(1) },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Плюс")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.updateCurrentZeroOffset(0f) },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("0", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (grinder.note.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CoffeeTeal.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeTeal.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CoffeeTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = grinder.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // --- 2. METHOD SELECTOR GRID ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Способ заваривания",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "13 способов",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.repository.brewMethods.forEach { bm ->
                        val isSelected = bm.id == method.id
                        Surface(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectMethod(bm) },
                            color = if (isSelected) CoffeeAmber.copy(alpha = 0.2f) else CoffeeDarkElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CoffeeAmber else CoffeeDarkBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bm.nameRu,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) CoffeeAmberLight else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    if (bm.star) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("★", color = CoffeeGold, fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    text = "${bm.minUm}–${bm.maxUm} μm",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. HERO RESULT DISPLAY ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoffeeAmber),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(CoffeeAmber.copy(alpha = 0.16f), CoffeeTeal.copy(alpha = 0.08f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Method and setting range
                    Column {
                        Text(
                            text = "${method.nameRu} (${method.nameEn})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = rangeText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CoffeeAmberLight
                        )
                        Text(
                            text = "${grinder.unit} • ${grinder.brand} ${grinder.model}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Стартовая точка (Sweet Spot)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = sweetSpotText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = CoffeeTeal
                            )
                            Text(
                                text = "Метод: ${method.minUm}–${method.maxUm} μm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Выбранный помол",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentDisplaySetting,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = CoffeeGreen
                            )
                            Text(
                                text = "${state.targetMicrons} μm",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // --- 4. DIAL-IN SLIDERS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Точная подстройка помола (Dial-In)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Grinder click position slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "На кофемолке: $currentDisplaySetting",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CoffeeGreen
                        )
                        Text(
                            text = "${grinder.minScale}–${grinder.maxScale} (${grinder.unit})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val lo = minOf(grinder.minPos, grinder.maxPos)
                    val hi = maxOf(grinder.minPos, grinder.maxPos)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.stepGrinderPosition(-1) },
                            modifier = Modifier.size(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Мельче")
                        }
                        Slider(
                            value = state.currentPosition.coerceIn(lo, hi),
                            onValueChange = { viewModel.setGrinderPosition(it) },
                            valueRange = lo..hi,
                            modifier = Modifier.weight(1f).testTag("grinder_position_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = CoffeeGreen,
                                activeTrackColor = CoffeeGreen,
                                inactiveTrackColor = CoffeeDarkBorder
                            )
                        )
                        OutlinedButton(
                            onClick = { viewModel.stepGrinderPosition(1) },
                            modifier = Modifier.size(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Крупнее")
                        }
                    }
                }

                // Micron slider (40-1400 μm)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Размер частиц: ${state.targetMicrons} μm",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CoffeeAmberLight
                        )
                        Text(
                            text = "40–1400 μm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = state.targetMicrons.toFloat(),
                        onValueChange = { viewModel.setTargetMicrons(it.roundToInt()) },
                        valueRange = 40f..1400f,
                        modifier = Modifier.fillMaxWidth().testTag("micron_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = CoffeeAmber,
                            activeTrackColor = CoffeeAmber,
                            inactiveTrackColor = CoffeeDarkBorder
                        )
                    )

                    // Category chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        viewModel.repository.categories.forEach { c ->
                            val isAct = state.targetMicrons in c.minUm..c.maxUm
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setTargetMicrons((c.minUm + c.maxUm) / 2) },
                                color = if (isAct) CoffeeAmber.copy(alpha = 0.2f) else CoffeeDarkElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAct) CoffeeAmber else CoffeeDarkBorder
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = c.nameRu,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isAct) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isAct) CoffeeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. BEAN MODIFIERS & WATER TEMP ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Поправки зерна и температура воды",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    val offset = state.beanModifiers.totalMicronOffset
                    Surface(
                        color = if (offset != 0) CoffeeAmber.copy(alpha = 0.2f) else CoffeeDarkElevated,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Смещение: ${if (offset > 0) "+" else ""}$offset μm",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (offset != 0) CoffeeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Process pills
                Column {
                    Text("Способ обработки:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BeanProcess.values().forEach { proc ->
                            FilterChip(
                                selected = state.beanModifiers.process == proc,
                                onClick = { viewModel.setBeanProcess(proc) },
                                label = { Text(proc.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Roast pills
                Column {
                    Text("Степень обжарки:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoastLevel.values().forEach { roast ->
                            FilterChip(
                                selected = state.beanModifiers.roast == roast,
                                onClick = { viewModel.setRoastLevel(roast) },
                                label = { Text(roast.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Altitude & Decaf pills
                Column {
                    Text("Высота и Декаф:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Altitude.values().forEach { alt ->
                            FilterChip(
                                selected = state.beanModifiers.altitude == alt,
                                onClick = { viewModel.setAltitude(alt) },
                                label = { Text(alt.label, fontSize = 12.sp) }
                            )
                        }
                        FilterChip(
                            selected = state.beanModifiers.isDecaf,
                            onClick = { viewModel.toggleDecaf() },
                            label = { Text("☕ Декаф (+35 μm)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoffeePurple.copy(alpha = 0.2f),
                                selectedLabelColor = CoffeePurple
                            )
                        )
                    }
                }

                // Nudge buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setTargetMicrons(state.targetMicrons - 25) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("-25 μm", fontSize = 11.5.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.setTargetMicrons(state.targetMicrons + 25) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("+25 μm", fontSize = 11.5.sp)
                    }
                    Button(
                        onClick = { viewModel.resetBeanModifiers() },
                        modifier = Modifier.weight(1.3f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeDarkElevated),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Сброс Sweet Spot", fontSize = 11.5.sp, color = CoffeeAmberLight)
                    }
                }

                // Recommended water temperature banner
                state.waterTempRec?.let { tempRec ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CoffeeTeal.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeTeal.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Рекомендуемая температура воды: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = tempRec.displayLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = CoffeeTeal
                                )
                            }
                            Text(
                                text = tempRec.reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 6. BREW RATIO & QUICK ACTIONS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Пропорция заваривания (Ratio)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dose input
                    OutlinedTextField(
                        value = state.dose.toString(),
                        onValueChange = { v -> v.toFloatOrNull()?.let { viewModel.updateDose(it) } },
                        label = { Text("Кофе (г)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    // Ratio input
                    OutlinedTextField(
                        value = state.ratio.toString(),
                        onValueChange = { v -> v.toFloatOrNull()?.let { viewModel.updateRatio(it) } },
                        label = { Text("1 : X воды") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Итого воды:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.totalWater} г (мл)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = CoffeeTeal
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.switchTab(AppTab.SCALE) },
                        modifier = Modifier.weight(1.1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber)
                    ) {
                        Text("⚖️ Весы BLE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.switchTab(AppTab.TIMER) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeDarkElevated)
                    ) {
                        Text("⏱ Таймер", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = { viewModel.switchTab(AppTab.COMPASS) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeDarkElevated)
                    ) {
                        Text("🧭 Компас", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // --- 7. VISUAL 13-METHOD CHART ---
        GrinderVisualChart(
            grinder = grinder,
            brewMethods = viewModel.repository.brewMethods,
            selectedMethodId = method.id,
            targetMicrons = state.targetMicrons,
            zeroOffset = state.zeroOffset,
            onMethodSelect = { viewModel.selectMethod(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
