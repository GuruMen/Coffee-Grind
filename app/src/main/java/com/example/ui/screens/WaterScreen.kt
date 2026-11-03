package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WaterCalculator
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeeRose
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.CoffeeGrindViewModel
import kotlin.math.roundToInt

@Composable
fun WaterScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.waterState.collectAsState()

    val mineralResult = WaterCalculator.calculateMinerals(
        volMl = state.volMl,
        gh = state.gh,
        mgRatio = state.mgRatio,
        kh = state.kh
    )

    val blendResult = WaterCalculator.calculateBlend(
        tdsA = state.blendTdsA,
        tdsB = state.blendTdsB,
        targetTds = state.blendTargetTds,
        totalVol = state.blendTotalVol
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mineralization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "💧 Калькулятор минерализации воды",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Рассчитайте капли концентратов Lotus Water или растворы солей Barista Hustle (MgSO₄, CaCl₂, сода) на объём дистиллята / осмоса:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = state.volMl.roundToInt().toString(),
                    onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.setWaterVol(v) } },
                    label = { Text("Объём осмоса (мл)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Presets
                Column {
                    Text("Целевой профиль воды:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WaterCalculator.PRESETS.forEach { p ->
                            FilterChip(
                                selected = state.gh == p.gh && state.kh == p.kh && state.mgRatio == p.mgRatio,
                                onClick = { viewModel.setWaterPreset(p) },
                                label = { Text(p.name, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                // Sliders
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Жёсткость GH (Mg + Ca):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("${state.gh} ppm", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = CoffeeTeal)
                            }
                            Slider(
                                value = state.gh.toFloat(),
                                onValueChange = { viewModel.setWaterGh(it.roundToInt()) },
                                valueRange = 10f..180f,
                                colors = SliderDefaults.colors(thumbColor = CoffeeTeal, activeTrackColor = CoffeeTeal)
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Доля Mg : Ca:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("${state.mgRatio}% Mg / ${100 - state.mgRatio}% Ca", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = CoffeeAmberLight)
                            }
                            Slider(
                                value = state.mgRatio.toFloat(),
                                onValueChange = { viewModel.setWaterMgRatio(it.roundToInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(thumbColor = CoffeeAmber, activeTrackColor = CoffeeAmber)
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Щёлочность KH (буфер):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("${state.kh} ppm", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = CoffeeGreen)
                            }
                            Slider(
                                value = state.kh.toFloat(),
                                onValueChange = { viewModel.setWaterKh(it.roundToInt()) },
                                valueRange = 5f..120f,
                                colors = SliderDefaults.colors(thumbColor = CoffeeGreen, activeTrackColor = CoffeeGreen)
                            )
                        }
                    }
                }

                // Mineral Result Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Итоговый TDS: ~${mineralResult.totalTds} ppm", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = if (mineralResult.inScaWindow) "✓ В окне SCA (50–175)" else "ℹ️ Авторский профиль",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (mineralResult.inScaWindow) CoffeeGreen else CoffeeAmberLight
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = CoffeeDarkElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("💧 Капли Lotus Water:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• Магний: ${mineralResult.lotusMgDrops} кап.", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    Text("• Кальций: ${mineralResult.lotusCaDrops} кап.", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    Text("• Буфер KH: ${mineralResult.lotusKhDrops} кап.", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = CoffeeDarkElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("🧪 Растворы (1000 ppm):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• MgSO₄: ${mineralResult.stockMgMl} мл", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    Text("• CaCl₂: ${mineralResult.stockCaMl} мл", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    Text("• Сода: ${mineralResult.stockKhMl} мл", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Two-Water Blend Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🚰 Смешивание двух вод под целевой TDS",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.blendTdsA.roundToInt().toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.setBlendTdsA(v) } },
                        label = { Text("Вода А (мг/л)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = state.blendTdsB.roundToInt().toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.setBlendTdsB(v) } },
                        label = { Text("Вода Б (мг/л)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.blendTargetTds.roundToInt().toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.setBlendTargetTds(v) } },
                        label = { Text("Целевой TDS (мг/л)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = state.blendTotalVol.roundToInt().toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.setBlendTotalVol(v) } },
                        label = { Text("Итоговый объём (мл)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (blendResult.isValid) {
                            Text(
                                text = "Для получения ${state.blendTotalVol.roundToInt()} мл воды с ${state.blendTargetTds.roundToInt()} мг/л смешайте:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Вода А (${state.blendTdsA.roundToInt()} ppm) — ${blendResult.pctA}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${blendResult.volA} мл (г)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CoffeeAmberLight)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Вода Б (${state.blendTdsB.roundToInt()} ppm) — ${blendResult.pctB}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${blendResult.volB} мл (г)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CoffeeGreen)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = blendResult.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoffeeRose
                            )
                        }
                    }
                }
            }
        }

        // Temperature Matrix Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🌡 Матрица температуры заваривания",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Метод", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.3f))
                    Text("Светлая", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    Text("Средняя", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    Text("Тёмная", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                }

                listOf(
                    listOf("Пуровер V60", "94–96 °C", "91–93 °C", "87–89 °C"),
                    listOf("Hario Switch", "93–95 °C", "90–92 °C", "85–88 °C"),
                    listOf("Аэропресс", "92–95 °C", "85–90 °C", "80–84 °C"),
                    listOf("Эспрессо", "94–96 °C", "92–93 °C", "88–90 °C"),
                    listOf("Френч-пресс", "95–96 °C", "93–94 °C", "89–91 °C")
                ).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(row[0], style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.3f))
                        Text(row[1], style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CoffeeTeal, modifier = Modifier.weight(1f))
                        Text(row[2], style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CoffeeGreen, modifier = Modifier.weight(1f))
                        Text(row[3], style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CoffeeAmberLight, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
