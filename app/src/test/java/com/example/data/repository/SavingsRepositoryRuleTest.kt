package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SavingsRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createGoal_validPayload_createsDocumentAndEmitsInFlow() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SavingsRepository(firestore, auth)

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGoal(
                title = "Отпуск в горах",
                targetAmount = 120000.0,
                initialAmount = 15000.0,
                currencySymbol = "₽",
                colorHex = "#10B981",
                targetDateMillis = System.currentTimeMillis() + 86_400_000L * 30
            )
        }
        assertTrue(goalId.isNotBlank())

        val emittedGoals = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGoals(uid).first { list -> list.any { it.id == goalId } }
        }
        val created = emittedGoals.first { it.id == goalId }
        assertEquals("Отпуск в горах", created.title)
        assertEquals(120000.0, created.targetAmount, 0.01)
        assertEquals(15000.0, created.currentAmount, 0.01)
        assertEquals(105000.0, created.remainingAmount, 0.01)
    }

    @Test
    fun addContribution_updatesGoalAmountAndCreatesTransaction() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SavingsRepository(firestore, auth)

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGoal(
                title = "Новый ноутбук",
                targetAmount = 100000.0,
                initialAmount = 10000.0
            )
        }

        val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addContribution(
                goalId = goalId,
                currentGoalAmount = 10000.0,
                deltaAmount = 5000.0,
                note = "Быстрый взнос +5 000"
            )
        }
        assertTrue(txId.isNotBlank())

        val updatedGoal = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getGoalById(uid, goalId)
        }
        assertNotNull(updatedGoal)
        assertEquals(15000.0, updatedGoal!!.currentAmount, 0.01)

        val transactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list -> list.any { it.id == txId } }
        }
        val tx = transactions.first { it.id == txId }
        assertEquals(5000.0, tx.amount, 0.01)
        assertEquals("Быстрый взнос +5 000", tx.note)
    }

    @Test
    fun getGoalById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SavingsRepository(firestore, auth)
        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createGoal(
                title = "Alice Private Goal",
                targetAmount = 50000.0,
                initialAmount = 5000.0
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SavingsRepository(firestore, auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getGoalById(aliceUid, goalId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun observeGoals_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = SavingsRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeGoals(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when observing goals while signed out")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_goalpulse@test.com"
        const val BOB_EMAIL = "bob_goalpulse@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
