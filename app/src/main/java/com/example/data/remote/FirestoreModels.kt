package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    val userId: String = "",
    val displayName: String = "HAIGENX1 Miner",
    val balanceHx1: Double = 0.0,
    val energy: Double = 100.0,
    val hashrateLevel: Int = 0,
    val tapPowerLevel: Int = 0,
    val energyLevel: Int = 0,
    val multiplierLevel: Int = 0,
    val turboMultiplierLevel: Int = 0,
    val boostExpiresAtMillis: Long = 0L,
    val simulatedPriceUsdt: Double = 0.000001,
    val usdtVirtualIndex: Double = 100.0,
    val lastActiveMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreParticipant(
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreTradeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val direction: String = "BUY",
    val leverage: Int = 1,
    val entryFeeHx1: Double = 10.0,
    val entryPriceUsdt: Double = 0.000001,
    val exitPriceUsdt: Double = 0.000001,
    val outcome: String = "TIE",
    val payoutHx1: Double = 10.0,
    val netPnlHx1: Double = 0.0,
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreMiningRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amountHx1: Double = 0.0,
    val sourceType: String = "AUTO",
    val hashrateSnapshot: Double = 12.5,
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
