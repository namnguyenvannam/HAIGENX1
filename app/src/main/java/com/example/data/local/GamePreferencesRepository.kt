package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "haigenx1_game_prefs"
)

data class PersistedGameData(
    val balanceHx1: Double = 0.0,
    val energy: Double = 100.0,
    val boostExpiresAtMillis: Long = 0L,
    val lastActiveMillis: Long = System.currentTimeMillis(),
    val simulatedPriceUsdt: Double = DEFAULT_INITIAL_PRICE_USDT,
    val usdtVirtualIndex: Double = DEFAULT_USDT_INDEX,
    val language: AppLanguage = AppLanguage.VI
) {
    companion object {
        const val DEFAULT_INITIAL_PRICE_USDT = 0.000001
        const val DEFAULT_USDT_INDEX = 100.0
    }
}

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val BALANCE_HX1 = doublePreferencesKey("balance_hx1")
        val ENERGY = doublePreferencesKey("energy")
        val BOOST_EXPIRES_AT = longPreferencesKey("boost_expires_at")
        val LAST_ACTIVE_MILLIS = longPreferencesKey("last_active_millis")
        val SIMULATED_PRICE_USDT = doublePreferencesKey("simulated_price_usdt")
        val USDT_VIRTUAL_INDEX = doublePreferencesKey("usdt_virtual_index")
        val LANGUAGE = stringPreferencesKey("app_language")
    }

    val gameDataFlow: Flow<PersistedGameData> = context.gameDataStore.data.map { prefs ->
        PersistedGameData(
            balanceHx1 = prefs[Keys.BALANCE_HX1] ?: 0.0,
            energy = prefs[Keys.ENERGY] ?: 100.0,
            boostExpiresAtMillis = prefs[Keys.BOOST_EXPIRES_AT] ?: 0L,
            lastActiveMillis = prefs[Keys.LAST_ACTIVE_MILLIS] ?: System.currentTimeMillis(),
            simulatedPriceUsdt = prefs[Keys.SIMULATED_PRICE_USDT]
                ?: PersistedGameData.DEFAULT_INITIAL_PRICE_USDT,
            usdtVirtualIndex = prefs[Keys.USDT_VIRTUAL_INDEX]
                ?: PersistedGameData.DEFAULT_USDT_INDEX,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE])
        )
    }

    suspend fun saveMiningSnapshot(
        balanceHx1: Double,
        energy: Double,
        boostExpiresAtMillis: Long,
        lastActiveMillis: Long
    ) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.BALANCE_HX1] = balanceHx1
            prefs[Keys.ENERGY] = energy
            prefs[Keys.BOOST_EXPIRES_AT] = boostExpiresAtMillis
            prefs[Keys.LAST_ACTIVE_MILLIS] = lastActiveMillis
        }
    }

    suspend fun saveMarketSnapshot(priceUsdt: Double, usdtIndex: Double) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.SIMULATED_PRICE_USDT] = priceUsdt
            prefs[Keys.USDT_VIRTUAL_INDEX] = usdtIndex
        }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun resetAll(keepLanguage: AppLanguage) {
        context.gameDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.LANGUAGE] = keepLanguage.code
            prefs[Keys.BALANCE_HX1] = 0.0
            prefs[Keys.ENERGY] = 100.0
            prefs[Keys.BOOST_EXPIRES_AT] = 0L
            prefs[Keys.LAST_ACTIVE_MILLIS] = System.currentTimeMillis()
            prefs[Keys.SIMULATED_PRICE_USDT] = PersistedGameData.DEFAULT_INITIAL_PRICE_USDT
            prefs[Keys.USDT_VIRTUAL_INDEX] = PersistedGameData.DEFAULT_USDT_INDEX
        }
    }
}
