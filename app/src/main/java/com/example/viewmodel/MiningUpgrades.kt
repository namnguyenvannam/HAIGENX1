package com.example.viewmodel

import kotlin.math.pow
import kotlin.math.roundToInt

enum class UpgradeCategory {
    MULTIPLIER,
    CORE
}

enum class UpgradeType(val category: UpgradeCategory) {
    OVERCLOCK_MULTIPLIER(UpgradeCategory.MULTIPLIER),
    QUANTUM_TURBO_RIG(UpgradeCategory.MULTIPLIER),
    HASHRATE_CORE(UpgradeCategory.CORE),
    TAP_DRILL(UpgradeCategory.CORE),
    ENERGY_CELL(UpgradeCategory.CORE)
}

data class UpgradeItemUi(
    val type: UpgradeType,
    val level: Int,
    val maxLevel: Int,
    val nextCostHx1: Double,
    val canAfford: Boolean,
    val isMaxed: Boolean,
    val currentStatText: String,
    val nextStatText: String
)

object MiningUpgrades {
    const val MAX_CORE_UPGRADE_LEVEL = 50
    const val MAX_OVERCLOCK_LEVEL = 20
    const val MAX_TURBO_RIG_LEVEL = 10

    const val BASE_HASHRATE = 12.5
    const val BASE_COINS_PER_SEC = 0.025
    const val BASE_TAP_GAIN = 0.65
    const val BASE_MAX_ENERGY = 100.0
    const val BASE_ENERGY_REGEN_PER_SEC = 2.5

    fun maxLevelFor(type: UpgradeType): Int = when (type) {
        UpgradeType.OVERCLOCK_MULTIPLIER -> MAX_OVERCLOCK_LEVEL
        UpgradeType.QUANTUM_TURBO_RIG -> MAX_TURBO_RIG_LEVEL
        UpgradeType.HASHRATE_CORE,
        UpgradeType.TAP_DRILL,
        UpgradeType.ENERGY_CELL -> MAX_CORE_UPGRADE_LEVEL
    }

    fun baseHashrateForLevel(level: Int): Double =
        BASE_HASHRATE + level.coerceAtLeast(0) * 6.0

    fun baseCoinsPerSecondForLevel(level: Int): Double =
        BASE_COINS_PER_SEC + level.coerceAtLeast(0) * 0.015

    fun baseTapGainForLevel(level: Int): Double =
        BASE_TAP_GAIN + level.coerceAtLeast(0) * 0.35

    fun maxEnergyForLevel(level: Int): Double =
        BASE_MAX_ENERGY + level.coerceAtLeast(0) * 20.0

    fun energyRegenPerSecondForLevel(level: Int): Double =
        BASE_ENERGY_REGEN_PER_SEC + level.coerceAtLeast(0) * 0.6

    fun overclockBonusForLevel(level: Int): Double =
        level.coerceAtLeast(0) * 0.25

    fun turboRigBonusForLevel(level: Int): Double =
        level.coerceAtLeast(0) * 0.50

    fun totalPermanentMultiplier(overclockLevel: Int, turboLevel: Int): Double =
        1.0 + overclockBonusForLevel(overclockLevel) + turboRigBonusForLevel(turboLevel)

    fun costForNextLevel(type: UpgradeType, currentLevel: Int): Double {
        val baseCost = when (type) {
            UpgradeType.OVERCLOCK_MULTIPLIER -> 35.0
            UpgradeType.QUANTUM_TURBO_RIG -> 90.0
            UpgradeType.HASHRATE_CORE -> 15.0
            UpgradeType.TAP_DRILL -> 12.0
            UpgradeType.ENERGY_CELL -> 10.0
        }
        val multiplier = when (type) {
            UpgradeType.OVERCLOCK_MULTIPLIER -> 1.55
            UpgradeType.QUANTUM_TURBO_RIG -> 1.72
            UpgradeType.HASHRATE_CORE -> 1.42
            UpgradeType.TAP_DRILL -> 1.38
            UpgradeType.ENERGY_CELL -> 1.35
        }
        return (baseCost * multiplier.pow(currentLevel.coerceAtLeast(0))).roundToInt().toDouble()
    }
}
