package com.example.widget

import android.content.Context
import com.example.data.model.SavingsGoal

data class WidgetGoalSnapshot(
    val goalId: String,
    val title: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val currencySymbol: String,
    val colorHex: String,
    val targetDateMillis: Long?
) {
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val progressFraction: Float
        get() = if (targetAmount > 0.0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = if (targetAmount > 0.0) ((currentAmount / targetAmount) * 100.0).toInt().coerceAtLeast(0) else 0
}

object WidgetSnapshotStore {
    private const val PREFS_NAME = "goalpulse_widget_prefs"
    private const val KEY_PINNED_GOAL_ID = "pinned_goal_id"
    private const val KEY_GOAL_ID = "goal_id"
    private const val KEY_TITLE = "title"
    private const val KEY_CURRENT_AMOUNT = "current_amount"
    private const val KEY_TARGET_AMOUNT = "target_amount"
    private const val KEY_CURRENCY = "currency_symbol"
    private const val KEY_COLOR_HEX = "color_hex"
    private const val KEY_TARGET_DATE = "target_date_millis"

    fun getPinnedGoalId(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PINNED_GOAL_ID, null)
    }

    fun setPinnedGoal(context: Context, goal: SavingsGoal) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PINNED_GOAL_ID, goal.id).apply()
        saveSnapshotAndRefreshWidget(appContext, goal)
    }

    fun saveSnapshotAndRefreshWidget(context: Context, goal: SavingsGoal?) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (goal == null) {
            prefs.edit()
                .remove(KEY_GOAL_ID)
                .remove(KEY_TITLE)
                .remove(KEY_CURRENT_AMOUNT)
                .remove(KEY_TARGET_AMOUNT)
                .remove(KEY_TARGET_DATE)
                .apply()
        } else {
            val editor = prefs.edit()
                .putString(KEY_GOAL_ID, goal.id)
                .putString(KEY_TITLE, goal.title)
                .putString(KEY_CURRENT_AMOUNT, goal.currentAmount.toString())
                .putString(KEY_TARGET_AMOUNT, goal.targetAmount.toString())
                .putString(KEY_CURRENCY, goal.currencySymbol)
                .putString(KEY_COLOR_HEX, goal.colorHex)
            if (goal.targetDateMillis != null) {
                editor.putLong(KEY_TARGET_DATE, goal.targetDateMillis)
            } else {
                editor.remove(KEY_TARGET_DATE)
            }
            editor.apply()
        }
        GoalSavingsWidgetProvider.updateAllWidgets(appContext)
    }

    fun loadSnapshot(context: Context): WidgetGoalSnapshot? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val goalId = prefs.getString(KEY_GOAL_ID, null) ?: return null
        val title = prefs.getString(KEY_TITLE, null) ?: return null
        val currentAmount = prefs.getString(KEY_CURRENT_AMOUNT, null)?.toDoubleOrNull() ?: 0.0
        val targetAmount = prefs.getString(KEY_TARGET_AMOUNT, null)?.toDoubleOrNull() ?: 0.0
        val currency = prefs.getString(KEY_CURRENCY, "₽") ?: "₽"
        val colorHex = prefs.getString(KEY_COLOR_HEX, "#10B981") ?: "#10B981"
        val targetDate = if (prefs.contains(KEY_TARGET_DATE)) prefs.getLong(KEY_TARGET_DATE, 0L) else null

        return WidgetGoalSnapshot(
            goalId = goalId,
            title = title,
            currentAmount = currentAmount,
            targetAmount = targetAmount,
            currencySymbol = currency,
            colorHex = colorHex,
            targetDateMillis = targetDate
        )
    }
}
