package com.example.ui.theme

import androidx.compose.ui.graphics.Color

var isDarkModeGlobal: Boolean = false

val BgDeep: Color
    get() = if (isDarkModeGlobal) Color(0xFF090D16) else Color(0xFFF8FAFC)       // Deep navy/black outer space background

val BgDark: Color
    get() = if (isDarkModeGlobal) Color(0xFF111827) else Color(0xFFF1F5F9)       // Deep grey background

val BgCard: Color
    get() = if (isDarkModeGlobal) Color(0xFF1E293B) else Color(0xFFFFFFFF)       // Darker slate card background

val BgHover: Color
    get() = if (isDarkModeGlobal) Color(0xFF334155) else Color(0xFFE2E8F0)      // Slate 700 selected state

val BorderColor: Color
    get() = if (isDarkModeGlobal) Color(0xFF334155) else Color(0xFFE2E8F0)  // Subtle dark border

val TextPrimary: Color
    get() = if (isDarkModeGlobal) Color(0xFFF8FAFC) else Color(0xFF0F172A)  // Very light gray for dark mode readability

val TextSecondary: Color
    get() = if (isDarkModeGlobal) Color(0xFFCBD5E1) else Color(0xFF475569) // Light grey info text

val TextMuted: Color
    get() = if (isDarkModeGlobal) Color(0xFF64748B) else Color(0xFF94A3B8)     // Soft slate for secondary labels

val HaikalAccent: Color
    get() = if (isDarkModeGlobal) Color(0xFF38BDF8) else Color(0xFF0284C7)  // High-contrast vibrant sky blue

val UmmuAccent: Color
    get() = if (isDarkModeGlobal) Color(0xFFF472B6) else Color(0xFFDB2777)    // High-contrast pink

val CombinedAccent: Color
    get() = if (isDarkModeGlobal) Color(0xFF818CF8) else Color(0xFF4F46E5) // Vibrant indigo

val SuccessAccent: Color
    get() = if (isDarkModeGlobal) Color(0xFF34D399) else Color(0xFF059669) // Emerald state

val DangerAccent: Color
    get() = if (isDarkModeGlobal) Color(0xFFF87171) else Color(0xFFDC2626)  // Crimson state

val WarningAccent: Color
    get() = if (isDarkModeGlobal) Color(0xFFFBBF24) else Color(0xFFD97706) // Rich amber gold


