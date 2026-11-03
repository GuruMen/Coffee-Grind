package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ble.BleConnectionState
import com.example.data.ble.BleScaleClient
import com.example.data.local.CoffeeBeanEntity
import com.example.data.local.GarageSettingEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.model.Altitude
import com.example.data.model.BeanModifiersState
import com.example.data.model.BeanProcess
import com.example.data.model.CoffeeOriginsHelper
import com.example.data.model.BrewMethod
import com.example.data.model.BrewProtocol
import com.example.data.model.BrewProtocols
import com.example.data.model.BrewTimeProfile
import com.example.data.model.Category
import com.example.data.model.DiscoveredBleScale
import com.example.data.model.ExtractionCompassCalculator
import com.example.data.model.ExtractionDiagnosis
import com.example.data.model.Grinder
import com.example.data.model.GrinderCalculator
import com.example.data.model.RoastLevel
import com.example.data.model.ScaleBrandProtocol
import com.example.data.model.ScaleBrewMode
import com.example.data.model.TasteProfile
import com.example.data.model.TelemetryPoint
import com.example.data.model.WaterCalculator
import com.example.data.model.WaterTempCalculator
import com.example.data.model.WaterTempRecommendation
import com.example.data.repository.CoffeeGrindRepository
import com.example.ui.components.TimerAudioAndHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AppTab(val title: String, val icon: String) {
    CALCULATOR("Калькулятор", "☕"),
    SCALE("Весы BLE", "⚖️"),
    TIMER("Таймер", "⏱"),
    COMPASS("Компас", "🧭"),
    CONVERTER("Конвертер", "⇄"),
    JOURNAL("Дневник", "📓"),
    WATER("Вода", "💧"),
    DATABASE("База", "📋")
}

data class CalculatorUiState(
    val currentGrinder: Grinder? = null,
    val currentMethod: BrewMethod? = null,
    val targetMicrons: Int = 638,
    val currentPosition: Float = 24f,
    val exactSweetSpotMode: Boolean = true,
    val brandFilter: String = "ALL",
    val searchQuery: String = "",
    val zeroOffset: Float = 0f,
    val isGarageFavorite: Boolean = false,
    val beanModifiers: BeanModifiersState = BeanModifiersState(),
    val waterTempRec: WaterTempRecommendation? = null,
    val dose: Float = 16f,
    val ratio: Float = 15.5f,
    val quickPicks: List<Pair<String, String>> = emptyList()
) {
    val totalWater: Int get() = (dose * ratio).roundToInt()
}

data class TimerUiState(
    val protocol: BrewProtocol = BrewProtocols.ALL.first(),
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibroEnabled: Boolean = true
)

data class CompassUiState(
    val taste: TasteProfile = TasteProfile.SOUR,
    val timeProfile: BrewTimeProfile = BrewTimeProfile.NORMAL,
    val actualTimeStr: String = "2:35",
    val diagnosis: ExtractionDiagnosis? = null
)

data class ConverterUiState(
    val grinderA: Grinder? = null,
    val grinderB: Grinder? = null,
    val posA: Float = 0f,
    val umA: Int = 0,
    val posB: Float = 0f,
    val displaySettingA: String = "",
    val displaySettingB: String = "",
    val isOutOfRangeB: Boolean = false
)

data class WaterUiState(
    val volMl: Float = 1000f,
    val gh: Int = 68,
    val mgRatio: Int = 60,
    val kh: Int = 40,
    val blendTdsA: Float = 15f,
    val blendTdsB: Float = 220f,
    val blendTargetTds: Float = 95f,
    val blendTotalVol: Float = 1000f
)

data class ScaleUiState(
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val statusMessage: String = "Готов к подключению умных весов (BLE)",
    val connectedScaleName: String? = null,
    val selectedProtocol: ScaleBrandProtocol = ScaleBrandProtocol.FUTULA_KOFEZAVR,
    val discoveredDevices: List<DiscoveredBleScale> = emptyList(),
    val brewMode: ScaleBrewMode = ScaleBrewMode.POUR_OVER,
    val currentWeightG: Float = 0.0f,
    val currentFlowRateGps: Float = 0.0f,
    val peakFlowRateGps: Float = 0.0f,
    val avgFlowRateGps: Float = 0.0f,
    val doseG: Float = 16.0f,
    val targetWeightG: Float = 250.0f,
    val elapsedDeciSeconds: Int = 0, // 0.1s resolution
    val isTimerRunning: Boolean = false,
    val autoStartOnFlow: Boolean = true,
    val autoTareOnStart: Boolean = true,
    val autoStopOnTarget: Boolean = true,
    val isSimulatingPour: Boolean = false,
    val telemetryPoints: List<TelemetryPoint> = emptyList()
) {
    val liveBrewRatio: Float
        get() = if (doseG > 0.1f) ((currentWeightG / doseG) * 10f).roundToInt() / 10f else 0f
    val elapsedSecondsFloat: Float
        get() = elapsedDeciSeconds / 10f
}

class CoffeeGrindViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CoffeeGrindRepository(application)
    val bleScaleClient = BleScaleClient(application)
    private val audioHaptics = TimerAudioAndHaptics(application)

    private val _currentTab = MutableStateFlow(AppTab.CALCULATOR)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    private val _scaleState = MutableStateFlow(ScaleUiState())
    val scaleState: StateFlow<ScaleUiState> = _scaleState.asStateFlow()

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private val _compassState = MutableStateFlow(CompassUiState())
    val compassState: StateFlow<CompassUiState> = _compassState.asStateFlow()

    private val _converterState = MutableStateFlow(ConverterUiState())
    val converterState: StateFlow<ConverterUiState> = _converterState.asStateFlow()

    private val _waterState = MutableStateFlow(WaterUiState())
    val waterState: StateFlow<WaterUiState> = _waterState.asStateFlow()

    private val _journalSearchQuery = MutableStateFlow("")
    val journalSearchQuery: StateFlow<String> = _journalSearchQuery.asStateFlow()

    val journalEntries: StateFlow<List<JournalEntryEntity>> = combine(
        repository.getJournalEntries(),
        _journalSearchQuery
    ) { entries, query ->
        if (query.isBlank()) {
            entries
        } else {
            val q = query.trim().lowercase()
            entries.filter {
                it.coffeeName.lowercase().contains(q) ||
                it.roaster.lowercase().contains(q) ||
                it.grinder.lowercase().contains(q) ||
                it.methodName.lowercase().contains(q) ||
                it.notes.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Coffee Bean Journal State ---
    private val _beanSearchQuery = MutableStateFlow("")
    val beanSearchQuery: StateFlow<String> = _beanSearchQuery.asStateFlow()

    private val _originFilter = MutableStateFlow("ALL")
    val originFilter: StateFlow<String> = _originFilter.asStateFlow()

    private val _inStockOnly = MutableStateFlow(false)
    val inStockOnly: StateFlow<Boolean> = _inStockOnly.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    val coffeeBeans: StateFlow<List<CoffeeBeanEntity>> = combine(
        repository.getCoffeeBeans(),
        _beanSearchQuery,
        _originFilter,
        _inStockOnly,
        _favoritesOnly
    ) { beans, query, origin, inStock, favs ->
        beans.filter { b ->
            val matchQuery = query.isBlank() ||
                b.name.contains(query, ignoreCase = true) ||
                b.roaster.contains(query, ignoreCase = true) ||
                b.origin.contains(query, ignoreCase = true) ||
                b.region.contains(query, ignoreCase = true) ||
                b.variety.contains(query, ignoreCase = true) ||
                b.tastingNotes.contains(query, ignoreCase = true) ||
                b.detailedReview.contains(query, ignoreCase = true)

            val matchOrigin = origin == "ALL" || b.origin.equals(origin, ignoreCase = true)
            val matchStock = !inStock || b.inStock
            val matchFav = !favs || b.isFavorite

            matchQuery && matchOrigin && matchStock && matchFav
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val garageSettings: StateFlow<Map<String, GarageSettingEntity>> = repository.getGarageSettings()
        .combine(MutableStateFlow(Unit)) { list, _ ->
            list.associateBy { it.grinderKey }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private var timerJob: Job? = null
    private var lastBeepStepIdx: Int = -1

    init {
        val initialGrinder = repository.grinders.find { it.brand == "Comandante" && it.model == "C40 MK4" }
            ?: repository.grinders.firstOrNull()
        val initialMethod = repository.brewMethods.find { it.id == "steep_and_release" }
            ?: repository.brewMethods.firstOrNull()

        _calcState.update {
            it.copy(
                currentGrinder = initialGrinder,
                currentMethod = initialMethod,
                quickPicks = repository.quickPicks,
                waterTempRec = if (initialMethod != null) WaterTempCalculator.calculate(initialMethod.id, it.beanModifiers) else null
            )
        }
        syncCalculatorToMethod(initialMethod)

        // Converter initial setup
        val gA = initialGrinder
        val gB = repository.grinders.find { it.brand == "1Zpresso" && it.model == "K-Ultra" }
            ?: repository.grinders.getOrNull(1) ?: initialGrinder
        _converterState.update { it.copy(grinderA = gA, grinderB = gB) }
        updateConverter()

        // Compass initial diagnosis
        updateCompassDiagnosis()

        // Observe garage updates for current grinder
        viewModelScope.launch {
            garageSettings.collect { map ->
                val curG = _calcState.value.currentGrinder
                if (curG != null) {
                    val setting = map[curG.key]
                    val isFav = setting?.isFavorite == true
                    val offset = setting?.zeroOffset ?: 0f
                    _calcState.update { it.copy(isGarageFavorite = isFav, zeroOffset = offset) }
                }
            }
        }

        // Observe BLE scale client flows
        viewModelScope.launch {
            combine(
                bleScaleClient.connectionState,
                bleScaleClient.statusMessage,
                bleScaleClient.connectedScaleName,
                bleScaleClient.selectedProtocol,
                bleScaleClient.discoveredDevices
            ) { conn, msg, name, proto, devices ->
                ScaleClientSnapshot(conn, msg, name, proto, devices)
            }.collect { snap ->
                _scaleState.update {
                    it.copy(
                        connectionState = snap.conn,
                        statusMessage = snap.msg,
                        connectedScaleName = snap.name,
                        selectedProtocol = snap.proto,
                        discoveredDevices = snap.devices
                    )
                }
            }
        }

        viewModelScope.launch {
            var lastWeight = 0f
            var lastTimestampMs = System.currentTimeMillis()
            bleScaleClient.rawWeightGrams.collect { newWeight ->
                val now = System.currentTimeMillis()
                val dtSec = ((now - lastTimestampMs).coerceAtLeast(50)) / 1000f
                val dW = newWeight - lastWeight
                val instantFlow = if (dtSec > 0f && dW > 0.05f) (dW / dtSec).coerceIn(0f, 35f) else 0f
                lastWeight = newWeight
                lastTimestampMs = now

                val st = _scaleState.value
                val smoothedFlow = ((st.currentFlowRateGps * 0.5f + instantFlow * 0.5f) * 10f).roundToInt() / 10f

                // Auto-start timer on first drop / flow (> 0.6g rise)
                if (!st.isTimerRunning && st.autoStartOnFlow && st.elapsedDeciSeconds == 0 && newWeight >= 0.8f && dW > 0.2f) {
                    startScaleSessionTimer(fromAutoFlow = true)
                }

                // Auto-stop timer when target weight is reached
                if (st.isTimerRunning && st.autoStopOnTarget && st.targetWeightG > 5f && newWeight >= st.targetWeightG) {
                    pauseScaleSessionTimer()
                    audioHaptics.playFinishAlert(_timerState.value.soundEnabled, _timerState.value.vibroEnabled)
                }

                _scaleState.update { cur ->
                    val newPeak = maxOf(cur.peakFlowRateGps, smoothedFlow)
                    val avg = if (cur.elapsedSecondsFloat > 0.5f && newWeight > 0f) {
                        ((newWeight / cur.elapsedSecondsFloat) * 10f).roundToInt() / 10f
                    } else cur.avgFlowRateGps
                    cur.copy(
                        currentWeightG = (newWeight * 10f).roundToInt() / 10f,
                        currentFlowRateGps = smoothedFlow,
                        peakFlowRateGps = newPeak,
                        avgFlowRateGps = avg
                    )
                }
            }
        }
    }

    private data class ScaleClientSnapshot(
        val conn: BleConnectionState,
        val msg: String,
        val name: String?,
        val proto: ScaleBrandProtocol,
        val devices: List<DiscoveredBleScale>
    )

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- CALCULATOR ACTIONS ---

    fun selectGrinder(grinder: Grinder) {
        val currentGarage = garageSettings.value[grinder.key]
        _calcState.update {
            it.copy(
                currentGrinder = grinder,
                isGarageFavorite = currentGarage?.isFavorite == true,
                zeroOffset = currentGarage?.zeroOffset ?: 0f
            )
        }
        syncCalculatorToMethod(_calcState.value.currentMethod)
        updateCompassDiagnosis()
    }

    fun selectMethod(method: BrewMethod) {
        _calcState.update {
            it.copy(
                currentMethod = method,
                dose = method.dose,
                ratio = method.ratio,
                waterTempRec = WaterTempCalculator.calculate(method.id, it.beanModifiers)
            )
        }
        syncCalculatorToMethod(method)

        // Also suggest matching timer protocol
        val matchingProtocol = BrewProtocols.ALL.find { it.methodIds.contains(method.id) }
        if (matchingProtocol != null) {
            _timerState.update { it.copy(protocol = matchingProtocol) }
        }
        updateCompassDiagnosis()
    }

    private fun syncCalculatorToMethod(method: BrewMethod?) {
        val g = _calcState.value.currentGrinder ?: return
        val m = method ?: return
        val mr = g.methodRanges[m.id]
        val baseUm = mr?.midUm ?: ((m.minUm + m.maxUm) / 2)
        val modOffset = _calcState.value.beanModifiers.totalMicronOffset
        val newUm = (baseUm + modOffset).coerceIn(40, 1400)
        val newPos = GrinderCalculator.micronsToPosition(newUm, g)
        _calcState.update {
            it.copy(
                targetMicrons = newUm,
                currentPosition = newPos,
                exactSweetSpotMode = (modOffset == 0 && mr != null)
            )
        }
    }

    fun setTargetMicrons(microns: Int) {
        val g = _calcState.value.currentGrinder ?: return
        val cleanUm = microns.coerceIn(40, 1400)
        val pos = GrinderCalculator.micronsToPosition(cleanUm, g)
        _calcState.update {
            it.copy(
                targetMicrons = cleanUm,
                currentPosition = pos,
                exactSweetSpotMode = false
            )
        }
        updateCompassDiagnosis()
    }

    fun setGrinderPosition(pos: Float) {
        val g = _calcState.value.currentGrinder ?: return
        val um = GrinderCalculator.positionToMicrons(pos, g).coerceIn(40, 1400)
        _calcState.update {
            it.copy(
                currentPosition = pos,
                targetMicrons = um,
                exactSweetSpotMode = false
            )
        }
        updateCompassDiagnosis()
    }

    fun stepGrinderPosition(direction: Int) {
        val g = _calcState.value.currentGrinder ?: return
        val step = if (g.step > 0f) g.step else 1f
        val rev = g.minPos > g.maxPos
        val delta = if (rev) -direction * step else direction * step
        val lo = minOf(g.minPos, g.maxPos)
        val hi = maxOf(g.minPos, g.maxPos)
        val next = (_calcState.value.currentPosition + delta).coerceIn(lo, hi)
        setGrinderPosition(next)
    }

    fun setBrandFilter(brand: String) {
        _calcState.update { it.copy(brandFilter = brand, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _calcState.update { it.copy(searchQuery = query) }
    }

    fun toggleCurrentGrinderGarage() {
        val g = _calcState.value.currentGrinder ?: return
        val curFav = _calcState.value.isGarageFavorite
        viewModelScope.launch {
            repository.toggleGarageFavorite(g.key, curFav)
            _calcState.update { it.copy(isGarageFavorite = !curFav) }
        }
    }

    fun updateCurrentZeroOffset(offset: Float) {
        val g = _calcState.value.currentGrinder ?: return
        val clean = offset.coerceIn(-50f, 50f)
        viewModelScope.launch {
            repository.updateZeroOffset(g.key, clean)
            _calcState.update { it.copy(zeroOffset = clean) }
        }
    }

    fun stepZeroOffset(direction: Int) {
        val g = _calcState.value.currentGrinder ?: return
        val step = if (g.step > 0f) g.step else 1f
        val next = _calcState.value.zeroOffset + direction * step
        updateCurrentZeroOffset(next)
    }

    fun setBeanProcess(process: BeanProcess) {
        _calcState.update {
            val newMods = it.beanModifiers.copy(process = process)
            it.copy(
                beanModifiers = newMods,
                waterTempRec = if (it.currentMethod != null) WaterTempCalculator.calculate(it.currentMethod.id, newMods) else null
            )
        }
        reapplyBeanModifiers()
    }

    fun setRoastLevel(roast: RoastLevel) {
        _calcState.update {
            val newMods = it.beanModifiers.copy(roast = roast)
            it.copy(
                beanModifiers = newMods,
                waterTempRec = if (it.currentMethod != null) WaterTempCalculator.calculate(it.currentMethod.id, newMods) else null
            )
        }
        reapplyBeanModifiers()
    }

    fun setAltitude(altitude: Altitude) {
        _calcState.update {
            val newMods = it.beanModifiers.copy(altitude = altitude)
            it.copy(
                beanModifiers = newMods,
                waterTempRec = if (it.currentMethod != null) WaterTempCalculator.calculate(it.currentMethod.id, newMods) else null
            )
        }
        reapplyBeanModifiers()
    }

    fun toggleDecaf() {
        _calcState.update {
            val newMods = it.beanModifiers.copy(isDecaf = !it.beanModifiers.isDecaf)
            it.copy(
                beanModifiers = newMods,
                waterTempRec = if (it.currentMethod != null) WaterTempCalculator.calculate(it.currentMethod.id, newMods) else null
            )
        }
        reapplyBeanModifiers()
    }

    fun resetBeanModifiers() {
        _calcState.update {
            val resetMods = BeanModifiersState()
            it.copy(
                beanModifiers = resetMods,
                waterTempRec = if (it.currentMethod != null) WaterTempCalculator.calculate(it.currentMethod.id, resetMods) else null
            )
        }
        syncCalculatorToMethod(_calcState.value.currentMethod)
    }

    private fun reapplyBeanModifiers() {
        val g = _calcState.value.currentGrinder ?: return
        val m = _calcState.value.currentMethod ?: return
        val mr = g.methodRanges[m.id]
        val baseUm = mr?.midUm ?: ((m.minUm + m.maxUm) / 2)
        val offset = _calcState.value.beanModifiers.totalMicronOffset
        val newUm = (baseUm + offset).coerceIn(40, 1400)
        val pos = GrinderCalculator.micronsToPosition(newUm, g)
        _calcState.update {
            it.copy(
                targetMicrons = newUm,
                currentPosition = pos,
                exactSweetSpotMode = (offset == 0)
            )
        }
        updateCompassDiagnosis()
    }

    fun updateDose(dose: Float) {
        _calcState.update { it.copy(dose = dose.coerceIn(1f, 300f)) }
    }

    fun updateRatio(ratio: Float) {
        _calcState.update { it.copy(ratio = ratio.coerceIn(1f, 35f)) }
    }

    // --- TIMER ACTIONS ---

    fun setTimerProtocol(protocol: BrewProtocol) {
        resetTimer()
        _timerState.update { it.copy(protocol = protocol) }
    }

    fun toggleTimer() {
        if (_timerState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun startTimer() {
        if (_timerState.value.isFinished) {
            _timerState.update { it.copy(elapsedSeconds = 0, isFinished = false) }
            lastBeepStepIdx = -1
        }
        _timerState.update { it.copy(isRunning = true) }

        if (_timerState.value.elapsedSeconds == 0) {
            audioHaptics.playStepAlert(_timerState.value.soundEnabled, _timerState.value.vibroEnabled)
            lastBeepStepIdx = 0
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.isRunning && _timerState.value.elapsedSeconds < _timerState.value.protocol.totalSec) {
                delay(1000)
                _timerState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }

                // Check step alert
                val curSec = _timerState.value.elapsedSeconds
                val stepIdx = _timerState.value.protocol.steps.indexOfFirst { curSec >= it.atSec && curSec < it.endSec }
                if (stepIdx >= 0 && stepIdx != lastBeepStepIdx) {
                    lastBeepStepIdx = stepIdx
                    audioHaptics.playStepAlert(_timerState.value.soundEnabled, _timerState.value.vibroEnabled)
                }

                if (_timerState.value.elapsedSeconds >= _timerState.value.protocol.totalSec) {
                    _timerState.update { it.copy(isRunning = false, isFinished = true) }
                    audioHaptics.playFinishAlert(_timerState.value.soundEnabled, _timerState.value.vibroEnabled)
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        lastBeepStepIdx = -1
        _timerState.update { it.copy(elapsedSeconds = 0, isRunning = false, isFinished = false) }
    }

    fun advanceTimer(seconds: Int) {
        val total = _timerState.value.protocol.totalSec
        val next = (_timerState.value.elapsedSeconds + seconds).coerceIn(0, total)
        _timerState.update { it.copy(elapsedSeconds = next) }
    }

    fun jumpTimerToSecond(sec: Int) {
        val total = _timerState.value.protocol.totalSec
        val next = sec.coerceIn(0, total)
        _timerState.update { it.copy(elapsedSeconds = next) }
    }

    fun toggleTimerSound() {
        _timerState.update {
            val n = !it.soundEnabled
            if (n) audioHaptics.playStepAlert(soundEnabled = true, vibroEnabled = false)
            it.copy(soundEnabled = n)
        }
    }

    fun toggleTimerVibro() {
        _timerState.update { it.copy(vibroEnabled = !it.vibroEnabled) }
    }

    // --- EXTRACTION COMPASS ACTIONS ---

    fun setCompassTaste(taste: TasteProfile) {
        _compassState.update { it.copy(taste = taste) }
        updateCompassDiagnosis()
    }

    fun setCompassTimeProfile(timeProfile: BrewTimeProfile) {
        _compassState.update { it.copy(timeProfile = timeProfile) }
        updateCompassDiagnosis()
    }

    fun setCompassActualTime(timeStr: String) {
        _compassState.update { it.copy(actualTimeStr = timeStr) }
        updateCompassDiagnosis()
    }

    private fun updateCompassDiagnosis() {
        val g = _calcState.value.currentGrinder ?: return
        val taste = _compassState.value.taste
        val timeP = _compassState.value.timeProfile
        val timeStr = _compassState.value.actualTimeStr
        val diag = ExtractionCompassCalculator.diagnose(taste, timeP, timeStr, g, _calcState.value.targetMicrons)
        _compassState.update { it.copy(diagnosis = diag) }
    }

    fun applyCompassRecommendation(deltaUm: Int) {
        val curUm = _calcState.value.targetMicrons
        setTargetMicrons(curUm + deltaUm)
        updateCompassDiagnosis()
        switchTab(AppTab.CALCULATOR)
    }

    // --- CONVERTER ACTIONS ---

    fun setConverterGrinderA(grinder: Grinder) {
        val mr = grinder.methodRanges["v60"] ?: grinder.methodRanges["steep_and_release"]
        val pos = mr?.midPos ?: ((grinder.minPos + grinder.maxPos) / 2)
        _converterState.update { it.copy(grinderA = grinder, posA = pos) }
        updateConverter()
    }

    fun setConverterGrinderB(grinder: Grinder) {
        _converterState.update { it.copy(grinderB = grinder) }
        updateConverter()
    }

    fun setConverterPosA(pos: Float) {
        _converterState.update { it.copy(posA = pos) }
        updateConverter()
    }

    fun stepConverterPosA(direction: Int) {
        val gA = _converterState.value.grinderA ?: return
        val step = if (gA.step > 0f) gA.step else 1f
        val rev = gA.minPos > gA.maxPos
        val delta = if (rev) -direction * step else direction * step
        val lo = minOf(gA.minPos, gA.maxPos)
        val hi = maxOf(gA.minPos, gA.maxPos)
        val next = (_converterState.value.posA + delta).coerceIn(lo, hi)
        setConverterPosA(next)
    }

    fun swapConverterGrinders() {
        val curA = _converterState.value.grinderA ?: return
        val curB = _converterState.value.grinderB ?: return
        val currentUm = _converterState.value.umA
        val newPosA = GrinderCalculator.micronsToPosition(currentUm, curB)
        _converterState.update {
            it.copy(
                grinderA = curB,
                grinderB = curA,
                posA = newPosA
            )
        }
        updateConverter()
    }

    fun setConverterPreset(methodId: String) {
        val gA = _converterState.value.grinderA ?: return
        val mr = gA.methodRanges[methodId]
        val pos = if (mr != null) {
            mr.midPos
        } else {
            val bm = repository.brewMethods.find { it.id == methodId }
            val midUm = if (bm != null) (bm.minUm + bm.maxUm) / 2 else 600
            GrinderCalculator.micronsToPosition(midUm, gA)
        }
        setConverterPosA(pos)
    }

    private fun updateConverter() {
        val gA = _converterState.value.grinderA ?: return
        val gB = _converterState.value.grinderB ?: return
        val posA = _converterState.value.posA
        val umA = GrinderCalculator.positionToMicrons(posA, gA)
        val dispA = GrinderCalculator.formatPosition(posA, gA)

        val outOfRange = (umA < gB.minUm || umA > gB.maxUm)
        val clampedUm = umA.coerceIn(gB.minUm, gB.maxUm)
        val posB = GrinderCalculator.micronsToPosition(clampedUm, gB)
        val dispB = GrinderCalculator.formatPosition(posB, gB)

        _converterState.update {
            it.copy(
                umA = umA,
                posB = posB,
                displaySettingA = dispA,
                displaySettingB = if (outOfRange) "≈ $dispB" else dispB,
                isOutOfRangeB = outOfRange
            )
        }
    }

    // --- WATER ACTIONS ---

    fun setWaterPreset(preset: com.example.data.model.WaterProfilePreset) {
        _waterState.update {
            it.copy(
                gh = preset.gh,
                mgRatio = preset.mgRatio,
                kh = preset.kh
            )
        }
    }

    fun setWaterVol(vol: Float) = _waterState.update { it.copy(volMl = vol.coerceIn(100f, 10000f)) }
    fun setWaterGh(gh: Int) = _waterState.update { it.copy(gh = gh.coerceIn(10, 200)) }
    fun setWaterMgRatio(ratio: Int) = _waterState.update { it.copy(mgRatio = ratio.coerceIn(0, 100)) }
    fun setWaterKh(kh: Int) = _waterState.update { it.copy(kh = kh.coerceIn(5, 150)) }

    fun setBlendTdsA(v: Float) = _waterState.update { it.copy(blendTdsA = v.coerceIn(0f, 1000f)) }
    fun setBlendTdsB(v: Float) = _waterState.update { it.copy(blendTdsB = v.coerceIn(1f, 2000f)) }
    fun setBlendTargetTds(v: Float) = _waterState.update { it.copy(blendTargetTds = v.coerceIn(5f, 500f)) }
    fun setBlendTotalVol(v: Float) = _waterState.update { it.copy(blendTotalVol = v.coerceIn(100f, 10000f)) }

    // --- JOURNAL ACTIONS ---

    fun setJournalSearch(q: String) {
        _journalSearchQuery.value = q
    }

    fun addJournalEntry(
        coffeeName: String,
        roaster: String,
        process: String,
        roastLevel: String,
        methodId: String,
        grinder: String,
        setting: String,
        microns: Int,
        dose: Float,
        water: Float,
        temp: Int,
        time: String,
        rating: Int,
        notes: String
    ) {
        val now = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val methodName = repository.brewMethods.find { it.id == methodId }?.nameRu ?: methodId
        val entry = JournalEntryEntity(
            date = now,
            coffeeName = coffeeName.ifBlank { "Новый лот кофе" },
            roaster = roaster.ifBlank { "Не указан" },
            process = process,
            roastLevel = roastLevel,
            methodId = methodId,
            methodName = methodName,
            grinder = grinder,
            setting = setting,
            microns = microns,
            dose = dose,
            water = water,
            temp = temp,
            time = time,
            rating = rating.coerceIn(1, 5),
            notes = notes
        )
        viewModelScope.launch {
            repository.saveJournalEntry(entry)
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteJournalEntry(id)
        }
    }

    fun loadJournalEntryToCalculator(entry: JournalEntryEntity) {
        val foundG = repository.grinders.find { "${it.brand} ${it.model}".equals(entry.grinder, ignoreCase = true) }
            ?: repository.grinders.find { entry.grinder.contains(it.model, ignoreCase = true) }
        val foundM = repository.brewMethods.find { it.id == entry.methodId }

        if (foundG != null) {
            selectGrinder(foundG)
        }
        if (foundM != null) {
            selectMethod(foundM)
        }
        if (entry.microns in 40..1400) {
            setTargetMicrons(entry.microns)
        }
        if (entry.dose > 0) updateDose(entry.dose)
        if (entry.dose > 0 && entry.water > 0) {
            updateRatio((entry.water / entry.dose * 10f).roundToInt() / 10f)
        }
        switchTab(AppTab.CALCULATOR)
    }

    // --- Coffee Bean Actions ---

    fun setBeanSearch(query: String) {
        _beanSearchQuery.value = query
    }

    fun setOriginFilter(origin: String) {
        _originFilter.value = origin
    }

    fun toggleInStockFilter() {
        _inStockOnly.value = !_inStockOnly.value
    }

    fun toggleFavoritesFilter() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun saveCoffeeBean(bean: CoffeeBeanEntity) {
        viewModelScope.launch {
            if (bean.id == 0L) {
                repository.saveCoffeeBean(bean)
            } else {
                repository.updateCoffeeBean(bean)
            }
        }
    }

    fun deleteCoffeeBean(id: Long) {
        viewModelScope.launch {
            repository.deleteCoffeeBean(id)
        }
    }

    fun toggleBeanFavorite(id: Long, curFav: Boolean) {
        viewModelScope.launch {
            repository.toggleBeanFavorite(id, curFav)
        }
    }

    fun toggleBeanInStock(id: Long, curInStock: Boolean) {
        viewModelScope.launch {
            repository.toggleBeanInStock(id, curInStock)
        }
    }

    fun applyBeanToCalculator(bean: CoffeeBeanEntity) {
        val proc = when {
            bean.process.contains("Натур", ignoreCase = true) || bean.process.contains("Хани", ignoreCase = true) -> BeanProcess.NATURAL
            bean.process.contains("Анаэроб", ignoreCase = true) || bean.process.contains("Инфьюз", ignoreCase = true) -> BeanProcess.ANAEROBIC
            else -> BeanProcess.WASHED
        }
        val roast = when {
            bean.roastLevel.contains("Светл", ignoreCase = true) -> RoastLevel.LIGHT
            bean.roastLevel.contains("Тёмн", ignoreCase = true) -> RoastLevel.DARK
            else -> RoastLevel.MEDIUM
        }
        val isDecaf = bean.process.contains("декаф", ignoreCase = true) || bean.name.contains("декаф", ignoreCase = true)

        setBeanProcess(proc)
        setRoastLevel(roast)
        if (isDecaf != _calcState.value.beanModifiers.isDecaf) {
            toggleDecaf()
        }
        switchTab(AppTab.CALCULATOR)
    }

    // --- BLE SMART SCALE & BEANZEN TELEMETRY ACTIONS ---

    private var scaleTimerJob: Job? = null
    private var virtualPourJob: Job? = null

    fun selectScaleProtocol(protocol: ScaleBrandProtocol) {
        bleScaleClient.selectProtocol(protocol)
    }

    fun startBleScaleScan() {
        bleScaleClient.startScan()
    }

    fun stopBleScaleScan() {
        bleScaleClient.stopScan()
    }

    fun connectBleScale(device: DiscoveredBleScale) {
        bleScaleClient.connectToDevice(device)
    }

    fun disconnectBleScale() {
        stopVirtualPourSimulation()
        bleScaleClient.disconnect()
    }

    fun activateVirtualScale() {
        bleScaleClient.activateVirtualSimulator()
        // Sync dose & target weight from calculator
        _scaleState.update {
            it.copy(
                doseG = _calcState.value.dose,
                targetWeightG = _calcState.value.totalWater.toFloat()
            )
        }
    }

    fun tareScale() {
        bleScaleClient.sendTare()
        _scaleState.update {
            it.copy(
                currentWeightG = 0f,
                currentFlowRateGps = 0f
            )
        }
    }

    fun setScaleBrewMode(mode: ScaleBrewMode) {
        stopVirtualPourSimulation()
        resetScaleSession()
        _scaleState.update {
            it.copy(
                brewMode = mode,
                doseG = mode.defaultDoseG,
                targetWeightG = mode.defaultTargetWeightG
            )
        }
    }

    fun setScaleDose(dose: Float) {
        val clean = dose.coerceIn(1f, 150f)
        _scaleState.update { it.copy(doseG = clean) }
        updateDose(clean)
    }

    fun setScaleTargetWeight(target: Float) {
        val clean = target.coerceIn(5f, 2000f)
        _scaleState.update { it.copy(targetWeightG = clean) }
    }

    fun toggleAutoStartOnFlow() {
        _scaleState.update { it.copy(autoStartOnFlow = !it.autoStartOnFlow) }
    }

    fun toggleAutoTareOnStart() {
        _scaleState.update { it.copy(autoTareOnStart = !it.autoTareOnStart) }
    }

    fun toggleAutoStopOnTarget() {
        _scaleState.update { it.copy(autoStopOnTarget = !it.autoStopOnTarget) }
    }

    fun toggleScaleSessionTimer() {
        if (_scaleState.value.isTimerRunning) {
            pauseScaleSessionTimer()
        } else {
            startScaleSessionTimer(fromAutoFlow = false)
        }
    }

    fun startScaleSessionTimer(fromAutoFlow: Boolean = false) {
        if (_scaleState.value.isTimerRunning) return
        if (!fromAutoFlow && _scaleState.value.autoTareOnStart && _scaleState.value.elapsedDeciSeconds == 0) {
            bleScaleClient.sendTare()
        }
        _scaleState.update { it.copy(isTimerRunning = true) }
        audioHaptics.playStepAlert(_timerState.value.soundEnabled, _timerState.value.vibroEnabled)

        scaleTimerJob?.cancel()
        scaleTimerJob = viewModelScope.launch {
            while (_scaleState.value.isTimerRunning) {
                delay(200) // 5 Hz telemetry recording
                _scaleState.update { cur ->
                    val nextDeci = cur.elapsedDeciSeconds + 2
                    val tSec = nextDeci / 10f
                    val pt = TelemetryPoint(
                        timeSec = tSec,
                        weightGrams = cur.currentWeightG,
                        flowRateGps = cur.currentFlowRateGps
                    )
                    val updatedPoints = (cur.telemetryPoints + pt).takeLast(600)
                    cur.copy(
                        elapsedDeciSeconds = nextDeci,
                        telemetryPoints = updatedPoints
                    )
                }
            }
        }
    }

    fun pauseScaleSessionTimer() {
        scaleTimerJob?.cancel()
        _scaleState.update { it.copy(isTimerRunning = false, currentFlowRateGps = 0f) }
    }

    fun resetScaleSession() {
        scaleTimerJob?.cancel()
        stopVirtualPourSimulation()
        bleScaleClient.updateVirtualWeight(0f)
        _scaleState.update {
            it.copy(
                currentWeightG = 0f,
                currentFlowRateGps = 0f,
                peakFlowRateGps = 0f,
                avgFlowRateGps = 0f,
                elapsedDeciSeconds = 0,
                isTimerRunning = false,
                telemetryPoints = emptyList()
            )
        }
    }

    fun addVirtualWaterDelta(gramsDelta: Float) {
        if (_scaleState.value.connectionState != BleConnectionState.VIRTUAL_SIMULATOR) {
            activateVirtualScale()
        }
        val next = (_scaleState.value.currentWeightG + gramsDelta).coerceIn(0f, 2000f)
        bleScaleClient.updateVirtualWeight(next)
    }

    fun startVirtualPourSimulation() {
        if (_scaleState.value.connectionState != BleConnectionState.VIRTUAL_SIMULATOR) {
            activateVirtualScale()
        }
        if (_scaleState.value.isSimulatingPour) {
            stopVirtualPourSimulation()
            return
        }

        resetScaleSession()
        _scaleState.update { it.copy(isSimulatingPour = true) }
        if (!_scaleState.value.autoStartOnFlow) {
            startScaleSessionTimer(fromAutoFlow = false)
        }

        virtualPourJob?.cancel()
        virtualPourJob = viewModelScope.launch {
            val mode = _scaleState.value.brewMode
            val targetW = _scaleState.value.targetWeightG
            var simWeight = 0f
            var tick = 0

            while (_scaleState.value.isSimulatingPour && simWeight < targetW) {
                delay(200)
                tick++
                val sec = tick * 0.2f

                val flowRateGps = when (mode) {
                    ScaleBrewMode.ESPRESSO -> {
                        // Pre-infusion 0..6s, ramp up 6..18s, steady 18..28s
                        when {
                            sec < 5f -> 0.3f
                            sec < 12f -> 1.4f
                            sec < 24f -> 2.1f
                            else -> 1.5f
                        }
                    }
                    ScaleBrewMode.DRIP_BAG -> {
                        // 3 pulses of pouring with pauses
                        val cycle = (sec % 24f)
                        if (cycle < 10f) 4.2f else 0.0f
                    }
                    ScaleBrewMode.POUR_OVER -> {
                        // Bloom 0..10s, pause 10..30s, active pours
                        when {
                            sec < 8f -> 5.8f
                            sec < 24f -> 0.0f
                            sec < 42f -> 6.4f
                            sec < 52f -> 0.0f
                            sec < 75f -> 5.2f
                            else -> 3.0f
                        }
                    }
                }

                simWeight = (simWeight + flowRateGps * 0.2f).coerceAtMost(targetW)
                bleScaleClient.updateVirtualWeight(simWeight)

                if (simWeight >= targetW) {
                    _scaleState.update { it.copy(isSimulatingPour = false) }
                    pauseScaleSessionTimer()
                    break
                }
            }
        }
    }

    fun stopVirtualPourSimulation() {
        virtualPourJob?.cancel()
        _scaleState.update { it.copy(isSimulatingPour = false) }
    }

    fun saveScaleSessionToJournal(notesExtra: String = "") {
        val sc = _scaleState.value
        val calc = _calcState.value
        val g = calc.currentGrinder
        val m = calc.currentMethod
        val settingStr = if (g != null) {
            val pos = GrinderCalculator.micronsToPosition(calc.targetMicrons, g)
            "${GrinderCalculator.formatPosition(pos, g, calc.zeroOffset)} (${calc.targetMicrons} μm)"
        } else "${calc.targetMicrons} μm"

        val totalSec = (sc.elapsedSecondsFloat).roundToInt()
        val mm = totalSec / 60
        val ss = totalSec % 60
        val timeFormatted = String.format("%d:%02d", mm, ss)

        val autoNote = buildString {
            append("Весы: ${sc.connectedScaleName ?: sc.selectedProtocol.title} • ")
            append("Режим: ${sc.brewMode.title} • ")
            append("Ratio 1:${sc.liveBrewRatio} • ")
            append("Средний поток: ${sc.avgFlowRateGps} г/с (пик ${sc.peakFlowRateGps} г/с)")
            if (notesExtra.isNotBlank()) {
                append(" • $notesExtra")
            }
        }

        addJournalEntry(
            coffeeName = "Заваривание с BLE-весами (${sc.brewMode.title})",
            roaster = sc.selectedProtocol.title,
            process = calc.beanModifiers.process.label,
            roastLevel = calc.beanModifiers.roast.label,
            methodId = when (sc.brewMode) {
                ScaleBrewMode.ESPRESSO -> "espresso"
                ScaleBrewMode.DRIP_BAG -> "pour_over"
                ScaleBrewMode.POUR_OVER -> m?.id ?: "v60"
            },
            grinder = g?.fullName ?: "Не указана",
            setting = settingStr,
            microns = calc.targetMicrons,
            dose = sc.doseG,
            water = sc.currentWeightG.coerceAtLeast(sc.targetWeightG),
            temp = calc.waterTempRec?.optimalTemp ?: 93,
            time = timeFormatted,
            rating = 5,
            notes = autoNote
        )
        switchTab(AppTab.JOURNAL)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        scaleTimerJob?.cancel()
        virtualPourJob?.cancel()
        bleScaleClient.disconnect()
        audioHaptics.release()
    }
}
