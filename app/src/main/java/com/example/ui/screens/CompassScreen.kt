package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrewTimeProfile
import com.example.data.model.GrinderCalculator
import com.example.data.model.TasteProfile
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CoffeeGrindViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CompassScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    val compassState by viewModel.compassState.collectAsState()
    val calcState by viewModel.calcState.collectAsState()
    val grinder = calcState.currentGrinder ?: return
    val method = calcState.currentMethod ?: return

    val curPos = GrinderCalculator.micronsToPosition(calcState.targetMicrons, grinder)
    val curSettingStr = GrinderCalculator.formatPosition(curPos, grinder, calcState.zeroOffset)
    val diag = compassState.diagnosis

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Intro Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🧭 Компас экстракции (Диагностика вкуса)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Отметьте вкус чашки и фактическое время — калькулятор рассчитает точную поправку в делениях вашей кофемолки ${grinder.brand} ${grinder.model} и температуре воды.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Taste Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Что доминирует во вкусе чашки?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                TasteProfile.values().forEach { taste ->
                    val isSelected = compassState.taste == taste
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setCompassTaste(taste) },
                        color = if (isSelected) CoffeeAmber.copy(alpha = 0.2f) else CoffeeDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CoffeeAmber else CoffeeDarkBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = taste.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isSelected) CoffeeAmberLight else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = taste.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Flow Time Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. Как прошёл пролив по времени?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrewTimeProfile.values().forEach { tp ->
                        FilterChip(
                            selected = compassState.timeProfile == tp,
                            onClick = { viewModel.setCompassTimeProfile(tp) },
                            label = { Text(tp.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Фактическое время:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = compassState.actualTimeStr,
                        onValueChange = { viewModel.setCompassActualTime(it) },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // 3. Live Diagnosis and Action Card
        diag?.let { d ->
            val step = if (grinder.step > 0f) grinder.step else 1f
            val newUm = (calcState.targetMicrons + d.deltaUm).coerceIn(40, 1400)
            val newPos = GrinderCalculator.micronsToPosition(newUm, grinder)
            val newSettingStr = GrinderCalculator.formatPosition(newPos, grinder, calcState.zeroOffset)
            val rawDiff = (abs(newPos - curPos) / step).roundToInt()
            val stepsCount = maxOf(if (d.deltaUm != 0) 1 else 0, rawDiff)

            val recTemp = calcState.waterTempRec?.optimalTemp ?: 93
            val newTemp = (recTemp + d.deltaTemp).coerceIn(78, 98)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CoffeeAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = d.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CoffeeAmberLight
                    )
                    Text(
                        text = d.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CoffeeDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🎯 Рекомендация для ${grinder.brand} ${grinder.model}:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CoffeeTeal
                            )

                            if (d.deltaUm != 0) {
                                val dirWord = if (d.deltaUm < 0) "уменьшите помол (мельче)" else "укрупните помол (крупнее)"
                                Text(
                                    text = "• Помол: $dirWord на ~$stepsCount дел./клик — с $curSettingStr до $newSettingStr ($newUm μm, ${if (d.deltaUm > 0) "+" else ""}${d.deltaUm} μm)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "• Помол: текущая настройка $curSettingStr ($newUm μm) идеальна!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoffeeGreen
                                )
                            }

                            if (d.deltaTemp != 0) {
                                val tempWord = if (d.deltaTemp > 0) {
                                    "или поднимите температуру воды на +${d.deltaTemp} °C (до $newTemp °C)"
                                } else {
                                    "или снизьте температуру воды на ${d.deltaTemp} °C (до $newTemp °C)"
                                }
                                Text("• Температура: $tempWord", style = MaterialTheme.typography.bodySmall)
                            }

                            Text(
                                text = "• Совет по технике: ${d.extraAdvice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (d.deltaUm != 0) {
                            Button(
                                onClick = { viewModel.applyCompassRecommendation(d.deltaUm) },
                                modifier = Modifier.weight(1.3f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber)
                            ) {
                                Text("⚡ Применить ($newSettingStr)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.switchTab(AppTab.JOURNAL) },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("📓 В дневник", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
