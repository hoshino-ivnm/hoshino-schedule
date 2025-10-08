package com.misaka.hoshinoschedule.data.local

import com.misaka.hoshinoschedule.data.model.Course
import com.misaka.hoshinoschedule.data.model.CourseTime
import com.misaka.hoshinoschedule.data.model.PeriodDefinition

fun CourseWithTimes.toModel(): Course = Course(
    id = course.id,
    name = course.name,
    teacher = course.teacher,
    location = course.location,
    notes = course.notes,
    colorHex = course.colorHex,
    times = times.map { it.toModel() }
)

fun CourseTimeEntity.toModel(): CourseTime = CourseTime(
    id = id,
    dayOfWeek = dayOfWeek,
    startPeriod = startPeriod,
    endPeriod = endPeriod,
    weeks = weeks.toWeekList()
)

fun Course.toEntity(): CourseEntity = CourseEntity(
    id = id,
    name = name,
    teacher = teacher,
    location = location,
    notes = notes,
    colorHex = colorHex
)

fun CourseTime.toEntity(courseId: Long): CourseTimeEntity = CourseTimeEntity(
    id = id,
    courseId = courseId,
    dayOfWeek = dayOfWeek,
    startPeriod = startPeriod,
    endPeriod = endPeriod,
    weeks = weeks.toWeekString()
)

fun PeriodDefinitionEntity.toModel(): PeriodDefinition = PeriodDefinition(
    id = id,
    sequence = sequence,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    label = label
)

fun PeriodDefinition.toEntity(): PeriodDefinitionEntity = PeriodDefinitionEntity(
    id = id,
    sequence = sequence,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    label = label
)


private fun String?.toWeekList(): List<Int> = this
    ?.split(',')
    ?.mapNotNull { it.trim().toIntOrNull() }
    ?.filter { it > 0 }
    ?.distinct()
    ?.sorted()
    ?: emptyList()

private fun List<Int>.toWeekString(): String? = this
    .filter { it > 0 }
    .distinct()
    .sorted()
    .takeIf { it.isNotEmpty() }
    ?.joinToString(separator = ",")
