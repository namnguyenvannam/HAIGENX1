package com.example

import com.example.core.AppLanguage
import com.example.data.local.PersistedGameData
import com.example.viewmodel.MarketNewsCatalog
import com.example.viewmodel.MiningUpgrades
import com.example.viewmodel.UpgradeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExampleUnitTest {
    @Test
    fun `anchor price defaults to 0_000001 USDT`() {
        assertEquals(0.000001, PersistedGameData.DEFAULT_INITIAL_PRICE_USDT, 1e-12)
    }

    @Test
    fun `mining upgrades increase output and scale cost monotonically`() {
        val baseHash = MiningUpgrades.baseHashrateForLevel(0)
        val lv3Hash = MiningUpgrades.baseHashrateForLevel(3)
        assertTrue(lv3Hash > baseHash)

        val baseTap = MiningUpgrades.baseTapGainForLevel(0)
        val lv2Tap = MiningUpgrades.baseTapGainForLevel(2)
        assertTrue(lv2Tap > baseTap)

        val baseEnergy = MiningUpgrades.maxEnergyForLevel(0)
        val lv2Energy = MiningUpgrades.maxEnergyForLevel(2)
        assertTrue(lv2Energy > baseEnergy)

        assertEquals(1.0, MiningUpgrades.totalPermanentMultiplier(0, 0), 1e-9)
        assertEquals(2.0, MiningUpgrades.totalPermanentMultiplier(2, 1), 1e-9)

        UpgradeType.entries.forEach { type ->
            val cost0 = MiningUpgrades.costForNextLevel(type, 0)
            val cost1 = MiningUpgrades.costForNextLevel(type, 1)
            val cost5 = MiningUpgrades.costForNextLevel(type, 5)
            assertTrue(cost1 > cost0)
            assertTrue(cost5 > cost1)
        }
    }

    @Test
    fun `market news catalog includes positive and negative catalysts without immediate repeat`() {
        assertTrue(MarketNewsCatalog.templates.any { it.isPositive })
        assertTrue(MarketNewsCatalog.templates.any { !it.isPositive })
        val first = MarketNewsCatalog.templates.first()
        val next = MarketNewsCatalog.pickRandomTemplate(first.headlineVi, Random(42))
        assertNotEquals(first.headlineVi, next.headlineVi)
        assertTrue(next.textFor(AppLanguage.VI).isNotBlank())
        assertTrue(next.textFor(AppLanguage.EN).isNotBlank())
    }

    @Test
    fun `leveraged trading fees and settlement payouts match 10x entry 18x win 2x loss spec`() {
        for (lev in 1..10) {
            val fee = 10.0 * lev
            val winPayout = 18.0 * lev
            val lossPayout = 2.0 * lev
            val tiePayout = 10.0 * lev
            assertEquals(8.0 * lev, winPayout - fee, 1e-9)
            assertEquals(-8.0 * lev, lossPayout - fee, 1e-9)
            assertEquals(0.0, tiePayout - fee, 1e-9)
        }
    }
}
