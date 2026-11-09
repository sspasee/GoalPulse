package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.ceil
import kotlin.math.max

@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val currencySymbol: String = "₽",
    val colorHex: String = "#10B981",
    val targetDateMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val remainingAmount: Double
        get() = max(0.0, targetAmount - currentAmount)

    @get:Exclude
    val progressFraction: Float
        get() = if (targetAmount > 0.0) {
            (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

    @get:Exclude
    val progressPercent: Int
        get() = if (targetAmount > 0.0) {
            ((currentAmount / targetAmount) * 100.0).toInt().coerceAtLeast(0)
        } else {
            0
        }

    @get:Exclude
    val isCompleted: Boolean
        get() = targetAmount > 0.0 && currentAmount >= targetAmount
}

@IgnoreExtraProperties
data class GoalTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goalId: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class GoalPaceMetrics(
    val daysRemaining: Int,
    val dailyRequired: Double,
    val weeklyRequired: Double,
    val monthlyRequired: Double,
    val isPastDeadline: Boolean
) {
    companion object {
        private const val MILLIS_PER_DAY = 86_400_000.0

        fun compute(goal: SavingsGoal, nowMillis: Long = System.currentTimeMillis()): GoalPaceMetrics? {
            val targetMillis = goal.targetDateMillis ?: return null
            val remainingMoney = goal.remainingAmount
            val diffMillis = targetMillis - nowMillis
            if (diffMillis <= 0L) {
                return GoalPaceMetrics(
                    daysRemaining = 0,
                    dailyRequired = remainingMoney,
                    weeklyRequired = remainingMoney,
                    monthlyRequired = remainingMoney,
                    isPastDeadline = remainingMoney > 0.0
                )
            }
            val days = ceil(diffMillis / MILLIS_PER_DAY).toInt().coerceAtLeast(1)
            val daily = remainingMoney / days.toDouble()
            val weeks = (days / 7.0).coerceAtLeast(1.0)
            val months = (days / 30.0).coerceAtLeast(1.0)
            return GoalPaceMetrics(
                daysRemaining = days,
                dailyRequired = daily,
                weeklyRequired = remainingMoney / weeks,
                monthlyRequired = remainingMoney / months,
                isPastDeadline = false
            )
        }
    }
}
