package com.misaka.kiraraschedule.data.repository

import com.misaka.kiraraschedule.data.settings.BackgroundMode
import com.misaka.kiraraschedule.data.settings.CourseDisplayField
import com.misaka.kiraraschedule.data.settings.SettingsDataSource
import com.misaka.kiraraschedule.data.settings.UserPreferences
import kotlinx.coroutines.flow.Flow

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

    suspend fun setDndConfig(enabled: Boolean, lead: Int, release: Int, skipThreshold: Int) = dataSource.update {
        it.copy(
            dndEnabled = enabled,
            dndLeadMinutes = lead,
            dndReleaseMinutes = release,
            dndSkipBreakThresholdMinutes = skipThreshold
        )
    }

    suspend fun setWeekendVisibility(showSaturday: Boolean, showSunday: Boolean) =
        dataSource.update { it.copy(showSaturday = showSaturday, showSunday = showSunday) }

    suspend fun replaceAll(preferences: UserPreferences) = dataSource.update { preferences }
}
