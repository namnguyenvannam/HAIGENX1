package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.FormatUtils
import com.example.core.stringsFor
import com.example.data.ai.AiHaigenxRepository
import com.example.data.local.GamePreferencesRepository
import com.example.data.local.MiningHistoryRepository
import com.example.data.local.MiningRecordEntity
import com.example.data.local.MiningSourceType
import com.example.data.local.PersistedGameData
import com.example.data.local.TradeDirection
import com.example.data.local.TradeHistoryRepository
import com.example.data.local.TradeOutcome
import com.example.data.local.TradeRecordEntity
import com.example.data.remote.CloudSyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.exp
import kotlin.math.ln
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
    val hashrateLevel: Int = 0,
    val tapPowerLevel: Int = 0,
    val energyLevel: Int = 0,
    val multiplierLevel: Int = 0,
    val turboMultiplierLevel: Int = 0,
    val permanentMultiplier: Double = 1.0,
    val baseHashrate: Double = 12.5,
    val effectiveHashrate: Double = 12.5,
    val coinsPerSecond: Double = 0.025,
    val tapGainHx1: Double = 0.65,
    val isBoostActive: Boolean = false,
    val boostRemainingSeconds: Long = 0L,
    val isRechargingPause: Boolean = false,
    val floatingParticles: List<FloatingCoinParticle> = emptyList()
)

data class MarketNewsItem(
    val id: String = UUID.randomUUID().toString(),
    val headlineVi: String,
    val headlineEn: String,
    val isPositive: Boolean,
    val isFakeNews: Boolean,
    val isFakeRevealed: Boolean,
    val isReacting: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    fun headlineFor(language: AppLanguage): String =
        if (language == AppLanguage.VI) headlineVi else headlineEn
}

data class MarketUiState(
    val currentPriceUsdt: Double = PersistedGameData.DEFAULT_INITIAL_PRICE_USDT,
    val priceChangePercent: Double = 0.0,
    val isPriceUp: Boolean = true,
    val priceHistory: List<Double> = listOf(PersistedGameData.DEFAULT_INITIAL_PRICE_USDT),
    val usdtVirtualIndex: Double = PersistedGameData.DEFAULT_USDT_INDEX,
    val usdtChangePercent: Double = 0.0,
    val isUsdtUp: Boolean = false,
    val usdtHistory: List<Double> = listOf(PersistedGameData.DEFAULT_USDT_INDEX),
    val activeNews: MarketNewsItem? = null,
    val nextNewsInSeconds: Int = 35,
    val tickCounter: Int = 0
)

data class ActiveTradePosition(
    val id: String = UUID.randomUUID().toString(),
    val direction: TradeDirection,
    val leverage: Int,
    val entryFeeHx1: Double,
    val entryPriceUsdt: Double,
    val remainingSeconds: Int = 30,
    val totalSeconds: Int = 30,
    val floatingPnlHx1: Double = 0.0
)

data class TradeSettlementEffect(
    val id: String = UUID.randomUUID().toString(),
    val direction: TradeDirection,
    val leverage: Int,
    val outcome: TradeOutcome,
    val netPnlHx1: Double,
    val payoutHx1: Double,
    val entryPriceUsdt: Double,
    val exitPriceUsdt: Double
)

data class TradingUiState(
    val selectedLeverage: Int = 1,
    val activePosition: ActiveTradePosition? = null,
    val lastSettlement: TradeSettlementEffect? = null
)

data class AdBoostDialogState(
    val isVisible: Boolean = false,
    val remainingSeconds: Int = 30
)

