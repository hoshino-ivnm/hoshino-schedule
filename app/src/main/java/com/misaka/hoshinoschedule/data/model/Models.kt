package com.misaka.hoshinoschedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val id: Long = 0,
    val name: String,
    val teacher: String? = null,
    val location: String? = null,
    val notes: String? = null,
    val colorHex: String? = null,
    val times: List<CourseTime> = emptyList()
)

@Serializable
data class CourseTime(
    val id: Long = 0,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weeks: List<Int> = emptyList()
)

@Serializable
data class PeriodDefinition(
    val id: Long = 0,
    val sequence: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val label: String? = null
)
