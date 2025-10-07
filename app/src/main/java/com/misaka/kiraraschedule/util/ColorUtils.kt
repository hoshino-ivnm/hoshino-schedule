package com.misaka.kiraraschedule.util

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

fun parseHexColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(clean.toColorInt())
    }.getOrNull()
}


fun Color.toHexString(includeHash: Boolean = true): String {
    val r = (red * 255f).roundToInt().coerceIn(0, 255)
    val g = (green * 255f).roundToInt().coerceIn(0, 255)
    val b = (blue * 255f).roundToInt().coerceIn(0, 255)
    val hex = "%02X%02X%02X".format(r, g, b)
    return if (includeHash) "#$hex" else hex
}
