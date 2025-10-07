package com.misaka.kiraraschedule.ui.schedule

import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.model.CourseTime
import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.settings.UserPreferences
import java.time.DayOfWeek
import java.time.LocalDate

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val periods: List<PeriodDefinition> = emptyList(),
    val preferences: UserPreferences? = null,
    val days: List<DaySchedule> = emptyList(),
    val termStartDate: LocalDate? = null,
    val totalWeeks: Int = 20,
    val currentWeekNumber: Int? = null
)

data class DaySchedule(
    val dayOfWeek: DayOfWeek,
    val items: List<DayScheduleItem>
)

data class DayScheduleItem(
    val course: Course,
    val time: CourseTime,
    val startMinutes: Int,
    val endMinutes: Int
)
