package com.misaka.kiraraschedule.util

import androidx.compose.ui.graphics.Color

fun parseHexColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    }.getOrNull()
}
