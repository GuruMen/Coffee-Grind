package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrewProtocols
import com.example.data.model.ValveState
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeeRose
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.CoffeeGrindViewModel
import kotlin.math.roundToInt

private fun formatMMSS(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun TimerScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timerState.collectAsState()
    val calcState by viewModel.calcState.collectAsState()
    val totalWater = calcState.totalWater

    val protocol = timerState.protocol
    val elapsed = timerState.elapsedSeconds
    val totalSec = protocol.totalSec

    // Calculate step cumulative waters
    var prevWater = 0
    val enrichedSteps = remember(protocol, totalWater) {
        prevWater = 0
        protocol.steps.mapIndexed { idx, st ->
            val cum = (totalWater * st.pct).roundToInt()
            val add = maxOf(0, cum - prevWater)
            prevWater = cum
            Triple(st, cum, add)
        }
    }

    val activeIndex = enrichedSteps.indexOfFirst { (st, _, _) ->
        elapsed >= st.atSec && elapsed < st.endSec
    }.let { if (it >= 0) it else if (elapsed >= totalSec) enrichedSteps.lastIndex else 0 }

    val activeStep = enrichedSteps[activeIndex]

    val progress by animateFloatAsState(
        targetValue = if (totalSec > 0) (elapsed.toFloat() / totalSec).coerceIn(0f, 1f) else 0f,
        label = "timer_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Protocol Selection Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("timer_protocol_card"),
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
                        text = "⏱ Протокол заваривания",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        color = CoffeeTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "${calcState.dose.roundToInt()} г → $totalWater г",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = CoffeeTeal
                        )
                    }
                }

                var dropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = protocol.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        BrewProtocols.ALL.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    viewModel.setTimerProtocol(p)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = protocol.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Clock & Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoffeeAmber),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggles for Sound and Vibro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.toggleTimerSound() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (timerState.soundEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (timerState.soundEnabled) "Звук: ВКЛ" else "Звук: ВЫКЛ", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleTimerVibro() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (timerState.vibroEnabled) CoffeeTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (timerState.vibroEnabled) "Вибро: ВКЛ" else "Вибро: ВЫКЛ", fontSize = 11.5.sp)
                    }
                }

                // Digits Display
                Text(
                    text = formatMMSS(elapsed),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontSize = 58.sp
                    ),
                    color = if (timerState.isFinished) CoffeeGreen else CoffeeAmberLight
                )
                Text(
                    text = "Целевое время: ${formatMMSS(totalSec)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Timer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("timer_start_pause_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerState.isRunning) CoffeeAmber else CoffeeGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (timerState.isRunning) "Пауза" else if (elapsed > 0 && !timerState.isFinished) "Продолжить" else "Старт",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сброс")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сброс")
                    }

                    OutlinedButton(
                        onClick = { viewModel.advanceTimer(15) },
                        modifier = Modifier.width(64.dp).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("+15с", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Active Step Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeTeal.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(CoffeeAmber.copy(alpha = 0.12f), CoffeeGreen.copy(alpha = 0.08f))
                        )
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val (st, cum, add) = activeStep

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CoffeeAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (timerState.isFinished) "✓ Завершено" else "Этап ${activeIndex + 1} из ${enrichedSteps.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CoffeeAmberLight
                        )
                    }
                    Text(
                        text = if (add > 0) "Влейте +$add г → до $cum г" else "Всего $cum г",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = CoffeeTeal
                    )
                }

                // Title with Valve status badge if applicable
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (st.valve) {
                        ValveState.OPEN -> {
                            Surface(
                                color = CoffeeGreen.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeGreen.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "КЛАПАН ОТКРЫТ",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = CoffeeGreen
                                )
                            }
                        }
                        ValveState.CLOSED -> {
                            Surface(
                                color = CoffeeRose.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeRose.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "КЛАПАН ЗАКРЫТ",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = CoffeeRose
                                )
                            }
                        }
                        ValveState.NONE -> {}
                    }
                    Text(
                        text = st.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = st.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = CoffeeAmber,
                    trackColor = CoffeeDarkBorder
                )

                // Next step preview
                val nextStep = enrichedSteps.getOrNull(activeIndex + 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (nextStep != null) {
                            "Далее в ${formatMMSS(nextStep.first.atSec)}: ${nextStep.first.title}"
                        } else {
                            "Финальный этап"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Steps List
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Все шаги протокола (нажмите для перехода):",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            enrichedSteps.forEachIndexed { idx, (st, cum, add) ->
                val isCurrent = idx == activeIndex && !timerState.isFinished
                val isPassed = elapsed >= st.endSec

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.jumpTimerToSecond(st.atSec) },
                    color = if (isCurrent) CoffeeAmber.copy(alpha = 0.18f) else CoffeeDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrent) CoffeeAmber else if (isPassed) CoffeeGreen.copy(alpha = 0.4f) else CoffeeDarkBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatMMSS(st.atSec)}–${formatMMSS(st.endSec)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isCurrent) CoffeeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(82.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (st.valve) {
                                    ValveState.OPEN -> Text("● ", color = CoffeeGreen, fontSize = 10.sp)
                                    ValveState.CLOSED -> Text("● ", color = CoffeeRose, fontSize = 10.sp)
                                    ValveState.NONE -> {}
                                }
                                Text(
                                    text = st.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = st.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = if (add > 0) "+$add г (до $cum)" else "$cum г",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CoffeeTeal,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(90.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
