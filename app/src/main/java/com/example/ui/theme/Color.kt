package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian & Emerald Wealth Palette
val ObsidianBackground = Color(0xFF0B111E)
val ObsidianSurface = Color(0xFF131C2E)
val ObsidianSurfaceVariant = Color(0xFF1C2840)
val ObsidianOutline = Color(0xFF2B3B59)

val LightBackground = Color(0xFFF4F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EEF6)
val LightOutline = Color(0xFFCBD5E1)

val EmeraldPrimary = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF059669)
val EmeraldContainerDark = Color(0xFF064E3B)
val EmeraldContainerLight = Color(0xFFD1FAE5)

val GoldAccent = Color(0xFFF59E0B)
val GoldLight = Color(0xFFFBBF24)
val CoralDanger = Color(0xFFEF4444)
val IndigoSecondary = Color(0xFF6366F1)

val GoalAccentColors = listOf(
    "#10B981" to "Изумруд",
    "#3B82F6" to "Сапфир",
    "#F59E0B" to "Золото",
    "#EC4899" to "Рубин",
    "#8B5CF6" to "Аметист",
    "#06B6D4" to "Бирюза"
)

fun parseHexColor(hex: String, fallback: Color = EmeraldPrimary): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
