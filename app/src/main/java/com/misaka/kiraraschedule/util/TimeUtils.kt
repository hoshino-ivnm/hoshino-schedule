package com.misaka.kiraraschedule.util

fun minutesToTimeText(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}
