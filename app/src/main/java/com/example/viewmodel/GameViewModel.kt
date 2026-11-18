package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.FormatUtils
import com.example.core.stringsFor
import com.example.data.ai.AiHaigenxRepository
import com.example.data.local.AppDatabase
import com.example.data.local.GamePreferencesRepository
import com.example.data.local.MiningHistoryRepository
import com.example.data.local.MiningRecordEntity
import com.example.data.local.MiningSourceType
import com.example.data.local.PersistedGameData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

private const val TAG = "GameViewModel"

data class FloatingCoinParticle(
    val id: String = UUID.randomUUID().toString(),
    val amountHx1: Double,
    val horizontalBias: Float
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val partialText: String) : AiUiState
    data class Error(val message: String, val lastPrompt: String) : AiUiState
}

data class MiningUiState(
    val balanceHx1: Double = 0.0,
    val energy: Double = 100.0,
    val maxEnergy: Double = 100.0,
    val baseHashrate: Double = 12.5,
    val effectiveHashrate: Double = 12.5,
    val coinsPerSecond: Double = 0.025,
    val isBoostActive: Boolean = false,
    val boostRemainingSeconds: Long = 0L,
    val isRechargingPause: Boolean = false,
    val floatingParticles: List<FloatingCoinParticle> = emptyList()
)

data class MarketUiState(
    val currentPriceUsdt: Double = PersistedGameData.DEFAULT_INITIAL_PRICE_USDT,
    val priceChangePercent: Double = 0.0,
    val isPriceUp: Boolean = true,
    val priceHistory: List<Double> = listOf(PersistedGameData.DEFAULT_INITIAL_PRICE_USDT),
    val usdtVirtualIndex: Double = PersistedGameData.DEFAULT_USDT_INDEX,
    val usdtChangePercent: Double = 0.0,
    val isUsdtUp: Boolean = false,
    val usdtHistory: List<Double> = listOf(PersistedGameData.DEFAULT_USDT_INDEX),
    val tickCounter: Int = 0
)

