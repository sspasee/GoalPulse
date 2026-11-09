package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.util.MoneyFormatUtils

class GoalSavingsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val componentName = ComponentName(appContext, GoalSavingsWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateAppWidget(appContext, manager, id)
                }
            }
        }

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snapshot = WidgetSnapshotStore.loadSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_goal_savings)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_add_button, pendingIntent)

            if (snapshot != null) {
                val ringBitmap = WidgetRingBitmapRenderer.renderCircularGaugeBitmap(
                    currentAmount = snapshot.currentAmount,
                    targetAmount = snapshot.targetAmount,
                    currencySymbol = snapshot.currencySymbol,
                    colorHex = snapshot.colorHex
                )
                views.setImageViewBitmap(R.id.widget_ring_image, ringBitmap)
                views.setTextViewText(R.id.widget_goal_title, snapshot.title)
                val remainingStr = MoneyFormatUtils.formatMoney(snapshot.remainingAmount, snapshot.currencySymbol)
                val targetStr = MoneyFormatUtils.formatMoney(snapshot.targetAmount, snapshot.currencySymbol)
                views.setTextViewText(R.id.widget_remaining_value, "Осталось: $remainingStr")
                views.setTextViewText(R.id.widget_target_value, "Цель: $targetStr")
            } else {
                val emptyRing = WidgetRingBitmapRenderer.renderCircularGaugeBitmap(
                    currentAmount = 0.0,
                    targetAmount = 100000.0,
                    currencySymbol = "₽",
                    colorHex = "#10B981"
                )
                views.setImageViewBitmap(R.id.widget_ring_image, emptyRing)
                views.setTextViewText(R.id.widget_goal_title, "Создайте цель в GoalPulse")
                views.setTextViewText(R.id.widget_remaining_value, "Нажмите, чтобы выбрать цель")
                views.setTextViewText(R.id.widget_target_value, "Круговая шкала сбора")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
