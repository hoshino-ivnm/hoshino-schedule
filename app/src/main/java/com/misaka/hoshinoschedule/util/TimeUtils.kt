package com.misaka.hoshinoschedule.util

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun minutesToTimeText(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}
