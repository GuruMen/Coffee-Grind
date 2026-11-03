package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Grinder
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeeRose
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.CoffeeGrindViewModel

@Composable
fun ConverterScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.converterState.collectAsState()
    val grinderA = state.grinderA ?: return
    val grinderB = state.grinderB ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Конвертер настроек (кофемолка A → B)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Переводит настройку с одной кофемолки на другую по эквивалентному размеру частиц (μm) на основе базы 228 моделей.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Method presets
        Column {
            Text(
                text = "Быстрый пресет метода:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "espresso" to "Эспрессо",
                    "moka_pot" to "Мока",
                    "aeropress" to "Аэропресс",
                    "v60" to "V60",
                    "pour_over" to "Пуровер",
                    "steep_and_release" to "Hario Switch",
                    "french_press" to "Френч-пресс",
                    "cold_brew" to "Колд Брю"
                ).forEach { (id, name) ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.setConverterPreset(id) },
                        label = { Text(name, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Grinder A Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Исходная кофемолка A (Откуда)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CoffeeAmberLight)
                )

                GrinderPickerRow(
                    currentGrinder = grinderA,
                    allGrinders = viewModel.repository.grinders,
                    allBrands = viewModel.repository.brands,
                    onGrinderSelected = { viewModel.setConverterGrinderA(it) }
                )

                // Slider & step controls for A
                val lo = minOf(grinderA.minPos, grinderA.maxPos)
                val hi = maxOf(grinderA.minPos, grinderA.maxPos)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.stepConverterPosA(-1) },
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Мельче")
                    }
                    Slider(
                        value = state.posA.coerceIn(lo, hi),
                        onValueChange = { viewModel.setConverterPosA(it) },
                        valueRange = lo..hi,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = CoffeeAmber, activeTrackColor = CoffeeAmber)
                    )
                    OutlinedButton(
                        onClick = { viewModel.stepConverterPosA(1) },
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Крупнее")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Частицы: ~${state.umA} μm",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.displaySettingA,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = CoffeeAmberLight
                    )
                }
            }
        }

        // Swap Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { viewModel.swapConverterGrinders() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeDarkElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber),
                modifier = Modifier.size(44.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Поменять местами", tint = CoffeeAmberLight)
            }
        }

        // Grinder B Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoffeeGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Целевая кофемолка B (Куда)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CoffeeGreen)
                )

                GrinderPickerRow(
                    currentGrinder = grinderB,
                    allGrinders = viewModel.repository.grinders,
                    allBrands = viewModel.repository.brands,
                    onGrinderSelected = { viewModel.setConverterGrinderB(it) }
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Эквивалентная настройка на модели B:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.displaySettingB} (${grinderB.unit})",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (state.isOutOfRangeB) CoffeeRose else CoffeeGreen
                        )
                        Text(
                            text = if (state.isOutOfRangeB) {
                                "⚠️ Выходит за диапазон ${grinderB.brand} ${grinderB.model} (${grinderB.minUm}–${grinderB.maxUm} μm)"
                            } else {
                                "Размер частиц: ~${state.umA} μm"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isOutOfRangeB) CoffeeRose else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Comparative Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Сравнение настроек по 13 методам",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Метод", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.1f))
                    Text(grinderA.model, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CoffeeAmberLight, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text(grinderB.model, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CoffeeGreen, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                viewModel.repository.brewMethods.forEach { bm ->
                    val sA = grinderA.methods[bm.id] ?: "—"
                    val sB = grinderB.methods[bm.id] ?: "—"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bm.nameRu, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.1f), maxLines = 1)
                        Text(if (sA != "N/A") sA else "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CoffeeAmberLight, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(if (sB != "N/A") sB else "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CoffeeGreen, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GrinderPickerRow(
    currentGrinder: Grinder,
    allGrinders: List<Grinder>,
    allBrands: List<String>,
    onGrinderSelected: (Grinder) -> Unit
) {
    var brandExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { brandExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(currentGrinder.brand, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                allBrands.forEach { b ->
                    DropdownMenuItem(
                        text = { Text(b) },
                        onClick = {
                            val firstInBrand = allGrinders.find { it.brand == b }
                            if (firstInBrand != null) onGrinderSelected(firstInBrand)
                            brandExpanded = false
                        }
                    )
                }
            }
        }

        val brandModels = remember(currentGrinder.brand) {
            allGrinders.filter { it.brand == currentGrinder.brand }
        }

        Box(modifier = Modifier.weight(1.3f)) {
            OutlinedButton(
                onClick = { modelExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(currentGrinder.model, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                brandModels.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g.model) },
                        onClick = {
                            onGrinderSelected(g)
                            modelExpanded = false
                        }
                    )
                }
            }
        }
    }
}
