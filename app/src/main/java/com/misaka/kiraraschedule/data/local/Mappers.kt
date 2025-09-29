package com.misaka.kiraraschedule.data.local

import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.model.CourseTime
import com.misaka.kiraraschedule.data.model.PeriodDefinition

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
    endPeriod = endPeriod
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
    endPeriod = endPeriod
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
