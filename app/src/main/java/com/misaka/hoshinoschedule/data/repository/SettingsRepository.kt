package com.misaka.hoshinoschedule.data.repository

import com.misaka.hoshinoschedule.data.settings.BackgroundMode
import com.misaka.hoshinoschedule.data.settings.CourseDisplayField
import com.misaka.hoshinoschedule.data.settings.SettingsDataSource
import com.misaka.hoshinoschedule.data.settings.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class SettingsRepository(private val dataSource: SettingsDataSource) {

    val preferences: Flow<UserPreferences> = dataSource.preferences

    suspend fun setTimetableName(name: String) = dataSource.update { it.copy(timetableName = name) }

    suspend fun setBackground(mode: BackgroundMode, value: String) = dataSource.update {
        it.copy(backgroundMode = mode, backgroundValue = value)
    }

    suspend fun setVisibleFields(fields: Set<CourseDisplayField>) = dataSource.update {
        it.copy(visibleFields = fields)
    }

    suspend fun setReminderLeadMinutes(minutes: Int) = dataSource.update {
        it.copy(reminderLeadMinutes = minutes)
    }

    suspend fun setDndConfig(enabled: Boolean, lead: Int, release: Int, skipThreshold: Int) =
        dataSource.update {
            it.copy(
                dndEnabled = enabled,
                dndLeadMinutes = lead,
                dndReleaseMinutes = release,
                dndSkipBreakThresholdMinutes = skipThreshold
            )
        }

    suspend fun setWeekendVisibility(showSaturday: Boolean, showSunday: Boolean) =
        dataSource.update { it.copy(showSaturday = showSaturday, showSunday = showSunday) }

    suspend fun setHideEmptyWeekend(hide: Boolean) = dataSource.update {
        it.copy(hideEmptyWeekends = hide)
    }

    suspend fun setTermStartDate(date: LocalDate?) = dataSource.update {
        it.copy(termStartDateIso = date?.toString())
    }

    suspend fun setTotalWeeks(weeks: Int) = dataSource.update {
        it.copy(totalWeeks = weeks.coerceAtLeast(1))
    }

    suspend fun setShowNonCurrentWeekCourses(show: Boolean) = dataSource.update {
        it.copy(showNonCurrentWeekCourses = show)
    }

    suspend fun setDeveloperMode(enabled: Boolean) = dataSource.update {
        it.copy(developerModeEnabled = enabled)
    }

    suspend fun setDeveloperTestNotificationDelay(seconds: Int) = dataSource.update {
        it.copy(developerTestNotificationDelaySeconds = seconds.coerceIn(0, 3600))
    }

    suspend fun setDeveloperTestDndDuration(minutes: Int) = dataSource.update {
        it.copy(developerTestDndDurationMinutes = minutes.coerceIn(1, 240))
    }

    suspend fun setDeveloperAutoDisableDnd(enabled: Boolean) = dataSource.update {
        it.copy(developerAutoDisableDnd = enabled)
    }

    suspend fun setDeveloperTestDndGap(minutes: Int) = dataSource.update {
        it.copy(developerTestDndGapMinutes = minutes.coerceIn(0, 240))
    }

    suspend fun setDeveloperTestDndSkipThreshold(minutes: Int) = dataSource.update {
        it.copy(developerTestDndSkipThresholdMinutes = minutes.coerceIn(0, 240))
    }

    suspend fun replaceAll(preferences: UserPreferences) = dataSource.update { preferences }
}
