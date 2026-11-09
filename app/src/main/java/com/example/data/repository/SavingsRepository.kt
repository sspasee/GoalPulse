package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.GoalTransaction
import com.example.data.model.SavingsGoal
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class SavingsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Необходима авторизация через Google для работы с облачной базой.")
    }

    fun observeGoals(userId: String = requireUserId()): Flow<List<SavingsGoal>> = flow {
        val path = "users/$userId/goals"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("goals")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toSavingsGoal()
                    }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String = requireUserId()): Flow<List<GoalTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toGoalTransaction()
                    }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getGoalById(ownerUserId: String, goalId: String): SavingsGoal? {
        val docRef = db.collection("users")
            .document(ownerUserId)
            .collection("goals")
            .document(goalId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) snapshot.toSavingsGoal() else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        currencySymbol: String = "₽",
        colorHex: String = "#10B981",
        targetDateMillis: Long? = null
    ): String {
        val uid = requireUserId()
        val safeTitle = title.trim().take(100).ifEmpty { "Моя цель" }
        val safeTarget = targetAmount.coerceAtLeast(1.0)
        val safeInitial = initialAmount.coerceAtLeast(0.0)
        val docRef = db.collection("users").document(uid).collection("goals").document()

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to safeTitle,
            "targetAmount" to safeTarget,
            "currentAmount" to safeInitial,
            "currencySymbol" to currencySymbol.trim().ifEmpty { "₽" }.take(10),
            "colorHex" to colorHex.trim().ifEmpty { "#10B981" }.take(12),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (targetDateMillis != null && targetDateMillis >= 0L) {
            payload["targetDateMillis"] = targetDateMillis
        }

        try {
            docRef.set(payload).await()
            if (safeInitial > 0.0) {
                val txRef = db.collection("users").document(uid).collection("transactions").document()
                val txPayload = mapOf(
                    "userId" to uid,
                    "goalId" to docRef.id,
                    "amount" to safeInitial,
                    "note" to "Стартовый баланс",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                txRef.set(txPayload).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateGoalDetails(
        goalId: String,
        title: String,
        targetAmount: Double,
        currencySymbol: String,
        colorHex: String,
        targetDateMillis: Long?
    ) {
        val uid = requireUserId()
        require(goalId.isNotBlank()) { "Goal ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
        val updates = mutableMapOf<String, Any>(
            "title" to title.trim().take(100).ifEmpty { "Моя цель" },
            "targetAmount" to targetAmount.coerceAtLeast(1.0),
            "currencySymbol" to currencySymbol.trim().ifEmpty { "₽" }.take(10),
            "colorHex" to colorHex.trim().ifEmpty { "#10B981" }.take(12),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (targetDateMillis != null && targetDateMillis >= 0L) {
            updates["targetDateMillis"] = targetDateMillis
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addContribution(
        goalId: String,
        currentGoalAmount: Double,
        deltaAmount: Double,
        note: String
    ): String {
        val uid = requireUserId()
        require(goalId.isNotBlank()) { "Goal ID must not be blank" }
        require(deltaAmount != 0.0) { "Delta amount must be non-zero" }

        val newAmount = max(0.0, currentGoalAmount + deltaAmount)
        val actualDelta = newAmount - currentGoalAmount
        if (actualDelta == 0.0) return ""

        val goalRef = db.collection("users").document(uid).collection("goals").document(goalId)
        val txRef = db.collection("users").document(uid).collection("transactions").document()

        val txPayload = mapOf(
            "userId" to uid,
            "goalId" to goalId,
            "amount" to actualDelta,
            "note" to note.trim().take(200),
            "createdAt" to FieldValue.serverTimestamp()
        )
        val goalUpdates = mapOf(
            "currentAmount" to newAmount,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(txRef, txPayload)
            batch.update(goalRef, goalUpdates)
            batch.commit().await()
            return txRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, goalRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(
        transaction: GoalTransaction,
        currentGoalAmount: Double
    ) {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID must not be blank" }
        val txRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)
        val goalRef = db.collection("users").document(uid).collection("goals").document(transaction.goalId)
        val restoredAmount = max(0.0, currentGoalAmount - transaction.amount)

        try {
            val batch = db.batch()
            batch.delete(txRef)
            batch.update(
                goalRef,
                mapOf(
                    "currentAmount" to restoredAmount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, txRef.path)
            throw e
        }
    }

    suspend fun deleteGoal(goalId: String) {
        val uid = requireUserId()
        require(goalId.isNotBlank()) { "Goal ID must not be blank" }
        val goalRef = db.collection("users").document(uid).collection("goals").document(goalId)
        try {
            goalRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, goalRef.path)
            throw e
        }
    }

    private fun DocumentSnapshot.toSavingsGoal(): SavingsGoal? {
        val data = this.data ?: return null
        return SavingsGoal(
            id = this.id,
            userId = data["userId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
            currentAmount = (data["currentAmount"] as? Number)?.toDouble() ?: 0.0,
            currencySymbol = data["currencySymbol"] as? String ?: "₽",
            colorHex = data["colorHex"] as? String ?: "#10B981",
            targetDateMillis = (data["targetDateMillis"] as? Number)?.toLong(),
            createdAt = this.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = this.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun DocumentSnapshot.toGoalTransaction(): GoalTransaction? {
        val data = this.data ?: return null
        return GoalTransaction(
            id = this.id,
            userId = data["userId"] as? String ?: "",
            goalId = data["goalId"] as? String ?: "",
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            note = data["note"] as? String ?: "",
            createdAt = this.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }
}
