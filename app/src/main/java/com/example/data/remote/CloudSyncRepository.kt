package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.MiningSourceType
import com.example.data.local.PersistedGameData
import com.example.data.local.TradeDirection
import com.example.data.local.TradeOutcome
import com.example.data.local.TradeRecordEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "CloudSyncRepository"

class CloudSyncRepository private constructor(
    private val appContext: Context?,
    private val dbOverride: FirebaseFirestore?,
    private val authOverride: FirebaseAuth?
) {
    constructor(context: Context) : this(
        appContext = context.applicationContext,
        dbOverride = null,
        authOverride = null
    )

    constructor(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ) : this(
        appContext = null,
        dbOverride = db,
        authOverride = auth
    )

    fun isFirebaseInitialized(): Boolean {
        return try {
            if (dbOverride != null && authOverride != null) {
                true
            } else {
                val ctx = appContext ?: return false
                FirebaseApp.getApps(ctx).isNotEmpty() || FirebaseApp.initializeApp(ctx) != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization check failed: ${e.message}")
            false
        }
    }

    private fun getAuthOrNull(): FirebaseAuth? {
        return try {
            if (authOverride != null) {
                authOverride
            } else if (isFirebaseInitialized()) {
                Firebase.auth
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }

    private fun getFirestoreOrNull(): FirebaseFirestore? {
        return try {
            if (dbOverride != null) {
                dbOverride
            } else if (isFirebaseInitialized()) {
                val ctx = appContext ?: return null
                val resId = ctx.resources.getIdentifier(
                    "firestore_database_id",
                    "string",
                    ctx.packageName
                )
                if (resId != 0) {
                    val databaseId = ctx.getString(resId)
                    if (databaseId.isNotBlank()) {
                        FirebaseFirestore.getInstance(databaseId)
                    } else {
                        FirebaseFirestore.getInstance()
                    }
                } else {
                    FirebaseFirestore.getInstance()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore unavailable: ${e.message}")
            null
        }
    }

    private fun requireReadyContext(expectedUserId: String): Pair<FirebaseFirestore, String> {
        val db = getFirestoreOrNull()
            ?: throw IllegalStateException("FirebaseFirestore is not initialized.")
        val auth = getAuthOrNull()
            ?: throw IllegalStateException("FirebaseAuth is not initialized.")
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Anonymous Firebase user is not signed in.")
        check(uid == expectedUserId) { "Authenticated UID changed; refusing cross-user Firestore operation." }
        return db to uid
    }

    private fun sanitizeDisplayName(rawName: String): String {
        val cleaned = rawName.trim().take(80)
        return cleaned.ifEmpty { "HAIGENX1 Miner" }
    }

    /**
     * Signs in anonymously via Firebase Auth when Firebase is configured.
     * Safe when google-services.json is absent or Anonymous Auth is unavailable.
     */
    suspend fun ensureAnonymousAuthUid(): Result<String> {
        return try {
            val auth = getAuthOrNull()
                ?: return Result.failure(IllegalStateException("Firebase is not initialized"))
            auth.currentUser?.uid?.takeIf { it.isNotBlank() }?.let { existingUid ->
                return Result.success(existingUid)
            }
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Anonymous auth returned null UID"))
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous Firebase auth failed or unconfigured: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Registers at most ONE document in `participants/{uid}` if not yet created on the server.
     * Enforced both client-side and server-side via `!exists(...)` create-only Firestore rules.
     */
    suspend fun ensureParticipantRegisteredOnce(userId: String): Result<Boolean> {
        val docPath = "participants/$userId"
        return try {
            val (db, uid) = requireReadyContext(userId)
            require(uid.matches(VALID_ID_REGEX)) { "Invalid participant ID" }
            val docRef = db.collection("participants").document(uid)
            val existing = docRef.get(Source.SERVER).await()
            if (existing.exists()) {
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Reads the exact server-side participant count using Firestore count aggregation.
     * Uses AggregateSource.SERVER so offline/unconfigured clients cleanly receive failure and render "--".
     */
    suspend fun fetchParticipantCount(userId: String): Result<Long> {
        val path = "participants"
        return try {
            val (db, _) = requireReadyContext(userId)
            val aggregate = db.collection("participants")
                .count()
                .get(AggregateSource.SERVER)
                .await()
            Result.success(aggregate.count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(userId: String): Result<FirestoreUserProfile?> {
        val docPath = "users/$userId"
        return try {
            val (db, uid) = requireReadyContext(userId)
            val snapshot = db.collection("users").document(uid).get().await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                val profile = snapshot.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(profile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun createUserProfile(
        userId: String,
        displayName: String,
        snapshot: PersistedGameData
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val (db, uid) = requireReadyContext(userId)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to sanitizeDisplayName(displayName),
                "balanceHx1" to snapshot.balanceHx1.coerceAtLeast(0.0),
                "energy" to snapshot.energy.coerceAtLeast(0.0),
                "hashrateLevel" to snapshot.hashrateLevel.coerceIn(0, 100),
                "tapPowerLevel" to snapshot.tapPowerLevel.coerceIn(0, 100),
                "energyLevel" to snapshot.energyLevel.coerceIn(0, 100),
                "multiplierLevel" to snapshot.multiplierLevel.coerceIn(0, 100),
                "turboMultiplierLevel" to snapshot.turboMultiplierLevel.coerceIn(0, 100),
                "boostExpiresAtMillis" to snapshot.boostExpiresAtMillis.coerceAtLeast(0L),
                "simulatedPriceUsdt" to snapshot.simulatedPriceUsdt.coerceAtLeast(1e-9),
                "usdtVirtualIndex" to snapshot.usdtVirtualIndex.coerceAtLeast(0.0),
                "lastActiveMillis" to snapshot.lastActiveMillis.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProgress(
        userId: String,
        displayName: String,
        balanceHx1: Double,
        energy: Double,
        hashrateLevel: Int,
        tapPowerLevel: Int,
        energyLevel: Int,
        multiplierLevel: Int,
        turboMultiplierLevel: Int,
        boostExpiresAtMillis: Long,
        simulatedPriceUsdt: Double,
        usdtVirtualIndex: Double,
        lastActiveMillis: Long
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val (db, uid) = requireReadyContext(userId)
            val payload = mapOf(
                "displayName" to sanitizeDisplayName(displayName),
                "balanceHx1" to balanceHx1.coerceAtLeast(0.0),
                "energy" to energy.coerceAtLeast(0.0),
                "hashrateLevel" to hashrateLevel.coerceIn(0, 100),
                "tapPowerLevel" to tapPowerLevel.coerceIn(0, 100),
                "energyLevel" to energyLevel.coerceIn(0, 100),
                "multiplierLevel" to multiplierLevel.coerceIn(0, 100),
                "turboMultiplierLevel" to turboMultiplierLevel.coerceIn(0, 100),
                "boostExpiresAtMillis" to boostExpiresAtMillis.coerceAtLeast(0L),
                "simulatedPriceUsdt" to simulatedPriceUsdt.coerceAtLeast(1e-9),
                "usdtVirtualIndex" to usdtVirtualIndex.coerceAtLeast(0.0),
                "lastActiveMillis" to lastActiveMillis.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val (db, uid) = requireReadyContext(userId)
            val payload = mapOf(
                "displayName" to sanitizeDisplayName(displayName),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveTradeRecord(userId: String, trade: TradeRecordEntity): Result<Unit> {
        val docPath = "users/$userId/trades/${trade.id}"
        return try {
            val (db, uid) = requireReadyContext(userId)
            require(trade.id.isNotBlank() && trade.id.matches(VALID_ID_REGEX)) {
                "Invalid trade ID"
            }
            val safeDirection = TradeDirection.entries.firstOrNull { it.name == trade.direction }?.name ?: "BUY"
            val safeOutcome = TradeOutcome.entries.firstOrNull { it.name == trade.outcome }?.name ?: "TIE"
            val payload = mapOf(
                "userId" to uid,
                "direction" to safeDirection,
                "leverage" to trade.leverage.coerceIn(1, 10),
                "entryFeeHx1" to trade.entryFeeHx1.coerceAtLeast(0.0001),
                "entryPriceUsdt" to trade.entryPriceUsdt.coerceAtLeast(1e-9),
                "exitPriceUsdt" to trade.exitPriceUsdt.coerceAtLeast(1e-9),
                "outcome" to safeOutcome,
                "payoutHx1" to trade.payoutHx1.coerceAtLeast(0.0),
                "netPnlHx1" to trade.netPnlHx1,
                "timestampMillis" to trade.timestampMillis.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("trades").document(trade.id)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeTrades(userId: String): Flow<List<TradeRecordEntity>> = flow {
        val path = "users/$userId/trades"
        try {
            val (db, uid) = requireReadyContext(userId)
            emitAll(
                db.collection("users").document(uid).collection("trades")
                    .orderBy("timestampMillis", Query.Direction.DESCENDING)
                    .limit(50)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            FirestoreTradeRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).map { remote ->
                            TradeRecordEntity(
                                id = remote.id,
                                direction = TradeDirection.entries.firstOrNull { it.name == remote.direction }?.name
                                    ?: TradeDirection.BUY.name,
                                leverage = remote.leverage.coerceIn(1, 10),
                                entryFeeHx1 = remote.entryFeeHx1,
                                entryPriceUsdt = remote.entryPriceUsdt,
                                exitPriceUsdt = remote.exitPriceUsdt,
                                outcome = TradeOutcome.entries.firstOrNull { it.name == remote.outcome }?.name
                                    ?: TradeOutcome.TIE.name,
                                payoutHx1 = remote.payoutHx1,
                                netPnlHx1 = remote.netPnlHx1,
                                timestampMillis = remote.timestampMillis
                            )
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
        }
    }

    suspend fun saveMiningRecord(
        userId: String,
        recordId: String,
        amountHx1: Double,
        sourceType: MiningSourceType,
        hashrateSnapshot: Double,
        timestampMillis: Long
    ): Result<Unit> {
        val docPath = "users/$userId/miningRecords/$recordId"
        return try {
            val (db, uid) = requireReadyContext(userId)
            if (amountHx1 <= 0.0) return Result.success(Unit)
            require(recordId.isNotBlank() && recordId.matches(VALID_ID_REGEX)) {
                "Invalid mining record ID"
            }
            val payload = mapOf(
                "userId" to uid,
                "amountHx1" to amountHx1,
                "sourceType" to sourceType.name,
                "hashrateSnapshot" to hashrateSnapshot.coerceAtLeast(0.0),
                "timestampMillis" to timestampMillis.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("miningRecords").document(recordId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fetchRecentMiningRecords(userId: String): Result<List<FirestoreMiningRecord>> {
        val path = "users/$userId/miningRecords"
        return try {
            val (db, uid) = requireReadyContext(userId)
            val snapshot = db.collection("users").document(uid)
                .collection("miningRecords")
                .orderBy("timestampMillis", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val records = snapshot.toObjects(
                FirestoreMiningRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(records)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    private companion object {
        val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
