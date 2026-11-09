package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GoalPaceMetrics
import com.example.data.model.GoalTransaction
import com.example.data.model.SavingsGoal
import com.example.data.repository.SavingsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StreamResult<out T> {
    data object Loading : StreamResult<Nothing>
    data class Success<T>(val data: T) : StreamResult<T>
    data class Error(val message: String) : StreamResult<Nothing>
}

data class DashboardUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val selectedGoal: SavingsGoal? = null,
    val selectedGoalTransactions: List<GoalTransaction> = emptyList(),
    val paceMetrics: GoalPaceMetrics? = null,
    val pinnedGoalId: String? = null,
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class SavingsViewModel(
    private val repository: SavingsRepository,
    private val currentUserId: String,
    initialPinnedGoalId: String? = null,
    private val onSyncWidgetSnapshot: (SavingsGoal?) -> Unit = {},
    private val onPinGoalToWidget: (SavingsGoal) -> Unit = {}
) : ViewModel() {

    private val selectedGoalIdFlow = MutableStateFlow<String?>(initialPinnedGoalId)
    private val pinnedGoalIdFlow = MutableStateFlow<String?>(initialPinnedGoalId)
    private val isMutatingFlow = MutableStateFlow(false)
    private val transientErrorFlow = MutableStateFlow<String?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val goalsStream: StateFlow<StreamResult<List<SavingsGoal>>> =
        repository.observeGoals(currentUserId)
            .map<List<SavingsGoal>, StreamResult<List<SavingsGoal>>> { StreamResult.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing goals", rootError)
                emit(StreamResult.Error(rootError.localizedMessage ?: "Ошибка загрузки целей"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = StreamResult.Loading
            )

    private val transactionsStream: StateFlow<StreamResult<List<GoalTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<GoalTransaction>, StreamResult<List<GoalTransaction>>> { StreamResult.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing transactions", rootError)
                emit(StreamResult.Error(rootError.localizedMessage ?: "Ошибка загрузки истории"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = StreamResult.Loading
            )

    val uiState: StateFlow<DashboardUiState> = combine(
        goalsStream,
        transactionsStream,
        selectedGoalIdFlow,
        pinnedGoalIdFlow,
        combine(isMutatingFlow, transientErrorFlow, statusMessageFlow) { mutating, err, status ->
            Triple(mutating, err, status)
        }
    ) { goalsRes, txRes, selectedId, pinnedId, meta ->
        val (isMutating, transientErr, statusMsg) = meta
        val goalsList = (goalsRes as? StreamResult.Success)?.data ?: emptyList()
        val txList = (txRes as? StreamResult.Success)?.data ?: emptyList()

        val activeGoal = when {
            goalsList.isEmpty() -> null
            selectedId != null -> goalsList.find { it.id == selectedId } ?: goalsList.first()
            pinnedId != null -> goalsList.find { it.id == pinnedId } ?: goalsList.first()
            else -> goalsList.first()
        }

        val effectivePinnedId = when {
            goalsList.isEmpty() -> null
            pinnedId != null && goalsList.any { it.id == pinnedId } -> pinnedId
            else -> activeGoal?.id
        }

        val widgetGoal = goalsList.find { it.id == effectivePinnedId } ?: activeGoal
        onSyncWidgetSnapshot(widgetGoal)

        val filteredTransactions = if (activeGoal != null) {
            txList.filter { it.goalId == activeGoal.id }
        } else {
            emptyList()
        }

        val pace = activeGoal?.let { GoalPaceMetrics.compute(it) }
        val streamError = (goalsRes as? StreamResult.Error)?.message
            ?: (txRes as? StreamResult.Error)?.message

        DashboardUiState(
            goals = goalsList,
            selectedGoal = activeGoal,
            selectedGoalTransactions = filteredTransactions,
            paceMetrics = pace,
            pinnedGoalId = effectivePinnedId,
            isLoading = goalsRes is StreamResult.Loading,
            isMutating = isMutating,
            errorMessage = transientErr ?: streamError,
            statusMessage = statusMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState()
    )

    fun selectGoal(goalId: String) {
        selectedGoalIdFlow.value = goalId
    }

    fun pinGoalToWidget(goal: SavingsGoal) {
        pinnedGoalIdFlow.value = goal.id
        onPinGoalToWidget(goal)
        statusMessageFlow.value = "Цель «${goal.title}» закреплена на виджете рабочего стола"
    }

    fun clearMessages() {
        transientErrorFlow.value = null
        statusMessageFlow.value = null
    }

    fun createGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double,
        currencySymbol: String,
        colorHex: String,
        targetDateMillis: Long?
    ) {
        viewModelScope.launch {
            isMutatingFlow.value = true
            transientErrorFlow.value = null
            try {
                val createdId = repository.createGoal(
                    title = title,
                    targetAmount = targetAmount,
                    initialAmount = initialAmount,
                    currencySymbol = currencySymbol,
                    colorHex = colorHex,
                    targetDateMillis = targetDateMillis
                )
                selectedGoalIdFlow.value = createdId
                if (pinnedGoalIdFlow.value == null) {
                    pinnedGoalIdFlow.value = createdId
                }
                statusMessageFlow.value = "Цель «$title» создана"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                transientErrorFlow.value = e.localizedMessage ?: "Не удалось создать цель"
            } finally {
                isMutatingFlow.value = false
            }
        }
    }

    fun updateGoalDetails(
        goalId: String,
        title: String,
        targetAmount: Double,
        currencySymbol: String,
        colorHex: String,
        targetDateMillis: Long?
    ) {
        viewModelScope.launch {
            isMutatingFlow.value = true
            transientErrorFlow.value = null
            try {
                repository.updateGoalDetails(
                    goalId = goalId,
                    title = title,
                    targetAmount = targetAmount,
                    currencySymbol = currencySymbol,
                    colorHex = colorHex,
                    targetDateMillis = targetDateMillis
                )
                statusMessageFlow.value = "Параметры цели обновлены"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                transientErrorFlow.value = e.localizedMessage ?: "Не удалось обновить цель"
            } finally {
                isMutatingFlow.value = false
            }
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            isMutatingFlow.value = true
            transientErrorFlow.value = null
            try {
                repository.deleteGoal(goal.id)
                if (selectedGoalIdFlow.value == goal.id) {
                    selectedGoalIdFlow.value = null
                }
                statusMessageFlow.value = "Цель «${goal.title}» удалена"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                transientErrorFlow.value = e.localizedMessage ?: "Не удалось удалить цель"
            } finally {
                isMutatingFlow.value = false
            }
        }
    }

    fun addQuickContribution(amount: Double, label: String) {
        val goal = uiState.value.selectedGoal ?: return
        addContribution(goal = goal, deltaAmount = amount, note = label)
    }

    fun addCustomContribution(amount: Double, isWithdrawal: Boolean, note: String) {
        val goal = uiState.value.selectedGoal ?: return
        val signedAmount = if (isWithdrawal) -amount else amount
        val defaultNote = if (isWithdrawal) "Снятие со сбора" else "Пополнение сбора"
        addContribution(
            goal = goal,
            deltaAmount = signedAmount,
            note = note.trim().ifEmpty { defaultNote }
        )
    }

    private fun addContribution(goal: SavingsGoal, deltaAmount: Double, note: String) {
        viewModelScope.launch {
            isMutatingFlow.value = true
            transientErrorFlow.value = null
            try {
                repository.addContribution(
                    goalId = goal.id,
                    currentGoalAmount = goal.currentAmount,
                    deltaAmount = deltaAmount,
                    note = note
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                transientErrorFlow.value = e.localizedMessage ?: "Ошибка сохранения взноса"
            } finally {
                isMutatingFlow.value = false
            }
        }
    }

    fun deleteTransaction(transaction: GoalTransaction) {
        val goal = uiState.value.selectedGoal ?: return
        viewModelScope.launch {
            isMutatingFlow.value = true
            transientErrorFlow.value = null
            try {
                repository.deleteTransaction(
                    transaction = transaction,
                    currentGoalAmount = goal.currentAmount
                )
                statusMessageFlow.value = "Операция отменена"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                transientErrorFlow.value = e.localizedMessage ?: "Не удалось отменить операцию"
            } finally {
                isMutatingFlow.value = false
            }
        }
    }

    private companion object {
        const val TAG = "SavingsViewModel"
    }
}