class GameViewModel(
    private val currentUserId: String,
    private val displayName: String,
    private val prefsRepo: GamePreferencesRepository,
    private val miningHistoryRepo: MiningHistoryRepository,
    private val tradeHistoryRepo: TradeHistoryRepository,
    private val cloudSyncRepo: CloudSyncRepository,
    private val aiRepo: AiHaigenxRepository = AiHaigenxRepository()
) : ViewModel() {

    private val _language = MutableStateFlow(AppLanguage.VI)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _miningState = MutableStateFlow(MiningUiState())
    val miningState: StateFlow<MiningUiState> = _miningState.asStateFlow()

    private val _marketState = MutableStateFlow(MarketUiState())
    val marketState: StateFlow<MarketUiState> = _marketState.asStateFlow()

    private val _tradingState = MutableStateFlow(TradingUiState())
    val tradingState: StateFlow<TradingUiState> = _tradingState.asStateFlow()

    private val _adBoostDialogState = MutableStateFlow(AdBoostDialogState())
    val adBoostDialogState: StateFlow<AdBoostDialogState> = _adBoostDialogState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow<String?>(null)
    val cloudSyncStatus: StateFlow<String?> = _cloudSyncStatus.asStateFlow()

    private val _participantCount = MutableStateFlow<Long?>(null)
    val participantCount: StateFlow<Long?> = _participantCount.asStateFlow()

    private val _accountDisplayName = MutableStateFlow(
        displayName.trim().ifEmpty { "HAIGENX1 Miner" }
    )
    val accountDisplayName: StateFlow<String> = _accountDisplayName.asStateFlow()

    val miningHistory: StateFlow<List<MiningRecordEntity>> = miningHistoryRepo.recentRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tradeHistory: StateFlow<List<TradeRecordEntity>> = tradeHistoryRepo.recentTrades
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var boostExpiresAtMillis: Long = 0L
    private var hashrateLevel: Int = 0
    private var tapPowerLevel: Int = 0
    private var energyLevel: Int = 0
    private var multiplierLevel: Int = 0
    private var turboMultiplierLevel: Int = 0
    private var participantRegistered: Boolean = false
    private var accumulatedAutoMineForHistory: Double = 0.0
    private var adCountdownJob: Job? = null
    private var aiRequestJob: Job? = null
    private var tradeCountdownJob: Job? = null
    private var tradeSettlementClearJob: Job? = null
    private var manualTapBatchAmount: Double = 0.0
    private var manualTapBatchCount: Int = 0
    private var isCloudProfileReady: Boolean = false

    init {
        viewModelScope.launch {
            val savedLocal = prefsRepo.gameDataFlow.first()
            _language.value = savedLocal.language
            if (savedLocal.customDisplayName.isNotBlank()) {
                _accountDisplayName.value = savedLocal.customDisplayName
            }

            // Hydrate immediately from local cache
            hashrateLevel = savedLocal.hashrateLevel
            tapPowerLevel = savedLocal.tapPowerLevel
            energyLevel = savedLocal.energyLevel
            multiplierLevel = savedLocal.multiplierLevel
            turboMultiplierLevel = savedLocal.turboMultiplierLevel
            boostExpiresAtMillis = savedLocal.boostExpiresAtMillis

            val now = System.currentTimeMillis()
            val localElapsed = ((now - savedLocal.lastActiveMillis) / 1000.0).coerceIn(0.0, 3600.0)
            val localMaxEnergy = MiningUpgrades.maxEnergyForLevel(energyLevel)
            val localRegen = MiningUpgrades.energyRegenPerSecondForLevel(energyLevel)
            val wasLocalBoosted = boostExpiresAtMillis > now
            val localPermMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel, turboMultiplierLevel)
            val localRate = MiningUpgrades.baseCoinsPerSecondForLevel(hashrateLevel) *
                localPermMult *
                if (wasLocalBoosted) 3.0 else 1.0
            var activeBalance = savedLocal.balanceHx1 + localElapsed * localRate
            var activeEnergy = (savedLocal.energy + localElapsed * localRegen).coerceAtMost(localMaxEnergy)
            var activePrice = savedLocal.simulatedPriceUsdt.coerceIn(MIN_PRICE_USDT, MAX_PRICE_USDT)
            var activeUsdtIndex = savedLocal.usdtVirtualIndex.coerceIn(MIN_USDT_INDEX, MAX_USDT_INDEX)

            _miningState.value = buildMiningState(
                balance = activeBalance,
                energy = activeEnergy,
                nowMillis = now,
                particles = emptyList()
            )
            _marketState.value = MarketUiState(
                currentPriceUsdt = activePrice,
                priceHistory = listOf(activePrice),
                usdtVirtualIndex = activeUsdtIndex,
                usdtHistory = listOf(activeUsdtIndex),
                nextNewsInSeconds = Random.nextInt(30, 91)
            )

            // Hydrate / link Firestore profile for currentUserId
            val remoteProfileResult = cloudSyncRepo.fetchUserProfile(currentUserId)
            val remoteProfile = remoteProfileResult.getOrNull()
            if (remoteProfileResult.isSuccess) {
                if (remoteProfile == null) {
                    val initialSnapshot = savedLocal.copy(
                        balanceHx1 = activeBalance,
                        energy = activeEnergy,
                        hashrateLevel = hashrateLevel,
                        tapPowerLevel = tapPowerLevel,
                        energyLevel = energyLevel,
                        multiplierLevel = multiplierLevel,
                        turboMultiplierLevel = turboMultiplierLevel,
                        boostExpiresAtMillis = boostExpiresAtMillis,
                        simulatedPriceUsdt = activePrice,
                        usdtVirtualIndex = activeUsdtIndex,
                        lastActiveMillis = now
                    )
                    cloudSyncRepo.createUserProfile(currentUserId, _accountDisplayName.value, initialSnapshot)
                        .onSuccess { isCloudProfileReady = true }
                        .onFailure { _cloudSyncStatus.value = it.localizedMessage }
                } else {
                    isCloudProfileReady = true
                    if (remoteProfile.displayName.isNotBlank() && savedLocal.customDisplayName.isBlank()) {
                        _accountDisplayName.value = remoteProfile.displayName
                    }
                    // Merge cloud & local cleanly so returning users keep cloud progress
                    val preferCloud = remoteProfile.lastActiveMillis >= savedLocal.lastActiveMillis ||
                        remoteProfile.balanceHx1 > activeBalance
                    if (preferCloud) {
                        hashrateLevel = remoteProfile.hashrateLevel.coerceIn(0, MiningUpgrades.MAX_CORE_UPGRADE_LEVEL)
                        tapPowerLevel = remoteProfile.tapPowerLevel.coerceIn(0, MiningUpgrades.MAX_CORE_UPGRADE_LEVEL)
                        energyLevel = remoteProfile.energyLevel.coerceIn(0, MiningUpgrades.MAX_CORE_UPGRADE_LEVEL)
                        multiplierLevel = remoteProfile.multiplierLevel.coerceIn(0, MiningUpgrades.MAX_OVERCLOCK_LEVEL)
                        turboMultiplierLevel = remoteProfile.turboMultiplierLevel.coerceIn(0, MiningUpgrades.MAX_TURBO_RIG_LEVEL)
                        boostExpiresAtMillis = maxOf(boostExpiresAtMillis, remoteProfile.boostExpiresAtMillis)
                        activeBalance = maxOf(activeBalance, remoteProfile.balanceHx1)
                        val maxE = MiningUpgrades.maxEnergyForLevel(energyLevel)
                        activeEnergy = remoteProfile.energy.coerceIn(0.0, maxE)
                        activePrice = remoteProfile.simulatedPriceUsdt.coerceIn(MIN_PRICE_USDT, MAX_PRICE_USDT)
                        activeUsdtIndex = remoteProfile.usdtVirtualIndex.coerceIn(MIN_USDT_INDEX, MAX_USDT_INDEX)
                        if (remoteProfile.displayName.isNotBlank()) {
                            _accountDisplayName.value = remoteProfile.displayName
                        }

                        _miningState.value = buildMiningState(
                            balance = activeBalance,
                            energy = activeEnergy,
                            nowMillis = System.currentTimeMillis(),
                            particles = emptyList()
                        )
                        _marketState.update { m ->
                            m.copy(
                                currentPriceUsdt = activePrice,
                                priceHistory = listOf(activePrice),
                                usdtVirtualIndex = activeUsdtIndex,
                                usdtHistory = listOf(activeUsdtIndex)
                            )
                        }
                        prefsRepo.applyCloudSnapshot(
                            displayName = _accountDisplayName.value,
                            balanceHx1 = activeBalance,
                            energy = activeEnergy,
                            hashrateLevel = hashrateLevel,
                            tapPowerLevel = tapPowerLevel,
                            energyLevel = energyLevel,
                            multiplierLevel = multiplierLevel,
                            turboMultiplierLevel = turboMultiplierLevel,
                            boostExpiresAtMillis = boostExpiresAtMillis,
                            simulatedPriceUsdt = activePrice,
                            usdtVirtualIndex = activeUsdtIndex,
                            lastActiveMillis = System.currentTimeMillis()
                        )
                    } else {
                        syncProfileToCloudNow()
                    }
                }
            } else {
                _cloudSyncStatus.value = remoteProfileResult.exceptionOrNull()?.localizedMessage
            }

            // Hydrate recent mining records from cloud if local Room is empty
            if (miningHistoryRepo.recentRecords.first().isEmpty()) {
                cloudSyncRepo.fetchRecentMiningRecords(currentUserId).onSuccess { remoteLogs ->
                    remoteLogs.forEach { rec ->
                        val source = MiningSourceType.entries.firstOrNull { it.name == rec.sourceType }
                            ?: MiningSourceType.AUTO
                        miningHistoryRepo.logMining(rec.amountHx1, source, rec.hashrateSnapshot)
                    }
                }
            }

            // Observe real-time cloud trades with two-tier CancellationException handling
            launch {
                cloudSyncRepo.observeTrades(currentUserId)
                    .catch { error ->
                        if (error is CancellationException &&
                            generateSequence<Throwable>(error) { it.cause }.none { it is com.google.firebase.firestore.FirebaseFirestoreException }
                        ) {
                            throw error
                        }
                        Log.w(TAG, "Trade listener error", error)
                    }
                    .collect { remoteTrades ->
                        tradeHistoryRepo.upsertCloudTrades(remoteTrades)
                    }
            }

            ensureWelcomeMessage(savedLocal.language)
            startParticipantsCounterLoop()
            startMiningLoop()
            startMarketEngineLoop()
        }
    }

    private fun ensureWelcomeMessage(lang: AppLanguage) {
        if (_chatMessages.value.isEmpty()) {
            val strings = stringsFor(lang)
            _chatMessages.value = listOf(
                ChatMessage(isUser = false, text = strings.aiWelcomeMessage)
            )
        }
    }

    private fun startParticipantsCounterLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (!participantRegistered) {
                    cloudSyncRepo.ensureParticipantRegisteredOnce(currentUserId)
                        .onSuccess { participantRegistered = true }
                }
                val countResult = cloudSyncRepo.fetchParticipantCount(currentUserId)
                _participantCount.value = countResult.getOrNull()
                delay(PARTICIPANTS_POLL_INTERVAL_MS)
            }
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
        val baseHash = MiningUpgrades.baseHashrateForLevel(hashrateLevel)
        val permanentMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel, turboMultiplierLevel)
        val boostMult = if (isBoosted) 3.0 else 1.0
        val totalMult = permanentMult * boostMult
        val effectiveHash = baseHash * totalMult
        val coinsPerSec = MiningUpgrades.baseCoinsPerSecondForLevel(hashrateLevel) * totalMult
        val tapGain = MiningUpgrades.baseTapGainForLevel(tapPowerLevel) * totalMult
        val maxEnergy = MiningUpgrades.maxEnergyForLevel(energyLevel)

        return MiningUiState(
            balanceHx1 = balance,
            energy = energy.coerceIn(0.0, maxEnergy),
            maxEnergy = maxEnergy,
            hashrateLevel = hashrateLevel,
            tapPowerLevel = tapPowerLevel,
            energyLevel = energyLevel,
            multiplierLevel = multiplierLevel,
            turboMultiplierLevel = turboMultiplierLevel,
            permanentMultiplier = permanentMult,
            baseHashrate = baseHash,
            effectiveHashrate = effectiveHash,
            coinsPerSecond = coinsPerSec,
            tapGainHx1 = tapGain,
            isBoostActive = isBoosted,
            boostRemainingSeconds = boostRemainingMs / 1000L,
            isRechargingPause = energy < TAP_ENERGY_COST,
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
                val permanentMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel, turboMultiplierLevel)
                val totalMult = permanentMult * if (isBoosted) 3.0 else 1.0
                val coinsPerSec = MiningUpgrades.baseCoinsPerSecondForLevel(hashrateLevel) * totalMult
                val minedDelta = coinsPerSec * 0.25
                val regenPerSec = MiningUpgrades.energyRegenPerSecondForLevel(energyLevel)
                val maxE = MiningUpgrades.maxEnergyForLevel(energyLevel)

                val newBalance = current.balanceHx1 + minedDelta
                val newEnergy = (current.energy + regenPerSec * 0.25).coerceAtMost(maxE)
                accumulatedAutoMineForHistory += minedDelta

                _miningState.value = buildMiningState(
                    balance = newBalance,
                    energy = newEnergy,
                    nowMillis = now,
                    particles = current.floatingParticles
                )

                // Every 4 seconds (16 ticks), save locally
                if (tickCount % 16 == 0) {
                    prefsRepo.saveMiningSnapshot(
                        balanceHx1 = newBalance,
                        energy = newEnergy,
                        boostExpiresAtMillis = boostExpiresAtMillis,
                        lastActiveMillis = now
                    )
                }

                // Every 12 seconds (48 ticks), sync profile snapshot to Firestore
                if (tickCount % 48 == 0) {
                    syncProfileToCloudNow()
                }

                // Every 15 seconds (60 ticks), log auto mining batch to Room & Firestore
                if (tickCount % 60 == 0 && accumulatedAutoMineForHistory >= 0.05) {
                    val amountToLog = accumulatedAutoMineForHistory
                    accumulatedAutoMineForHistory = 0.0
                    val source = if (isBoosted) MiningSourceType.BOOST_BONUS else MiningSourceType.AUTO
                    val hashSnap = MiningUpgrades.baseHashrateForLevel(hashrateLevel) * totalMult
                    miningHistoryRepo.logMining(amountToLog, source, hashSnap)
                    if (isCloudProfileReady) {
                        cloudSyncRepo.saveMiningRecord(
                            userId = currentUserId,
                            recordId = UUID.randomUUID().toString(),
                            amountHx1 = amountToLog,
                            sourceType = source,
                            hashrateSnapshot = hashSnap,
                            timestampMillis = now
                        )
                    }
                }
            }
        }
    }

    private fun startMarketEngineLoop() {
        viewModelScope.launch {
            var secondsUntilNews = Random.nextInt(30, 91)
            var idleTickAccumMs = 0L

            while (isActive) {
                delay(1000L)
                secondsUntilNews--
                idleTickAccumMs += 1000L

                if (secondsUntilNews <= 0) {
                    runNewsCatalystWave()
                    secondsUntilNews = Random.nextInt(30, 91)
                    idleTickAccumMs = 0L
                } else {
                    _marketState.update { it.copy(nextNewsInSeconds = secondsUntilNews) }
                    if (idleTickAccumMs >= 3500L) {
                        idleTickAccumMs = 0L
                        applyCalmMarketDriftTick(secondsUntilNews)
                    }
                }
            }
        }
    }

    /**
     * Between news catalysts: ±1% to ±3% gentle oscillation with gradual pull back toward anchor 0.000001 USDT.
     */
    private suspend fun applyCalmMarketDriftTick(nextNewsCountdown: Int) {
        val current = _marketState.value
        val startPrice = current.currentPriceUsdt
        val anchor = PersistedGameData.DEFAULT_INITIAL_PRICE_USDT

        // Pull ~14% in log-space toward anchor 0.000001, plus ±1%..±3% small noise
        val logRatio = ln(anchor / startPrice.coerceAtLeast(MIN_PRICE_USDT))
        val pullFactor = exp(logRatio * 0.14)
        val noiseMagnitude = 0.01 + Random.nextDouble() * 0.02
        val noiseSign = if (Random.nextBoolean()) 1.0 else -1.0
        val noiseFactor = 1.0 + noiseSign * noiseMagnitude

        val newPrice = (startPrice * pullFactor * noiseFactor).coerceIn(MIN_PRICE_USDT, MAX_PRICE_USDT)
        applyPriceTransitionStep(
            newPrice = newPrice,
            prevPrice = startPrice,
            activeNews = current.activeNews,
            nextNewsCountdown = nextNewsCountdown,
            persistSnapshot = true
        )
    }

    /**
     * Executes a 3.5-4.5 second smooth multi-step price reaction when breaking news arrives.
     * Good news: +100%..+200%, Bad news: -50%..-99%, 25% fake news reverses direction
     * and reveals the "Tin giả" badge strictly AFTER the smooth move finishes.
     */
    private suspend fun runNewsCatalystWave() {
        val prevNews = _marketState.value.activeNews
        val template = MarketNewsCatalog.pickRandomTemplate(prevNews?.headlineVi)
        val isFakeNews = Random.nextFloat() < 0.25f
        val effectiveUp = if (isFakeNews) !template.isPositive else template.isPositive

        val startPrice = _marketState.value.currentPriceUsdt
        val targetMultiplier = if (effectiveUp) {
            // +100% to +200% => 2.0x to 3.0x
            2.0 + Random.nextDouble() * 1.0
        } else {
            // -50% to -99% => 0.50x down to 0.01x
            val dropRatio = 0.50 + Random.nextDouble() * 0.49
            (1.0 - dropRatio)
        }
        val targetPrice = (startPrice * targetMultiplier).coerceIn(MIN_PRICE_USDT, MAX_PRICE_USDT)

        val newsDuringReaction = MarketNewsItem(
            headlineVi = template.headlineVi,
            headlineEn = template.headlineEn,
            isPositive = template.isPositive,
            isFakeNews = isFakeNews,
            isFakeRevealed = false,
            isReacting = true
        )

        _marketState.update {
            it.copy(
                activeNews = newsDuringReaction,
                nextNewsInSeconds = 0
            )
        }

        // Smooth 4.0s transition in 10 sub-steps (within 3-5 seconds requirement)
        val steps = 10
        val stepDelayMs = 400L
        val logStart = ln(startPrice.coerceAtLeast(MIN_PRICE_USDT))
        val logEnd = ln(targetPrice.coerceAtLeast(MIN_PRICE_USDT))

        for (step in 1..steps) {
            delay(stepDelayMs)
            val fraction = step.toDouble() / steps.toDouble()
            // Ease-in-out cubic progress for organic smooth movement
            val eased = if (fraction < 0.5) {
                4.0 * fraction * fraction * fraction
            } else {
                1.0 - (-2.0 * fraction + 2.0).let { it * it * it } / 2.0
            }
            val stepPrice = exp(logStart + (logEnd - logStart) * eased)
                .coerceIn(MIN_PRICE_USDT, MAX_PRICE_USDT)
            val prevStepPrice = _marketState.value.currentPriceUsdt
            val isLast = step == steps

            val updatedNews = newsDuringReaction.copy(
                isReacting = !isLast,
                isFakeRevealed = isLast && isFakeNews
            )

            applyPriceTransitionStep(
                newPrice = stepPrice,
                prevPrice = prevStepPrice,
                activeNews = updatedNews,
                nextNewsCountdown = 0,
                persistSnapshot = isLast
            )
        }
    }

    private suspend fun applyPriceTransitionStep(
        newPrice: Double,
        prevPrice: Double,
        activeNews: MarketNewsItem?,
        nextNewsCountdown: Int,
        persistSnapshot: Boolean
    ) {
        val current = _marketState.value
        val priceDeltaRatio = if (prevPrice > 0.0) (newPrice - prevPrice) / prevPrice else 0.0
        val pricePct = priceDeltaRatio * 100.0
        val isPriceUp = newPrice >= prevPrice

        // Inverse USDT index reacts inversely to HAIGENX1 price movement
        val usdtDeltaRatio = -(priceDeltaRatio * 0.45).coerceIn(-0.45, 0.75)
        val newUsdtIndex = (current.usdtVirtualIndex * (1.0 + usdtDeltaRatio))
            .coerceIn(MIN_USDT_INDEX, MAX_USDT_INDEX)
        val usdtPct = usdtDeltaRatio * 100.0
        val isUsdtUp = newUsdtIndex >= current.usdtVirtualIndex

        val updatedPriceHistory = (current.priceHistory + newPrice).takeLast(32)
        val updatedUsdtHistory = (current.usdtHistory + newUsdtIndex).takeLast(32)

        _marketState.value = MarketUiState(
            currentPriceUsdt = newPrice,
            priceChangePercent = pricePct,
            isPriceUp = isPriceUp,
            priceHistory = updatedPriceHistory,
            usdtVirtualIndex = newUsdtIndex,
            usdtChangePercent = usdtPct,
            isUsdtUp = isUsdtUp,
            usdtHistory = updatedUsdtHistory,
            activeNews = activeNews,
            nextNewsInSeconds = nextNewsCountdown,
            tickCounter = current.tickCounter + 1
        )

        refreshActiveTradeFloatingPnl(newPrice)

        if (persistSnapshot) {
            prefsRepo.saveMarketSnapshot(newPrice, newUsdtIndex)
        }
    }

    private fun refreshActiveTradeFloatingPnl(livePrice: Double) {
        _tradingState.update { state ->
            val pos = state.activePosition ?: return@update state
            val pnl = computeFloatingPnl(
                direction = pos.direction,
                leverage = pos.leverage,
                entryPrice = pos.entryPriceUsdt,
                currentPrice = livePrice
            )
            state.copy(activePosition = pos.copy(floatingPnlHx1 = pnl))
        }
    }

    private fun computeFloatingPnl(
        direction: TradeDirection,
        leverage: Int,
        entryPrice: Double,
        currentPrice: Double
    ): Double {
        return when {
            currentPrice == entryPrice -> 0.0
            direction == TradeDirection.BUY && currentPrice > entryPrice -> 8.0 * leverage
            direction == TradeDirection.SHORT && currentPrice < entryPrice -> 8.0 * leverage
            else -> -(8.0 * leverage)
        }
    }

    fun selectLeverage(leverage: Int) {
        val clamped = leverage.coerceIn(1, 10)
        _tradingState.update { it.copy(selectedLeverage = clamped) }
    }

    fun openTrade(direction: TradeDirection): Boolean {
        if (_tradingState.value.activePosition != null) return false
        val leverage = _tradingState.value.selectedLeverage.coerceIn(1, 10)
        val fee = 10.0 * leverage
        val mining = _miningState.value
        if (mining.balanceHx1 < fee) return false

        val now = System.currentTimeMillis()
        val entryPrice = _marketState.value.currentPriceUsdt
        val newBalance = (mining.balanceHx1 - fee).coerceAtLeast(0.0)

        _miningState.value = buildMiningState(
            balance = newBalance,
            energy = mining.energy,
            nowMillis = now,
            particles = mining.floatingParticles
        )

        val position = ActiveTradePosition(
            direction = direction,
            leverage = leverage,
            entryFeeHx1 = fee,
            entryPriceUsdt = entryPrice,
            remainingSeconds = 30,
            totalSeconds = 30,
            floatingPnlHx1 = 0.0
        )

        _tradingState.update {
            it.copy(activePosition = position, lastSettlement = null)
        }

        viewModelScope.launch {
            prefsRepo.saveMiningSnapshot(
                balanceHx1 = newBalance,
                energy = mining.energy,
                boostExpiresAtMillis = boostExpiresAtMillis,
                lastActiveMillis = now
            )
            syncProfileToCloudNow()
        }

        tradeCountdownJob?.cancel()
        tradeCountdownJob = viewModelScope.launch {
            for (sec in 29 downTo 0) {
                delay(1000L)
                val livePrice = _marketState.value.currentPriceUsdt
                val active = _tradingState.value.activePosition ?: return@launch
                if (sec > 0) {
                    val floating = computeFloatingPnl(
                        direction = active.direction,
                        leverage = active.leverage,
                        entryPrice = active.entryPriceUsdt,
                        currentPrice = livePrice
                    )
                    _tradingState.update { st ->
                        st.copy(
                            activePosition = active.copy(
                                remainingSeconds = sec,
                                floatingPnlHx1 = floating
                            )
                        )
                    }
                } else {
                    settleActivePosition(active, livePrice)
                }
            }
        }
        return true
    }

    private suspend fun settleActivePosition(position: ActiveTradePosition, exitPrice: Double) {
        val outcome = when {
            exitPrice == position.entryPriceUsdt -> TradeOutcome.TIE
            position.direction == TradeDirection.BUY && exitPrice > position.entryPriceUsdt -> TradeOutcome.WIN
            position.direction == TradeDirection.SHORT && exitPrice < position.entryPriceUsdt -> TradeOutcome.WIN
            else -> TradeOutcome.LOSS
        }

        val payoutHx1 = when (outcome) {
            TradeOutcome.WIN -> 18.0 * position.leverage
            TradeOutcome.LOSS -> 2.0 * position.leverage
            TradeOutcome.TIE -> 10.0 * position.leverage
        }
        val netPnlHx1 = payoutHx1 - position.entryFeeHx1
        val now = System.currentTimeMillis()
        val currentMining = _miningState.value
        val updatedBalance = (currentMining.balanceHx1 + payoutHx1).coerceAtLeast(0.0)

        _miningState.value = buildMiningState(
            balance = updatedBalance,
            energy = currentMining.energy,
            nowMillis = now,
            particles = currentMining.floatingParticles
        )

        val settlement = TradeSettlementEffect(
            direction = position.direction,
            leverage = position.leverage,
            outcome = outcome,
            netPnlHx1 = netPnlHx1,
            payoutHx1 = payoutHx1,
            entryPriceUsdt = position.entryPriceUsdt,
            exitPriceUsdt = exitPrice
        )

        _tradingState.update {
            it.copy(
                activePosition = null,
                lastSettlement = settlement
            )
        }

        // Auto-clear settlement banner after 4.5s
        tradeSettlementClearJob?.cancel()
        tradeSettlementClearJob = viewModelScope.launch {
            delay(4500L)
            _tradingState.update { st ->
                if (st.lastSettlement?.id == settlement.id) st.copy(lastSettlement = null) else st
            }
        }

        val tradeRecord = TradeRecordEntity(
            id = position.id,
            direction = position.direction.name,
            leverage = position.leverage,
            entryFeeHx1 = position.entryFeeHx1,
            entryPriceUsdt = position.entryPriceUsdt,
            exitPriceUsdt = exitPrice,
            outcome = outcome.name,
            payoutHx1 = payoutHx1,
            netPnlHx1 = netPnlHx1,
            timestampMillis = now
        )

        tradeHistoryRepo.logTrade(tradeRecord)
        prefsRepo.saveMiningSnapshot(
            balanceHx1 = updatedBalance,
            energy = currentMining.energy,
            boostExpiresAtMillis = boostExpiresAtMillis,
            lastActiveMillis = now
        )

        if (isCloudProfileReady) {
            cloudSyncRepo.saveTradeRecord(currentUserId, tradeRecord)
            syncProfileToCloudNow()
        }
    }

    fun dismissTradeSettlementBanner() {
        _tradingState.update { it.copy(lastSettlement = null) }
    }

    fun getUpgradeItems(strings: com.example.core.AppStrings): List<UpgradeItemUi> {
        val bal = _miningState.value.balanceHx1
        val lang = _language.value
        val currPermMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel, turboMultiplierLevel)
        val nextOverclockMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel + 1, turboMultiplierLevel)
        val nextTurboMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel, turboMultiplierLevel + 1)

        return listOf(
            buildUpgradeItem(
                type = UpgradeType.OVERCLOCK_MULTIPLIER,
                level = multiplierLevel,
                balance = bal,
                currentStat = "×${FormatUtils.formatHashrate(currPermMult, lang)}",
                nextStat = "×${FormatUtils.formatHashrate(nextOverclockMult, lang)}"
            ),
            buildUpgradeItem(
                type = UpgradeType.QUANTUM_TURBO_RIG,
                level = turboMultiplierLevel,
                balance = bal,
                currentStat = "×${FormatUtils.formatHashrate(currPermMult, lang)}",
                nextStat = "×${FormatUtils.formatHashrate(nextTurboMult, lang)}"
            ),
            buildUpgradeItem(
                type = UpgradeType.HASHRATE_CORE,
                level = hashrateLevel,
                balance = bal,
                currentStat = "${FormatUtils.formatHashrate(MiningUpgrades.baseHashrateForLevel(hashrateLevel), lang)} H/s",
                nextStat = "${FormatUtils.formatHashrate(MiningUpgrades.baseHashrateForLevel(hashrateLevel + 1), lang)} H/s"
            ),
            buildUpgradeItem(
                type = UpgradeType.TAP_DRILL,
                level = tapPowerLevel,
                balance = bal,
                currentStat = "+${FormatUtils.formatHx1(MiningUpgrades.baseTapGainForLevel(tapPowerLevel), lang, 2)} HX1",
                nextStat = "+${FormatUtils.formatHx1(MiningUpgrades.baseTapGainForLevel(tapPowerLevel + 1), lang, 2)} HX1"
            ),
            buildUpgradeItem(
                type = UpgradeType.ENERGY_CELL,
                level = energyLevel,
                balance = bal,
                currentStat = "${MiningUpgrades.maxEnergyForLevel(energyLevel).toInt()}⚡",
                nextStat = "${MiningUpgrades.maxEnergyForLevel(energyLevel + 1).toInt()}⚡"
            )
        )
    }

    private fun buildUpgradeItem(
        type: UpgradeType,
        level: Int,
        balance: Double,
        currentStat: String,
        nextStat: String
    ): UpgradeItemUi {
        val maxLevel = MiningUpgrades.maxLevelFor(type)
        val maxed = level >= maxLevel
        val cost = MiningUpgrades.costForNextLevel(type, level)
        return UpgradeItemUi(
            type = type,
            level = level,
            maxLevel = maxLevel,
            nextCostHx1 = cost,
            canAfford = !maxed && balance >= cost,
            isMaxed = maxed,
            currentStatText = currentStat,
            nextStatText = nextStat
        )
    }

    fun purchaseUpgrade(type: UpgradeType): Boolean {
        val currentLevel = when (type) {
            UpgradeType.OVERCLOCK_MULTIPLIER -> multiplierLevel
            UpgradeType.QUANTUM_TURBO_RIG -> turboMultiplierLevel
            UpgradeType.HASHRATE_CORE -> hashrateLevel
            UpgradeType.TAP_DRILL -> tapPowerLevel
            UpgradeType.ENERGY_CELL -> energyLevel
        }
        if (currentLevel >= MiningUpgrades.maxLevelFor(type)) return false

        val cost = MiningUpgrades.costForNextLevel(type, currentLevel)
        val current = _miningState.value
        if (current.balanceHx1 < cost) return false

        when (type) {
            UpgradeType.OVERCLOCK_MULTIPLIER -> multiplierLevel++
            UpgradeType.QUANTUM_TURBO_RIG -> turboMultiplierLevel++
            UpgradeType.HASHRATE_CORE -> hashrateLevel++
            UpgradeType.TAP_DRILL -> tapPowerLevel++
            UpgradeType.ENERGY_CELL -> energyLevel++
        }

        val now = System.currentTimeMillis()
        val newBalance = (current.balanceHx1 - cost).coerceAtLeast(0.0)
        val newEnergy = if (type == UpgradeType.ENERGY_CELL) {
            MiningUpgrades.maxEnergyForLevel(energyLevel)
        } else {
            current.energy
        }

        _miningState.value = buildMiningState(
            balance = newBalance,
            energy = newEnergy,
            nowMillis = now,
            particles = current.floatingParticles
        )

        viewModelScope.launch {
            prefsRepo.saveUpgradesAndBalance(
                balanceHx1 = newBalance,
                energy = newEnergy,
                hashrateLevel = hashrateLevel,
                tapPowerLevel = tapPowerLevel,
                energyLevel = energyLevel,
                multiplierLevel = multiplierLevel,
                turboMultiplierLevel = turboMultiplierLevel,
                lastActiveMillis = now
            )
            syncProfileToCloudNow()
        }
        return true
    }

    fun updateDisplayName(newName: String) {
        val cleaned = newName.trim().take(80)
        if (cleaned.isEmpty()) return
        _accountDisplayName.value = cleaned
        viewModelScope.launch {
            prefsRepo.saveCustomDisplayName(cleaned)
            if (isCloudProfileReady) {
                cloudSyncRepo.updateDisplayName(currentUserId, cleaned)
            }
        }
    }

    fun onMineTap(): Boolean {
        val current = _miningState.value
        if (current.energy < TAP_ENERGY_COST) {
            return false
        }

        val now = System.currentTimeMillis()
        val isBoosted = boostExpiresAtMillis > now
        val permMult = MiningUpgrades.totalPermanentMultiplier(multiplierLevel, turboMultiplierLevel)
        val totalMult = permMult * if (isBoosted) 3.0 else 1.0
        val tapGain = MiningUpgrades.baseTapGainForLevel(tapPowerLevel) * totalMult
        val newBalance = current.balanceHx1 + tapGain
        val newEnergy = (current.energy - TAP_ENERGY_COST).coerceAtLeast(0.0)

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

        viewModelScope.launch {
            delay(950L)
            _miningState.update { state ->
                state.copy(floatingParticles = state.floatingParticles.filterNot { it.id == particle.id })
            }
        }

        manualTapBatchAmount += tapGain
        manualTapBatchCount++
        val shouldFlush = manualTapBatchCount >= 3 || newEnergy < TAP_ENERGY_COST
        if (shouldFlush) {
            val amount = manualTapBatchAmount
            manualTapBatchAmount = 0.0
            manualTapBatchCount = 0
            val snapshotHashrate = MiningUpgrades.baseHashrateForLevel(hashrateLevel) * totalMult
            viewModelScope.launch {
                miningHistoryRepo.logMining(
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
                if (isCloudProfileReady) {
                    cloudSyncRepo.saveMiningRecord(
                        userId = currentUserId,
                        recordId = UUID.randomUUID().toString(),
                        amountHx1 = amount,
                        sourceType = MiningSourceType.MANUAL,
                        hashrateSnapshot = snapshotHashrate,
                        timestampMillis = now
                    )
                    syncProfileToCloudNow()
                }
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
        syncProfileToCloudNow()
    }

    fun sendAiMessage(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return
        if (_aiUiState.value is AiUiState.Loading || _aiUiState.value is AiUiState.Streaming) return

        val lang = _language.value
        val mining = _miningState.value
        val market = _marketState.value
        val activeTrade = _tradingState.value.activePosition

        val contextSummary = buildString {
            append("- Số dư HAIGENX1: ${FormatUtils.formatHx1(mining.balanceHx1, lang)} HX1\n")
            append("- Cấp nâng cấp: ASIC Lv${mining.hashrateLevel}, Titan Drill Lv${mining.tapPowerLevel}, Energy Cell Lv${mining.energyLevel}\n")
            append("- Hashrate hiện tại: ${FormatUtils.formatHashrate(mining.effectiveHashrate, lang)} H/s ")
            append(if (mining.isBoostActive) "(Đang bật AI Boost 3x còn ${FormatUtils.formatCountdown(mining.boostRemainingSeconds)})\n" else "(Chưa bật AI Boost)\n")
            append("- Năng lượng: ${mining.energy.toInt()}/${mining.maxEnergy.toInt()}\n")
            append("- Giá HAIGENX1/USDT (Sàn 1 - mô phỏng, giá gốc 0.000001): ${FormatUtils.formatUsdtPrice(market.currentPriceUsdt, lang)} USDT ")
            append("(${FormatUtils.formatPercent(market.priceChangePercent, lang)})\n")
            append("- Chỉ số USDT (ngược chiều): ${FormatUtils.formatHashrate(market.usdtVirtualIndex, lang)} ")
            append("(${FormatUtils.formatPercent(market.usdtChangePercent, lang)})\n")
            market.activeNews?.let { news ->
                append("- Tin tức mới nhất: \"${news.headlineFor(lang)}\"")
                if (news.isFakeRevealed) append(" [Đã xác nhận TIN GIẢ]")
                append("\n")
            }
            if (activeTrade != null) {
                append("- Lệnh đang mở: ${activeTrade.direction.name} x${activeTrade.leverage}, còn ${activeTrade.remainingSeconds}s, P&L tạm tính: ${activeTrade.floatingPnlHx1} HX1")
            }
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
        if (_chatMessages.value.size <= 1 && _chatMessages.value.firstOrNull()?.isUser == false) {
            _chatMessages.value = listOf(
                ChatMessage(isUser = false, text = stringsFor(newLang).aiWelcomeMessage)
            )
        }
    }

    suspend fun flushStateBeforeSignOut() {
        adCountdownJob?.cancel()
        aiRequestJob?.cancel()
        tradeCountdownJob?.cancel()
        val now = System.currentTimeMillis()
        val m = _miningState.value
        val mk = _marketState.value
        prefsRepo.applyCloudSnapshot(
            displayName = _accountDisplayName.value,
            balanceHx1 = m.balanceHx1,
            energy = m.energy,
            hashrateLevel = hashrateLevel,
            tapPowerLevel = tapPowerLevel,
            energyLevel = energyLevel,
            multiplierLevel = multiplierLevel,
            turboMultiplierLevel = turboMultiplierLevel,
            boostExpiresAtMillis = boostExpiresAtMillis,
            simulatedPriceUsdt = mk.currentPriceUsdt,
            usdtVirtualIndex = mk.usdtVirtualIndex,
            lastActiveMillis = now
        )
        syncProfileToCloudNow()
    }

    private suspend fun syncProfileToCloudNow() {
        if (!isCloudProfileReady) return
        val m = _miningState.value
        val mk = _marketState.value
        cloudSyncRepo.updateUserProgress(
            userId = currentUserId,
            displayName = _accountDisplayName.value,
            balanceHx1 = m.balanceHx1,
            energy = m.energy,
            hashrateLevel = hashrateLevel,
            tapPowerLevel = tapPowerLevel,
            energyLevel = energyLevel,
            multiplierLevel = multiplierLevel,
            turboMultiplierLevel = turboMultiplierLevel,
            boostExpiresAtMillis = boostExpiresAtMillis,
            simulatedPriceUsdt = mk.currentPriceUsdt,
            usdtVirtualIndex = mk.usdtVirtualIndex,
            lastActiveMillis = System.currentTimeMillis()
        ).onFailure { err ->
            Log.w(TAG, "Cloud progress sync deferred: ${err.message}")
        }
    }

    companion object {
        const val TAP_ENERGY_COST = 4.0
        const val MIN_PRICE_USDT = 0.000000001
        const val MAX_PRICE_USDT = 0.05
        const val MIN_USDT_INDEX = 10.0
        const val MAX_USDT_INDEX = 800.0
        const val PARTICIPANTS_POLL_INTERVAL_MS = 30_000L
    }
}
