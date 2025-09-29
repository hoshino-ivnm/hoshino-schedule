package com.misaka.kiraraschedule.data.work

import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.model.PeriodDefinition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class ScheduledClass(
    val course: Course,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
)

class SchedulePlanner {

    fun buildUpcomingClasses(
        courses: List<Course>,
        periods: List<PeriodDefinition>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        startDate: LocalDate = LocalDate.now(zoneId),
        daysAhead: Long = 14
    ): List<ScheduledClass> {
        if (periods.isEmpty()) return emptyList()
        val periodMap = periods.associateBy { it.sequence }
        val results = mutableListOf<ScheduledClass>()
        val endDateExclusive = startDate.plusDays(daysAhead + 1)

        var date = startDate
        while (date.isBefore(endDateExclusive)) {
            val dayValue = date.dayOfWeek.value
            courses.forEach { course ->
                course.times.filter { it.dayOfWeek == dayValue }.forEach { time ->
                    val startPeriod = periodMap[time.startPeriod] ?: return@forEach
                    val endPeriod = periodMap[time.endPeriod] ?: return@forEach
                    val startLocalTime = minutesToLocalDateTime(date, startPeriod.startMinutes)
                    val endLocalTime = minutesToLocalDateTime(date, endPeriod.endMinutes)
                    results += ScheduledClass(
                        course = course,
                        startDateTime = startLocalTime.atZone(zoneId),
                        endDateTime = endLocalTime.atZone(zoneId)
                    )
                }
            }
            date = date.plusDays(1)
        }
        return results.sortedBy { it.startDateTime }
    }

    private fun minutesToLocalDateTime(date: LocalDate, minutes: Int): LocalDateTime {
        val hour = minutes / 60
        val minute = minutes % 60
        return date.atTime(hour, minute)
    }
}
