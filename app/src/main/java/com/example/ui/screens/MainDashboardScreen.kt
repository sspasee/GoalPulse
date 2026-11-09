package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.GoalPaceMetrics
import com.example.data.model.GoalTransaction
import com.example.data.model.SavingsGoal
import com.example.ui.components.CircularGoalGauge
import com.example.ui.components.CreateOrEditGoalSheet
import com.example.ui.components.CustomContributionDialog
import com.example.ui.components.HomeWidgetPreviewCard
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.SavingsViewModel
import com.example.util.MoneyFormatUtils

private val QuickDepositPresets = listOf(500.0, 1000.0, 5000.0, 10000.0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboardScreen(
    viewModel: SavingsViewModel,
    userEmail: String?,
    onSignOutClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateGoalSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var customContributionWithdrawalMode by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(uiState.errorMessage, uiState.statusMessage) {
        val message = uiState.errorMessage ?: uiState.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    if (showCreateGoalSheet) {
        CreateOrEditGoalSheet(
            existingGoal = null,
            onDismiss = { showCreateGoalSheet = false },
            onSave = { title, target, initial, currency, colorHex, targetDate ->
                showCreateGoalSheet = false
                viewModel.createGoal(
                    title = title,
                    targetAmount = target,
                    initialAmount = initial,
                    currencySymbol = currency,
                    colorHex = colorHex,
                    targetDateMillis = targetDate
                )
            }
        )
    }

    val goalToEdit = editingGoal
    if (goalToEdit != null) {
        CreateOrEditGoalSheet(
            existingGoal = goalToEdit,
            onDismiss = { editingGoal = null },
            onSave = { title, target, _, currency, colorHex, targetDate ->
                editingGoal = null
                viewModel.updateGoalDetails(
                    goalId = goalToEdit.id,
                    title = title,
                    targetAmount = target,
                    currencySymbol = currency,
                    colorHex = colorHex,
                    targetDateMillis = targetDate
                )
            },
            onDelete = { goal ->
                editingGoal = null
                viewModel.deleteGoal(goal)
            }
        )
    }

    val withdrawalMode = customContributionWithdrawalMode
    val currentActiveGoal = uiState.selectedGoal
    if (withdrawalMode != null && currentActiveGoal != null) {
        CustomContributionDialog(
            currencySymbol = currentActiveGoal.currencySymbol,
            initialIsWithdrawal = withdrawalMode,
            onDismiss = { customContributionWithdrawalMode = null },
            onConfirm = { amount, isWithdrawal, note ->
                customContributionWithdrawalMode = null
                viewModel.addCustomContribution(amount, isWithdrawal, note)
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGoalSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_goal")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_goal_button)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Top Header Bar
                item {
                    DashboardHeaderBar(
                        userEmail = userEmail,
                        onAddGoalClick = { showCreateGoalSheet = true },
                        onSignOutClick = onSignOutClick
                    )
                }

                // 2. Loading State or Empty State or Active Goal Content
                if (uiState.isLoading && uiState.goals.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.goals.isEmpty()) {
                    item {
                        EmptyGoalsHeroCard(
                            onCreateGoalClick = { showCreateGoalSheet = true },
                            onCreateDemoGoalClick = {
                                viewModel.createGoal(
                                    title = "Финансовая подушка",
                                    targetAmount = 150000.0,
                                    initialAmount = 45000.0,
                                    currencySymbol = "₽",
                                    colorHex = "#10B981",
                                    targetDateMillis = System.currentTimeMillis() + 86_400_000L * 90
                                )
                            }
                        )
                    }
                } else {
                    // 3. Multi-Goal Selector Row
                    item {
                        GoalsSelectorRow(
                            goals = uiState.goals,
                            selectedGoalId = currentActiveGoal?.id,
                            onSelectGoal = { viewModel.selectGoal(it) },
                            onAddGoalClick = { showCreateGoalSheet = true }
                        )
                    }

                    if (currentActiveGoal != null) {
                        // 4. Main Circular Gauge Hero Card
                        item {
                            MainGaugeDashboardCard(
                                goal = currentActiveGoal,
                                onEditClick = { editingGoal = currentActiveGoal }
                            )
                        }

                        // 5. Quick Contributions & Custom Deposit/Withdraw Section
                        item {
                            QuickContributionCard(
                                goal = currentActiveGoal,
                                isMutating = uiState.isMutating,
                                onQuickAdd = { preset ->
                                    val formatted = MoneyFormatUtils.formatMoney(preset, currentActiveGoal.currencySymbol)
                                    viewModel.addQuickContribution(
                                        amount = preset,
                                        label = "Быстрое пополнение +$formatted"
                                    )
                                },
                                onCustomDepositClick = { customContributionWithdrawalMode = false },
                                onCustomWithdrawClick = { customContributionWithdrawalMode = true }
                            )
                        }

                        // 6. Deadline Pace Calculation Card
                        item {
                            PaceCalculationCard(
                                goal = currentActiveGoal,
                                paceMetrics = uiState.paceMetrics,
                                onSetDeadlineClick = { editingGoal = currentActiveGoal }
                            )
                        }

                        // 7. Home Screen Widget Preview & Pin Card
                        item {
                            HomeWidgetPreviewCard(
                                goal = currentActiveGoal,
                                isPinned = uiState.pinnedGoalId == currentActiveGoal.id,
                                onPinClick = { viewModel.pinGoalToWidget(currentActiveGoal) }
                            )
                        }

                        // 8. Transactions History Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.history_section_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${uiState.selectedGoalTransactions.size} опер.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.selectedGoalTransactions.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.empty_history_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.selectedGoalTransactions,
                                key = { it.id }
                            ) { transaction ->
                                TransactionRowCard(
                                    transaction = transaction,
                                    currencySymbol = currentActiveGoal.currencySymbol,
                                    onDeleteClick = { viewModel.deleteTransaction(transaction) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardHeaderBar(
    userEmail: String?,
    onAddGoalClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!userEmail.isNullOrBlank()) {
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                onClick = onAddGoalClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("header_new_goal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Цель",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onSignOutClick,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.sign_out),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoalsSelectorRow(
    goals: List<SavingsGoal>,
    selectedGoalId: String?,
    onSelectGoal: (String) -> Unit,
    onAddGoalClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goals_selector_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items = goals, key = { it.id }) { goal ->
            val isSelected = goal.id == selectedGoalId
            val accentColor = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    accentColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                modifier = Modifier
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectGoal(goal.id) }
                    .testTag("goal_chip_${goal.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${goal.progressPercent}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .clickable { onAddGoalClick() }
                    .testTag("add_goal_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.new_goal_button),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.new_goal_button),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MainGaugeDashboardCard(
    goal: SavingsGoal,
    onEditClick: () -> Unit
) {
    val accentColor = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_gauge_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "АКТИВНАЯ ЦЕЛЬ СБОРА",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_active_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_goal_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Central Circular Scale with Current Saved Amount Inside
            CircularGoalGauge(
                goal = goal,
                gaugeSize = 276.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom 3-column metrics summary strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumnItem(
                    label = "Собрано",
                    value = MoneyFormatUtils.formatCompactMoney(goal.currentAmount, goal.currencySymbol),
                    valueColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                MetricColumnItem(
                    label = "Осталось",
                    value = MoneyFormatUtils.formatCompactMoney(goal.remainingAmount, goal.currencySymbol),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricColumnItem(
                    label = "Нужная сумма",
                    value = MoneyFormatUtils.formatCompactMoney(goal.targetAmount, goal.currencySymbol),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricColumnItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickContributionCard(
    goal: SavingsGoal,
    isMutating: Boolean,
    onQuickAdd: (Double) -> Unit,
    onCustomDepositClick: () -> Unit,
    onCustomWithdrawClick: () -> Unit
) {
    val accentColor = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_contribute_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDepositPresets.forEach { amount ->
                    val label = "+${MoneyFormatUtils.formatMoney(amount, goal.currencySymbol)}"
                    FilledTonalButton(
                        onClick = { onQuickAdd(amount) },
                        enabled = !isMutating,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accentColor.copy(alpha = 0.16f),
                            contentColor = accentColor
                        ),
                        modifier = Modifier.testTag("quick_add_${amount.toInt()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCustomDepositClick,
                    enabled = !isMutating,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("custom_deposit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.custom_deposit_button),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onCustomWithdrawClick,
                    enabled = !isMutating && goal.currentAmount > 0.0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("custom_withdraw_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.custom_withdraw_button),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaceCalculationCard(
    goal: SavingsGoal,
    paceMetrics: GoalPaceMetrics?,
    onSetDeadlineClick: () -> Unit
) {
    val accentColor = parseHexColor(goal.colorHex, MaterialTheme.colorScheme.primary)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pace_calculation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.pace_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (goal.targetDateMillis != null) {
                    Text(
                        text = "до ${MoneyFormatUtils.formatDate(goal.targetDateMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.clickable { onSetDeadlineClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (goal.isCompleted) {
                Text(
                    text = "Поздравляем! Нужная сумма полностью собрана.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor
                )
            } else if (paceMetrics == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Укажите желаемую дату завершения, чтобы рассчитать ежедневный и ежемесячный взнос.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onSetDeadlineClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Задать дату")
                    }
                }
            } else {
                Text(
                    text = if (paceMetrics.isPastDeadline) {
                        "Целевая дата наступила. Осталось накопить ${MoneyFormatUtils.formatMoney(goal.remainingAmount, goal.currencySymbol)}."
                    } else {
                        "Осталось ${paceMetrics.daysRemaining} дн. Чтобы успеть вовремя, откладывайте:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaceBoxItem(
                        period = "В день",
                        amount = MoneyFormatUtils.formatCompactMoney(paceMetrics.dailyRequired, goal.currencySymbol),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    PaceBoxItem(
                        period = "В неделю",
                        amount = MoneyFormatUtils.formatCompactMoney(paceMetrics.weeklyRequired, goal.currencySymbol),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    PaceBoxItem(
                        period = "В месяц",
                        amount = MoneyFormatUtils.formatCompactMoney(paceMetrics.monthlyRequired, goal.currencySymbol),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaceBoxItem(
    period: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = period,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransactionRowCard(
    transaction: GoalTransaction,
    currencySymbol: String,
    onDeleteClick: () -> Unit
) {
    val isDeposit = transaction.amount >= 0.0
    val amountColor = if (isDeposit) MaterialTheme.colorScheme.primary else CoralDanger
    val timestampMillis = transaction.createdAt?.toDate()?.time ?: System.currentTimeMillis()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDeposit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank {
                        if (isDeposit) "Пополнение сбора" else "Снятие со сбора"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = MoneyFormatUtils.formatDateTime(timestampMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = MoneyFormatUtils.formatSignedMoney(transaction.amount, currencySymbol),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_tx_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Отменить операцию",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyGoalsHeroCard(
    onCreateGoalClick: () -> Unit,
    onCreateDemoGoalClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_goals_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_goal_hero_1794204119129),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.empty_goals_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_goals_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onCreateGoalClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("empty_state_create_goal_button")
            ) {
                Icon(imageVector = Icons.Default.Flag, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Создать свою цель",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCreateDemoGoalClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_state_demo_goal_button")
            ) {
                Text("Заполнить пример цели (150 000 ₽)")
            }
        }
    }
}
