package com.misaka.kiraraschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.repository.CourseRepository
import com.misaka.kiraraschedule.data.repository.PeriodRepository
import com.misaka.kiraraschedule.data.repository.SettingsRepository
import com.misaka.kiraraschedule.data.work.ReminderScheduler
import com.misaka.kiraraschedule.util.computeWeekNumber
import com.misaka.kiraraschedule.util.termStartDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleViewModel(
    private val courseRepository: CourseRepository,
    private val periodRepository: PeriodRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val refreshEvents = MutableStateFlow(0)

    val uiState: StateFlow<ScheduleUiState> = refreshEvents.flatMapLatest {
        combine(
            courseRepository.observeAllCourses(),
            periodRepository.observePeriods(),
            settingsRepository.preferences
        ) { courses, periods, preferences ->
            val periodMap = periods.associateBy { it.sequence }
            val days = DayOfWeek.values().map { day ->
                val dayValue = day.value
                val items = courses.flatMap { course ->
                    course.times.filter { it.dayOfWeek == dayValue }.mapNotNull { time ->
                        val start =
                            periodMap[time.startPeriod]?.startMinutes ?: return@mapNotNull null
                        val end = periodMap[time.endPeriod]?.endMinutes ?: return@mapNotNull null
                        DayScheduleItem(
                            course = course,
                            time = time,
                            startMinutes = start,
                            endMinutes = end
                        )
                    }
                }.sortedBy { it.startMinutes }
                DaySchedule(day, items)
            }
            val termStart = preferences.termStartDate()
            val today = LocalDate.now()
            val totalWeeks = preferences.totalWeeks.coerceAtLeast(1)
            val currentWeek = computeWeekNumber(termStart, today)
            ScheduleUiState(
                isLoading = false,
                periods = periods,
                preferences = preferences,
                days = days,
                termStartDate = termStart,
                totalWeeks = totalWeeks,
                currentWeekNumber = currentWeek
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleUiState()
    )

    init {
        viewModelScope.launch {
            periodRepository.ensureDefaults()
        }
        viewModelScope.launch {
            uiState.collect { state ->
                val prefs = state.preferences ?: return@collect
                if (state.periods.isEmpty()) return@collect
                reminderScheduler.rescheduleAll(
                    courses = state.days.flatMap { day -> day.items.map { it.course } }
                        .distinctBy { it.id },
                    periods = state.periods,
                    preferences = prefs
                )
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            courseRepository.delete(course)
            triggerRefresh()
        }
    }

    fun triggerRefresh() {
        refreshEvents.value += 1
    }
}
