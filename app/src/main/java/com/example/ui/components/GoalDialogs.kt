package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SavingsGoal
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GoalAccentColors
import com.example.ui.theme.parseHexColor
import com.example.util.MoneyFormatUtils

private val SupportedCurrencies = listOf("₽", "$", "€", "₸", "₴")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditGoalSheet(
    existingGoal: SavingsGoal? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        targetAmount: Double,
        initialAmount: Double,
        currencySymbol: String,
        colorHex: String,
        targetDateMillis: Long?
    ) -> Unit,
    onDelete: ((SavingsGoal) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(existingGoal) { mutableStateOf(existingGoal?.title ?: "") }
    var targetAmountText by remember(existingGoal) {
        mutableStateOf(
            existingGoal?.targetAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var initialAmountText by remember(existingGoal) { mutableStateOf("") }
    var selectedCurrency by remember(existingGoal) {
        mutableStateOf(existingGoal?.currencySymbol ?: "₽")
    }
    var selectedColorHex by remember(existingGoal) {
        mutableStateOf(existingGoal?.colorHex ?: GoalAccentColors.first().first)
    }
    var targetDateMillis by remember(existingGoal) {
        mutableStateOf(existingGoal?.targetDateMillis)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDateMillis ?: (System.currentTimeMillis() + 86_400_000L * 30)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingGoal == null) "Новая финансовая цель" else "Редактировать цель",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = { Text("Название цели (например, Отпуск, Автомобиль)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetAmountText,
                onValueChange = {
                    targetAmountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                    validationError = null
                },
                label = { Text("Целевая сумма сбора ($selectedCurrency)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_target_amount_input")
            )

            if (existingGoal == null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = initialAmountText,
                    onValueChange = {
                        initialAmountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                    },
                    label = { Text("Уже накоплено (необязательно, $selectedCurrency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_initial_amount_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Валюта",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupportedCurrencies.forEach { symbol ->
                    val selected = symbol == selectedCurrency
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCurrency = symbol },
                        label = {
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Целевая дата завершения сбора",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("pick_target_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = targetDateMillis?.let { MoneyFormatUtils.formatDate(it) } ?: "Выбрать дату"
                    )
                }

                DatePresetChip(label = "+1 мес", days = 30) { targetDateMillis = it }
                DatePresetChip(label = "+3 мес", days = 90) { targetDateMillis = it }
                DatePresetChip(label = "+6 мес", days = 180) { targetDateMillis = it }
                DatePresetChip(label = "+1 год", days = 365) { targetDateMillis = it }

                if (targetDateMillis != null) {
                    TextButton(onClick = { targetDateMillis = null }) {
                        Text("Сбросить")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Цвет круговой шкалы",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoalAccentColors.forEach { (hex, label) ->
                    val swatchColor = parseHexColor(hex)
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) swatchColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) swatchColor else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedColorHex = hex }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(swatchColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (existingGoal != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(existingGoal) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("delete_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить цель"
                        )
                    }
                }

                Button(
                    onClick = {
                        val cleanTitle = title.trim()
                        val parsedTarget = targetAmountText.replace(',', '.').toDoubleOrNull()
                        val parsedInitial = initialAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        when {
                            cleanTitle.isEmpty() -> {
                                validationError = "Введите название цели"
                            }
                            parsedTarget == null || parsedTarget <= 0.0 -> {
                                validationError = "Введите корректную целевую сумму больше 0"
                            }
                            parsedInitial < 0.0 -> {
                                validationError = "Стартовая сумма не может быть отрицательной"
                            }
                            else -> {
                                onSave(
                                    cleanTitle,
                                    parsedTarget,
                                    parsedInitial,
                                    selectedCurrency,
                                    selectedColorHex,
                                    targetDateMillis
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_goal_button")
                ) {
                    Text(
                        text = if (existingGoal == null) "Создать цель" else "Сохранить изменения",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DatePresetChip(
    label: String,
    days: Int,
    onSelect: (Long) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable {
            onSelect(System.currentTimeMillis() + days.toLong() * 86_400_000L)
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun CustomContributionDialog(
    currencySymbol: String,
    initialIsWithdrawal: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, isWithdrawal: Boolean, note: String) -> Unit
) {
    var isWithdrawal by remember { mutableStateOf(initialIsWithdrawal) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isWithdrawal) "Снять со сбора" else "Пополнить сбор",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isWithdrawal,
                        onClick = { isWithdrawal = false },
                        label = { Text("Пополнение +") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isWithdrawal,
                        onClick = { isWithdrawal = true },
                        label = { Text("Снятие −") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        errorText = null
                    },
                    label = { Text("Сумма ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_contribution_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Комментарий (необязательно)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_contribution_note_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        errorText = "Введите сумму больше 0"
                    } else {
                        onConfirm(parsed, isWithdrawal, noteText)
                    }
                },
                modifier = Modifier.testTag("confirm_contribution_button")
            ) {
                Text(if (isWithdrawal) "Снять" else "Внести")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
