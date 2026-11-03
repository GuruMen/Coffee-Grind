package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ble.BleConnectionState
import com.example.data.model.ScaleBrandProtocol
import com.example.data.model.ScaleBrewMode
import com.example.data.model.TelemetryPoint
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ScaleScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.scaleState.collectAsState()
    val calcState by viewModel.calcState.collectAsState()

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            viewModel.startBleScaleScan()
        } else {
            // Activate virtual scale fallback if user denies or is on an emulator without BLE
            viewModel.activateVirtualScale()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. BLE CONNECTION & SCALE PROTOCOL SELECTOR CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ble_scale_connection_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (state.connectionState) {
                    BleConnectionState.CONNECTED -> CoffeeGreen
                    BleConnectionState.VIRTUAL_SIMULATOR -> CoffeeTeal
                    BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> CoffeeAmber
                    BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (state.connectionState) {
                                BleConnectionState.CONNECTED -> Icons.Default.BluetoothConnected
                                BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> Icons.Default.BluetoothSearching
                                else -> Icons.Default.Bluetooth
                            },
                            contentDescription = null,
                            tint = when (state.connectionState) {
                                BleConnectionState.CONNECTED -> CoffeeGreen
                                BleConnectionState.VIRTUAL_SIMULATOR -> CoffeeTeal
                                else -> CoffeeAmberLight
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "⚖️ Bluetooth-весы (BeanZen / Кофезавр)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = state.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status Badge
                    val badgeColor = when (state.connectionState) {
                        BleConnectionState.CONNECTED -> CoffeeGreen
                        BleConnectionState.VIRTUAL_SIMULATOR -> CoffeeTeal
                        BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> CoffeeGold
                        BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = when (state.connectionState) {
                                BleConnectionState.CONNECTED -> "BLE ONLINE"
                                BleConnectionState.VIRTUAL_SIMULATOR -> "СИМУЛЯТОР"
                                BleConnectionState.SCANNING -> "ПОИСК..."
                                BleConnectionState.CONNECTING -> "СВЯЗЬ..."
                                BleConnectionState.DISCONNECTED -> "ОФЛАЙН"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor
                        )
                    }
                }

                // Scale Brand Protocol Picker
                var protoExpanded by remember { mutableStateOf(false) }
                Column {
                    Text(
                        text = "Протокол / Модель умных весов (9 семейств):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { protoExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = state.selectedProtocol.title,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = protoExpanded,
                            onDismissRequest = { protoExpanded = false }
                        ) {
                            ScaleBrandProtocol.values().forEach { proto ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(proto.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(proto.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectScaleProtocol(proto)
                                        protoExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Action buttons: Scan BLE, Virtual Scale, Disconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.connectionState == BleConnectionState.SCANNING) {
                        Button(
                            onClick = { viewModel.stopBleScaleScan() },
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeRose),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Стоп поиск", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { permissionLauncher.launch(requiredPermissions) },
                            modifier = Modifier.weight(1.1f).height(42.dp).testTag("scan_ble_scales_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Найти BLE-весы", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.activateVirtualScale() },
                        modifier = Modifier.weight(1.1f).height(42.dp).testTag("activate_virtual_scale_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = CoffeeTeal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Виртуальные весы", fontSize = 11.5.sp, color = CoffeeTeal)
                    }

                    if (state.connectionState != BleConnectionState.DISCONNECTED) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectBleScale() },
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Отключить", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Discovered BLE Scales List
                if (state.discoveredDevices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Найденные Bluetooth-устройства (${state.discoveredDevices.size}):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeAmberLight
                        )
                        state.discoveredDevices.forEach { dev ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.connectBleScale(dev) },
                                color = CoffeeDarkElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "${dev.detectedProtocol.title} • ${dev.address}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${dev.rssi} dBm • Подключить",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CoffeeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. BREW MODE SELECTOR (POUR-OVER / ESPRESSO / DRIP BAG) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Режим профилирования (BeanZen Modes)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScaleBrewMode.values().forEach { mode ->
                        val isSel = state.brewMode == mode
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setScaleBrewMode(mode) },
                            label = { Text(mode.title, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoffeeAmber.copy(alpha = 0.22f),
                                selectedLabelColor = CoffeeAmberLight
                            )
                        )
                    }
                }

                Text(
                    text = state.brewMode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Automation toggles (Auto-Start on First Drop, Auto-Tare, Auto-Stop on Target)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.autoStartOnFlow,
                        onClick = { viewModel.toggleAutoStartOnFlow() },
                        label = { Text("⚡ Авто-старт от 1-й капли", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = state.autoTareOnStart,
                        onClick = { viewModel.toggleAutoTareOnStart() },
                        label = { Text("⚖️ Авто-тара на старте", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = state.autoStopOnTarget,
                        onClick = { viewModel.toggleAutoStopOnTarget() },
                        label = { Text("🛑 Авто-стоп на цели", fontSize = 11.5.sp) }
                    )
                }

                // Dose & Target Weight inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.doseG.toString(),
                        onValueChange = { v -> v.toFloatOrNull()?.let { viewModel.setScaleDose(it) } },
                        label = { Text("Доза кофе (г)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = state.targetWeightG.roundToInt().toString(),
                        onValueChange = { v -> v.toFloatOrNull()?.let { viewModel.setScaleTargetWeight(it) } },
                        label = {
                            Text(
                                if (state.brewMode == ScaleBrewMode.ESPRESSO) "Цель напитка Yield (г)"
                                else "Целевой вес воды (г)"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // --- 3. LIVE TELEMETRY DASHBOARD (WEIGHT, FLOW RATE, BREW RATIO, TIMER) ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("scale_live_dashboard"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoffeeAmber),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(CoffeeAmber.copy(alpha = 0.14f), CoffeeTeal.copy(alpha = 0.08f))
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Big metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weight in Grams
                    Column {
                        Text(
                            text = "ТЕКУЩИЙ ВЕС",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", state.currentWeightG),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 44.sp
                                ),
                                color = CoffeeAmberLight
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "г",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = "Цель: ${state.targetWeightG.roundToInt()} г",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Flow Rate (g/s)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ПОТОК (FLOW)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", state.currentFlowRateGps),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = CoffeeTeal
                        )
                        Text(
                            text = "г/сек (пик ${state.peakFlowRateGps})",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Live Brew Ratio & Timer
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BREW RATIO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "1 : ${state.liveBrewRatio}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = CoffeeGreen
                        )
                        val totalSec = state.elapsedSecondsFloat.toInt()
                        val tenths = state.elapsedDeciSeconds % 10
                        Text(
                            text = String.format(Locale.US, "⏱ %02d:%02d.%d", totalSec / 60, totalSec % 60, tenths),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Target weight progress bar
                val progress = if (state.targetWeightG > 0f) {
                    (state.currentWeightG / state.targetWeightG).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (progress >= 1f) CoffeeGreen else CoffeeAmber,
                    trackColor = CoffeeDarkBorder
                )

                // Drip Bag Step Guide if in Drip Bag mode
                if (state.brewMode == ScaleBrewMode.DRIP_BAG) {
                    val stageText = when {
                        state.currentWeightG < 30f -> "Шаг 1/4 (Блуминг): влейте до 30 г воды и подождите 25 сек"
                        state.currentWeightG < 80f -> "Шаг 2/4 (1-й пролив): плавно долейте воду до 80 г"
                        state.currentWeightG < 130f -> "Шаг 3/4 (2-й пролив): после оседания долейте до 130 г"
                        state.currentWeightG < 180f -> "Шаг 4/4 (Финал): долейте до ${state.targetWeightG.roundToInt()} г и снимите дрип-пакет"
                        else -> "✅ Дрип-пакет готов! Извлеките пакет из чашки."
                    }
                    Surface(
                        color = CoffeeDarkElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🥡 $stageText",
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CoffeeTeal
                        )
                    }
                }

                // Scale Controls: TARE, START/PAUSE, RESET
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.tareScale() },
                        modifier = Modifier.weight(1f).height(46.dp).testTag("scale_tare_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("TARE (0.0 г)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.toggleScaleSessionTimer() },
                        modifier = Modifier.weight(1.3f).height(46.dp).testTag("scale_start_stop_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isTimerRunning) CoffeeAmber else CoffeeGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isTimerRunning) "Пауза" else "Старт",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetScaleSession() },
                        modifier = Modifier.weight(0.9f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сброс", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сброс", fontSize = 12.sp)
                    }
                }

                // Interactive Virtual Pour Simulator Controls (for testing without physical scale or practicing)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoffeeDarkElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Симуляция пролива (тест графика без физических весов):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startVirtualPourSimulation() },
                                modifier = Modifier.weight(1.4f).height(38.dp).testTag("simulate_pour_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isSimulatingPour) CoffeeRose else CoffeeTeal
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (state.isSimulatingPour) "⏹ Стоп авто-пролив" else "▶ Авто-пролив кривой",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.addVirtualWaterDelta(15f) },
                                modifier = Modifier.weight(0.8f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+15 г", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                            }
                            OutlinedButton(
                                onClick = { viewModel.addVirtualWaterDelta(50f) },
                                modifier = Modifier.weight(0.8f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+50 г", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. REAL-TIME DUAL-AXIS EXTRACTION CHART (WEIGHT & FLOW RATE) ---
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
                        text = "📈 График экстракции (Вес и Скорость потока)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Средний: ${state.avgFlowRateGps} г/с",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CoffeeTeal
                    )
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(CoffeeAmber, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Масса напитка / воды (г)", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(CoffeeTeal, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скорость пролива Flow Rate (г/с)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                ExtractionTelemetryGraph(
                    points = state.telemetryPoints,
                    targetWeightG = state.targetWeightG,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CoffeeDarkElevated)
                        .border(1.dp, CoffeeDarkBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )

                // Save to Brew Journal button
                Button(
                    onClick = { viewModel.saveScaleSessionToJournal() },
                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("save_scale_session_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💾 Сохранить пролив с весов в Дневник (${calcState.currentGrinder?.model ?: ""})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExtractionTelemetryGraph(
    points: List<TelemetryPoint>,
    targetWeightG: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (points.size < 2) {
            Text(
                text = "Нажмите «Старт», начните вливание на весах или запустите «▶ Авто-пролив кривой», чтобы построить живой график веса и скорости потока.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                // Draw horizontal grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = h * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val maxTime = points.last().timeSec.coerceAtLeast(10f)
                val maxWeight = maxOf(targetWeightG, points.maxOf { it.weightGrams }, 20f)
                val maxFlow = maxOf(points.maxOf { it.flowRateGps }, 10f)

                val weightPath = Path()
                val flowPath = Path()

                points.forEachIndexed { idx, pt ->
                    val x = (pt.timeSec / maxTime).coerceIn(0f, 1f) * w
                    val yWeight = h - ((pt.weightGrams / maxWeight).coerceIn(0f, 1f) * h)
                    val yFlow = h - ((pt.flowRateGps / maxFlow).coerceIn(0f, 1f) * h)

                    if (idx == 0) {
                        weightPath.moveTo(x, yWeight)
                        flowPath.moveTo(x, yFlow)
                    } else {
                        weightPath.lineTo(x, yWeight)
                        flowPath.lineTo(x, yFlow)
                    }
                }

                // Draw Flow Rate curve (Teal)
                drawPath(
                    path = flowPath,
                    color = CoffeeTeal,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Draw Weight curve (Amber)
                drawPath(
                    path = weightPath,
                    color = CoffeeAmber,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
