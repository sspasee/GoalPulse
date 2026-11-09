package com.example.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.util.MoneyFormatUtils

object WidgetRingBitmapRenderer {

    fun renderCircularGaugeBitmap(
        currentAmount: Double,
        targetAmount: Double,
        currencySymbol: String,
        colorHex: String,
        sizePx: Int = 420
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryColor = try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#10B981")
        }

        val strokeWidth = sizePx * 0.095f
        val padding = strokeWidth * 1.15f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // 1. Background inner glow disc
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(28, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx / 2f) - strokeWidth * 1.55f, discPaint)

        // 2. Track ring
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#23324D")
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        // 3. Progress arc
        val progress = if (targetAmount > 0.0) {
            (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        val sweepAngle = 270f * progress
        if (sweepAngle > 0.5f) {
            val lighterColor = Color.rgb(
                (Color.red(primaryColor) + 55).coerceAtMost(255),
                (Color.green(primaryColor) + 55).coerceAtMost(255),
                (Color.blue(primaryColor) + 55).coerceAtMost(255)
            )
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                shader = LinearGradient(
                    0f,
                    sizePx.toFloat(),
                    sizePx.toFloat(),
                    0f,
                    primaryColor,
                    lighterColor,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawArc(arcRect, 135f, sweepAngle, false, progressPaint)
        }

        // 4. Center text: "СОБРАНО" label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.062f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("СОБРАНО", sizePx / 2f, sizePx * 0.37f, labelPaint)

        // 5. Center text: Current saved amount inside the ring
        val amountText = MoneyFormatUtils.formatCompactMoney(currentAmount, currencySymbol)
        val dynamicSizeFactor = when {
            amountText.length > 13 -> 0.085f
            amountText.length > 10 -> 0.102f
            else -> 0.122f
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = sizePx * dynamicSizeFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(amountText, sizePx / 2f, sizePx * 0.53f, amountPaint)

        // 6. Percentage pill inside the bottom of the ring
        val percent = if (targetAmount > 0.0) ((currentAmount / targetAmount) * 100.0).toInt().coerceAtLeast(0) else 0
        val percentText = "$percent%"
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.078f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(percentText, sizePx / 2f, sizePx * 0.69f, percentPaint)

        return bitmap
    }
}
