package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrinderCalculator
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CompassScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.ScaleScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.screens.WaterScreen
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeeGrindTheme
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CoffeeGrindViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CoffeeGrindViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var userDarkTheme by remember { mutableStateOf(systemDark) }

            CoffeeGrindTheme(darkTheme = userDarkTheme) {
                MainAppScreen(
                    viewModel = viewModel,
                    isDark = userDarkTheme,
                    onToggleTheme = { userDarkTheme = !userDarkTheme }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: CoffeeGrindViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val calcState by viewModel.calcState.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val beans by viewModel.coffeeBeans.collectAsState()
    val context = LocalContext.current

    // Back handling: if on another tab, return to calculator tab
    BackHandler(enabled = currentTab != AppTab.CALCULATOR) {
        viewModel.switchTab(AppTab.CALCULATOR)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CoffeeAmber.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("☕", fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Coffee Grind Calculator",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "228 моделей • Honest Coffee Guide",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Native Android Share Sheet action
                        IconButton(
                            onClick = {
                                val g = calcState.currentGrinder
                                val m = calcState.currentMethod
                                if (g != null && m != null) {
                                    val pos = GrinderCalculator.micronsToPosition(calcState.targetMicrons, g)
                                    val settingStr = GrinderCalculator.formatPosition(pos, g, calcState.zeroOffset)
                                    val temp = calcState.waterTempRec?.optimalTemp ?: 93
                                    val shareText = "☕ Рецепт кофе:\n" +
                                        "• Метод: ${m.nameRu}\n" +
                                        "• Кофемолка: ${g.brand} ${g.model}\n" +
                                        "• Помол: $settingStr (${calcState.targetMicrons} μm)\n" +
                                        "• Пропорция: ${calcState.dose.toInt()} г : ${calcState.totalWater} г воды (1:${calcState.ratio})\n" +
                                        "• Температура воды: $temp°C\n\n" +
                                        "Калькулятор помола кофе (228 моделей)"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Поделиться рецептом"))
                                }
                            },
                            modifier = Modifier.testTag("share_recipe_top_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться рецептом", tint = CoffeeAmberLight)
                        }

                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Сменить тему",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Top Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = CoffeeDarkElevated,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        // Standard indicator
                    },
                    divider = {}
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        val badgeCount = if (tab == AppTab.JOURNAL) " (${beans.size})" else ""
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.switchTab(tab) },
                            text = {
                                Text(
                                    text = "${tab.icon} ${tab.title}$badgeCount",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.5.sp,
                                    color = if (isSelected) CoffeeAmberLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Sticky summary bar for Calculator tab (matching web UX)
            if (currentTab == AppTab.CALCULATOR) {
                val g = calcState.currentGrinder
                val m = calcState.currentMethod
                if (g != null && m != null) {
                    val mr = g.methodRanges[m.id]
                    val isSupported = mr != null && g.methods[m.id] != null && g.methods[m.id] != "N/A"
                    val rangeText = if (isSupported) GrinderCalculator.formatRangeWithOffset(mr, g, calcState.zeroOffset) else "Вне шкалы"
                    val sweetText = if (isSupported && mr != null) GrinderCalculator.formatSweetSpotWithOffset(mr, g, calcState.zeroOffset) else "Н/Д"

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${g.brand} ${g.model}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = m.nameRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = rangeText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = CoffeeAmberLight
                                    )
                                    Text(
                                        text = "Старт: $sweetText",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = CoffeeTeal
                                    )
                                }

                                Button(
                                    onClick = { viewModel.switchTab(AppTab.TIMER) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Таймер ⏱", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppTab.CALCULATOR -> CalculatorScreen(viewModel = viewModel)
                AppTab.SCALE -> ScaleScreen(viewModel = viewModel)
                AppTab.TIMER -> TimerScreen(viewModel = viewModel)
                AppTab.COMPASS -> CompassScreen(viewModel = viewModel)
                AppTab.CONVERTER -> ConverterScreen(viewModel = viewModel)
                AppTab.JOURNAL -> JournalScreen(viewModel = viewModel)
                AppTab.WATER -> WaterScreen(viewModel = viewModel)
                AppTab.DATABASE -> DatabaseScreen(viewModel = viewModel)
            }
        }
    }
}