data class AdBoostDialogState(
    val isVisible: Boolean = false,
    val remainingSeconds: Int = 30
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = GamePreferencesRepository(application.applicationContext)
    private val historyRepo = MiningHistoryRepository(
        AppDatabase.getInstance(application.applicationContext).miningRecordDao()
    )
    private val aiRepo = AiHaigenxRepository()

    private val _language = MutableStateFlow(AppLanguage.VI)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _miningState = MutableStateFlow(MiningUiState())
    val miningState: StateFlow<MiningUiState> = _miningState.asStateFlow()

    private val _marketState = MutableStateFlow(MarketUiState())
    val marketState: StateFlow<MarketUiState> = _marketState.asStateFlow()

    private val _adBoostDialogState = MutableStateFlow(AdBoostDialogState())
    val adBoostDialogState: StateFlow<AdBoostDialogState> = _adBoostDialogState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    val miningHistory: StateFlow<List<MiningRecordEntity>> = historyRepo.recentRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var boostExpiresAtMillis: Long = 0L
    private var accumulatedAutoMineForHistory: Double = 0.0
    private var adCountdownJob: Job? = null
    private var aiRequestJob: Job? = null
    private var manualTapBatchAmount: Double = 0.0
    private var manualTapBatchCount: Int = 0

    init {
        viewModelScope.launch {
            val saved = prefsRepo.gameDataFlow.first()
            _language.value = saved.language

            val now = System.currentTimeMillis()
            val elapsedSecs = ((now - saved.lastActiveMillis) / 1000.0).coerceIn(0.0, 3600.0)
            boostExpiresAtMillis = saved.boostExpiresAtMillis

            val wasBoosted = boostExpiresAtMillis > now
            val baseRate = 12.5
            val passiveYield = elapsedSecs * (if (wasBoosted) 0.05 else 0.025)
            val restoredBalance = saved.balanceHx1 + passiveYield
            val restoredEnergy = (saved.energy + elapsedSecs * 2.5).coerceAtMost(100.0)

            _miningState.value = buildMiningState(
                balance = restoredBalance,
                energy = restoredEnergy,
                nowMillis = now,
                particles = emptyList()
            )

            val initialPrice = saved.simulatedPriceUsdt.coerceAtLeast(0.00000005)
            val initialUsdt = saved.usdtVirtualIndex.coerceAtLeast(15.0)
            _marketState.value = MarketUiState(
                currentPriceUsdt = initialPrice,
                priceHistory = listOf(initialPrice),
                usdtVirtualIndex = initialUsdt,
                usdtHistory = listOf(initialUsdt)
            )

            if (passiveYield >= 0.05) {
                historyRepo.logMining(
                    amountHx1 = passiveYield,
                    sourceType = MiningSourceType.AUTO,
                    hashrateSnapshot = if (wasBoosted) baseRate * 3.0 else baseRate
                )
            }

            ensureWelcomeMessage(saved.language)
            startMiningLoop()
            startMarketLoop()
        }
    }

    private fun ensureWelcomeMessage(lang: AppLanguage) {
        if (_chatMessages.value.isEmpty()) {
            val strings = stringsFor(lang)
            _chatMessages.value = listOf(
                ChatMessage(
                    isUser = false,
                    text = strings.aiWelcomeMessage
                )
            )
        }
    }

    private fun buildMiningState(
        balance: Double,
        energy: Double,
        nowMillis: Long,
        particles: List<FloatingCoinParticle>
    ): MiningUiState {
        val boostRemainingMs = (boostExpiresAtMillis - nowMillis).coerceAtLeast(0L)
        val isBoosted = boostRemainingMs > 0L
        val baseHashrate = 12.5
        val effectiveHashrate = if (isBoosted) baseHashrate * 3.0 else baseHashrate
        val coinsPerSec = if (isBoosted) 0.075 else 0.025
        return MiningUiState(
            balanceHx1 = balance,
            energy = energy.coerceIn(0.0, 100.0),
            maxEnergy = 100.0,
            baseHashrate = baseHashrate,
            effectiveHashrate = effectiveHashrate,
            coinsPerSecond = coinsPerSec,
            isBoostActive = isBoosted,
            boostRemainingSeconds = boostRemainingMs / 1000L,
            isRechargingPause = energy < 4.0,
            floatingParticles = particles
        )
    }

    private fun startMiningLoop() {
        viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(250L)
                tickCount++
                val now = System.currentTimeMillis()
                val current = _miningState.value
                val isBoosted = boostExpiresAtMillis > now
                val coinsPerSec = if (isBoosted) 0.075 else 0.025
                val minedDelta = coinsPerSec * 0.25
                val energyRegen = 2.5 * 0.25

                val newBalance = current.balanceHx1 + minedDelta
                val newEnergy = (current.energy + energyRegen).coerceAtMost(100.0)
                accumulatedAutoMineForHistory += minedDelta

                _miningState.value = buildMiningState(
                    balance = newBalance,
                    energy = newEnergy,
                    nowMillis = now,
                    particles = current.floatingParticles
                )

                // Every 4 seconds (16 ticks), persist state
                if (tickCount % 16 == 0) {
                    prefsRepo.saveMiningSnapshot(
                        balanceHx1 = newBalance,
                        energy = newEnergy,
                        boostExpiresAtMillis = boostExpiresAtMillis,
                        lastActiveMillis = now
                    )
                }

                // Every 15 seconds (60 ticks), log auto mining batch to Room
                if (tickCount % 60 == 0 && accumulatedAutoMineForHistory >= 0.05) {
                    val amountToLog = accumulatedAutoMineForHistory
                    accumulatedAutoMineForHistory = 0.0
                    val source = if (isBoosted) MiningSourceType.BOOST_BONUS else MiningSourceType.AUTO
                    historyRepo.logMining(
                        amountHx1 = amountToLog,
                        sourceType = source,
                        hashrateSnapshot = if (isBoosted) 37.5 else 12.5
                    )
                }
            }
        }
    }

    private fun startMarketLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(4000L)
                val current = _marketState.value
                // Random 10% to 20% swing
                val swingRatio = 0.10 + Random.nextDouble() * 0.10
                val usdtIncreases = Random.nextBoolean()

                val newUsdtIndex: Double
                val newPrice: Double
                val priceChangePct: Double
                val usdtChangePct: Double

                if (usdtIncreases) {
                    // USDT ảo tăng -> HAIGENX1 giảm mạnh
                    val usdtDelta = swingRatio * 0.65
                    newUsdtIndex = (current.usdtVirtualIndex * (1.0 + usdtDelta)).coerceAtMost(500.0)
                    newPrice = (current.currentPriceUsdt * (1.0 - swingRatio)).coerceAtLeast(0.00000005)
                    usdtChangePct = usdtDelta * 100.0
                    priceChangePct = -(swingRatio * 100.0)
                } else {
                    // USDT ảo giảm -> HAIGENX1 tăng mạnh
                    val usdtDelta = swingRatio * 0.65
                    newUsdtIndex = (current.usdtVirtualIndex * (1.0 - usdtDelta)).coerceAtLeast(15.0)
                    newPrice = (current.currentPriceUsdt * (1.0 + swingRatio)).coerceAtMost(0.01)
                    usdtChangePct = -(usdtDelta * 100.0)
                    priceChangePct = swingRatio * 100.0
                }

                val updatedPriceHistory = (current.priceHistory + newPrice).takeLast(28)
                val updatedUsdtHistory = (current.usdtHistory + newUsdtIndex).takeLast(28)

                _marketState.value = MarketUiState(
                    currentPriceUsdt = newPrice,
                    priceChangePercent = priceChangePct,
                    isPriceUp = !usdtIncreases,
                    priceHistory = updatedPriceHistory,
                    usdtVirtualIndex = newUsdtIndex,
                    usdtChangePercent = usdtChangePct,
                    isUsdtUp = usdtIncreases,
                    usdtHistory = updatedUsdtHistory,
                    tickCounter = current.tickCounter + 1
                )

                prefsRepo.saveMarketSnapshot(newPrice, newUsdtIndex)
            }
        }
    }

    fun onMineTap(): Boolean {
        val current = _miningState.value
        val tapEnergyCost = 4.0
        if (current.energy < tapEnergyCost) {
            return false
        }

        val now = System.currentTimeMillis()
        val isBoosted = boostExpiresAtMillis > now
        val tapGain = if (isBoosted) 1.95 else 0.65
        val newBalance = current.balanceHx1 + tapGain
        val newEnergy = (current.energy - tapEnergyCost).coerceAtLeast(0.0)

        val particle = FloatingCoinParticle(
            amountHx1 = tapGain,
            horizontalBias = (Random.nextFloat() * 1.2f) - 0.6f
        )

        _miningState.value = buildMiningState(
            balance = newBalance,
            energy = newEnergy,
            nowMillis = now,
            particles = (current.floatingParticles + particle).takeLast(12)
        )

        // Schedule particle cleanup after animation
        viewModelScope.launch {
            delay(950L)
            _miningState.update { state ->
                state.copy(floatingParticles = state.floatingParticles.filterNot { it.id == particle.id })
            }
        }

        // Batch manual taps into Room every 3 taps or immediately if first
        manualTapBatchAmount += tapGain
        manualTapBatchCount++
        val shouldFlush = manualTapBatchCount >= 3 || newEnergy < tapEnergyCost
        if (shouldFlush) {
            val amount = manualTapBatchAmount
            manualTapBatchAmount = 0.0
            manualTapBatchCount = 0
            val snapshotHashrate = if (isBoosted) 37.5 else 12.5
            viewModelScope.launch {
                historyRepo.logMining(
                    amountHx1 = amount,
                    sourceType = MiningSourceType.MANUAL,
                    hashrateSnapshot = snapshotHashrate
                )
                prefsRepo.saveMiningSnapshot(
                    balanceHx1 = newBalance,
                    energy = newEnergy,
                    boostExpiresAtMillis = boostExpiresAtMillis,
                    lastActiveMillis = now
                )
            }
        }

        return true
    }

    fun startSimulatedAdBoostWait() {
        if (_miningState.value.isBoostActive) return
        adCountdownJob?.cancel()
        _adBoostDialogState.value = AdBoostDialogState(isVisible = true, remainingSeconds = 30)
        adCountdownJob = viewModelScope.launch {
            for (sec in 30 downTo 1) {
                _adBoostDialogState.value = AdBoostDialogState(isVisible = true, remainingSeconds = sec)
                delay(1000L)
            }
            activateBoostFor10Minutes()
            _adBoostDialogState.value = AdBoostDialogState(isVisible = false, remainingSeconds = 0)
        }
    }

    fun cancelSimulatedAdWait() {
        adCountdownJob?.cancel()
        adCountdownJob = null
        _adBoostDialogState.value = AdBoostDialogState(isVisible = false, remainingSeconds = 30)
    }

    private suspend fun activateBoostFor10Minutes() {
        val now = System.currentTimeMillis()
        boostExpiresAtMillis = now + 10 * 60 * 1000L
        val current = _miningState.value
        _miningState.value = buildMiningState(
            balance = current.balanceHx1,
            energy = current.energy,
            nowMillis = now,
            particles = current.floatingParticles
        )
        prefsRepo.saveMiningSnapshot(
            balanceHx1 = current.balanceHx1,
            energy = current.energy,
            boostExpiresAtMillis = boostExpiresAtMillis,
            lastActiveMillis = now
        )
    }

    fun sendAiMessage(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return
        if (_aiUiState.value is AiUiState.Loading || _aiUiState.value is AiUiState.Streaming) return

        val lang = _language.value
        val mining = _miningState.value
        val market = _marketState.value

        val contextSummary = buildString {
            append("- Số dư HAIGENX1: ${FormatUtils.formatHx1(mining.balanceHx1, lang)} HX1\n")
            append("- Hashrate hiện tại: ${FormatUtils.formatHashrate(mining.effectiveHashrate, lang)} H/s ")
            append(if (mining.isBoostActive) "(Đang bật AI Boost 3x còn ${FormatUtils.formatCountdown(mining.boostRemainingSeconds)})\n" else "(Chưa bật AI Boost)\n")
            append("- Năng lượng: ${mining.energy.toInt()}/100\n")
            append("- Giá HAIGENX1/USDT (Sàn 1 - mô phỏng): ${FormatUtils.formatUsdtPrice(market.currentPriceUsdt, lang)} USDT ")
            append("(${FormatUtils.formatPercent(market.priceChangePercent, lang)})\n")
            append("- Chỉ số USDT (ngược chiều): ${FormatUtils.formatHashrate(market.usdtVirtualIndex, lang)} ")
            append("(${FormatUtils.formatPercent(market.usdtChangePercent, lang)})")
        }

        _chatMessages.update { it + ChatMessage(isUser = true, text = trimmed) }
        _aiUiState.value = AiUiState.Loading

        aiRequestJob?.cancel()
        aiRequestJob = viewModelScope.launch {
            val strings = stringsFor(lang)
            try {
                val finalReply = aiRepo.streamReplyWithRetryAndFallback(
                    userPrompt = trimmed,
                    gameContextSummary = contextSummary,
                    language = lang,
                    onWaitingForRetry = {
                        _aiUiState.value = AiUiState.Loading
                    },
                    onStreamingText = { partial ->
                        _aiUiState.value = AiUiState.Streaming(partial)
                    }
                )
                _chatMessages.update {
                    it + ChatMessage(isUser = false, text = finalReply)
                }
                _aiUiState.value = AiUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI HAIGENX1 failed after retries and fallbacks", e)
                _aiUiState.value = AiUiState.Error(
                    message = strings.aiOverloadError,
                    lastPrompt = trimmed
                )
            }
        }
    }

    fun retryLastAiMessage() {
        val err = _aiUiState.value as? AiUiState.Error ?: return
        val promptToRetry = err.lastPrompt
        // Remove the failed user duplicate before resending or resend cleanly
        _chatMessages.update { list ->
            if (list.lastOrNull()?.isUser == true && list.lastOrNull()?.text == promptToRetry) {
                list.dropLast(1)
            } else {
                list
            }
        }
        sendAiMessage(promptToRetry)
    }

    fun setLanguage(newLang: AppLanguage) {
        if (_language.value == newLang) return
        _language.value = newLang
        viewModelScope.launch {
            prefsRepo.saveLanguage(newLang)
        }
        // Refresh welcome message if conversation only had the default welcome message
        if (_chatMessages.value.size <= 1 && _chatMessages.value.firstOrNull()?.isUser == false) {
            _chatMessages.value = listOf(
                ChatMessage(isUser = false, text = stringsFor(newLang).aiWelcomeMessage)
            )
        }
    }

    fun resetGameData() {
        viewModelScope.launch {
            val currentLang = _language.value
            boostExpiresAtMillis = 0L
            accumulatedAutoMineForHistory = 0.0
            manualTapBatchAmount = 0.0
            manualTapBatchCount = 0
            adCountdownJob?.cancel()
            aiRequestJob?.cancel()

            prefsRepo.resetAll(currentLang)
            historyRepo.clearHistory()

            _miningState.value = MiningUiState()
            _marketState.value = MarketUiState()
            _adBoostDialogState.value = AdBoostDialogState()
            _aiUiState.value = AiUiState.Idle
            _chatMessages.value = listOf(
                ChatMessage(isUser = false, text = stringsFor(currentLang).aiWelcomeMessage)
            )
        }
    }
}
