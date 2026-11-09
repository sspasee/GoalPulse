package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavingsGoal
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.parseHexColor
import com.example.util.MoneyFormatUtils
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGoalGauge(
    goal: SavingsGoal,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 276.dp
) {
    val accentColor = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)
    val lighterAccent = Color(
        red = (accentColor.red + 0.18f).coerceAtMost(1f),
        green = (accentColor.green + 0.18f).coerceAtMost(1f),
        blue = (accentColor.blue + 0.18f).coerceAtMost(1f),
        alpha = 1f
    )

    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "circular_gauge_progress"
    )

    val animatedAmount by animateFloatAsState(
        targetValue = goal.currentAmount.toFloat(),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "circular_gauge_amount"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickInactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .size(gaugeSize)
            .testTag("circular_goal_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.088f
            val outerPadding = strokeWidth * 1.15f
            val arcDiameter = size.minDimension - outerPadding * 2f
            val topLeft = Offset(
                x = (size.width - arcDiameter) / 2f,
                y = (size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = arcDiameter / 2f

            // 1. Subtle radial inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.18f),
                        accentColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.92f
                ),
                radius = radius * 0.92f,
                center = center
            )

            // 2. Outer scale tick marks (36 ticks along 270 degrees)
            val totalTicks = 36
            val tickInnerRadius = radius - strokeWidth * 0.85f
            val tickOuterRadius = radius - strokeWidth * 0.62f
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks.toFloat()
                val angleDeg = 135f + 270f * tickFraction
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val startOffset = Offset(
                    x = center.x + (tickInnerRadius * cos(angleRad)).toFloat(),
                    y = center.y + (tickInnerRadius * sin(angleRad)).toFloat()
                )
                val endOffset = Offset(
                    x = center.x + (tickOuterRadius * cos(angleRad)).toFloat(),
                    y = center.y + (tickOuterRadius * sin(angleRad)).toFloat()
                )
                val isReached = tickFraction <= animatedProgress
                drawLine(
                    color = if (isReached) accentColor.copy(alpha = 0.75f) else tickInactiveColor,
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = if (i % 6 == 0) 5f else 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Background 270-degree circular track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 4. Active progress arc + glowing tip dot
            val sweepAngle = 270f * animatedProgress
            if (sweepAngle > 0.5f) {
                // Soft glow under the progress arc
                drawArc(
                    color = accentColor.copy(alpha = 0.25f),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 1.45f, cap = StrokeCap.Round)
                )

                // Crisp gradient progress arc
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(accentColor, lighterAccent),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    ),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Glowing endpoint indicator dot
                val endAngleRad = Math.toRadians((135f + sweepAngle).toDouble())
                val tipCenter = Offset(
                    x = center.x + (radius * cos(endAngleRad)).toFloat(),
                    y = center.y + (radius * sin(endAngleRad)).toFloat()
                )
                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 0.28f,
                    center = tipCenter
                )
            }
        }

        // Content inside the circular gauge: Current saved amount & remaining amount
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = accentColor.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (goal.isCompleted) {
                            stringResource(R.string.goal_completed_badge)
                        } else {
                            "${goal.progressPercent}% • ${stringResource(R.string.saved_label)}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary requirement: Current saved amount inside the circular scale
            val formattedCurrent = MoneyFormatUtils.formatMoney(
                amount = animatedAmount.toDouble(),
                currencySymbol = goal.currencySymbol
            )
            Text(
                text = formattedCurrent,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = when {
                        formattedCurrent.length > 13 -> 21.sp
                        formattedCurrent.length > 10 -> 24.sp
                        else -> 28.sp
                    },
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("gauge_current_amount_text")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "из ${MoneyFormatUtils.formatMoney(goal.targetAmount, goal.currencySymbol)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = RobotoMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Remaining money pill inside the bottom of the ring
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("gauge_remaining_badge")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.remaining_prefix),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatUtils.formatMoney(goal.remainingAmount, goal.currencySymbol),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HomeWidgetPreviewCard(
    goal: SavingsGoal,
    isPinned: Boolean,
    onPinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Виджет рабочего стола Android",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color(0xFFF8FAFC)
                    )
                }

                FilledTonalButton(
                    onClick = onPinClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPinned) accentColor.copy(alpha = 0.22f) else Color(0xFF1E293B),
                        contentColor = if (isPinned) accentColor else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.testTag("pin_goal_to_widget_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPinned) stringResource(R.string.pinned_to_widget) else stringResource(R.string.pin_to_widget),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated Home Screen Widget Frame
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF162036))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Circular Scale inside Widget Preview
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = size.minDimension * 0.11f
                        drawArc(
                            color = Color(0xFF283753),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = 135f,
                            sweepAngle = 270f * goal.progressFraction,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = MoneyFormatUtils.formatCompactMoney(goal.currentAmount, goal.currencySymbol),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${goal.progressPercent}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Осталось: ${MoneyFormatUtils.formatMoney(goal.remainingAmount, goal.currencySymbol)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Цель: ${MoneyFormatUtils.formatMoney(goal.targetAmount, goal.currencySymbol)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
