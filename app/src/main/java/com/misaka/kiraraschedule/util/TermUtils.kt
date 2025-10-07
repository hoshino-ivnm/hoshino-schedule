package com.misaka.kiraraschedule.util

import com.misaka.kiraraschedule.data.settings.UserPreferences
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun UserPreferences.termStartDate(): LocalDate? = termStartDateIso
    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun computeWeekNumber(termStart: LocalDate?, date: LocalDate): Int? {
    val start = termStart ?: return null
    val days = ChronoUnit.DAYS.between(start, date)
    if (days < 0) return null
    return (days / 7).toInt() + 1
}

fun computeWeekNumber(termStart: LocalDate?, date: LocalDate, offsetWeeks: Int): Int? {
    return computeWeekNumber(termStart, date)?.let { it + offsetWeeks }
}

fun weekWithinTerm(termStart: LocalDate?, totalWeeks: Int, date: LocalDate): Boolean {
    val start = termStart ?: return true
    val week = computeWeekNumber(start, date)
    return week != null && week in 1..totalWeeks.coerceAtLeast(1)
}

fun isTimeActiveInWeek(weeks: List<Int>, weekNumber: Int?): Boolean {
    if (weeks.isEmpty()) return true
    return weekNumber?.let { it in weeks } ?: true
}
