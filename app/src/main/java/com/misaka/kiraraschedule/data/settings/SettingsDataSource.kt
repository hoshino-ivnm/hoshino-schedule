package com.misaka.kiraraschedule.data.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
enum class BackgroundMode { COLOR, IMAGE }

@Serializable
enum class CourseDisplayField { NAME, TEACHER, LOCATION, NOTES }

@Serializable
data class UserPreferences(
    val timetableName: String = "My Timetable",
    val backgroundMode: BackgroundMode = BackgroundMode.COLOR,
    val backgroundValue: String = "#FFBB86FC",
    val visibleFields: Set<CourseDisplayField> = setOf(CourseDisplayField.NAME, CourseDisplayField.TEACHER),
    val reminderLeadMinutes: Int = 10,
    val dndEnabled: Boolean = false,
    val dndLeadMinutes: Int = 5,
    val dndReleaseMinutes: Int = 5,
    val dndSkipBreakThresholdMinutes: Int = 15,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true
)

class SettingsDataSource(private val dataStore: androidx.datastore.core.DataStore<Preferences>) {

    private val timetableNameKey = stringPreferencesKey("timetable_name")
    private val backgroundModeKey = stringPreferencesKey("background_mode")
    private val backgroundValueKey = stringPreferencesKey("background_value")
    private val visibleFieldsKey = stringPreferencesKey("visible_fields")
    private val reminderLeadMinutesKey = intPreferencesKey("reminder_lead_minutes")
    private val dndEnabledKey = intPreferencesKey("dnd_enabled")
    private val dndLeadMinutesKey = intPreferencesKey("dnd_lead_minutes")
    private val dndReleaseMinutesKey = intPreferencesKey("dnd_release_minutes")
    private val dndSkipThresholdKey = intPreferencesKey("dnd_skip_threshold")
    private val showSaturdayKey = intPreferencesKey("show_saturday")
    private val showSundayKey = intPreferencesKey("show_sunday")

    private val defaults = UserPreferences()

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs -> prefs.toModel() }

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { prefs ->
            val current = prefs.toModel()
            val updated = transform(current)
            prefs[timetableNameKey] = updated.timetableName
            prefs[backgroundModeKey] = updated.backgroundMode.name
            prefs[backgroundValueKey] = updated.backgroundValue
            prefs[visibleFieldsKey] = updated.visibleFields.joinToString(",") { it.name }
            prefs[reminderLeadMinutesKey] = updated.reminderLeadMinutes
            prefs[dndEnabledKey] = if (updated.dndEnabled) 1 else 0
            prefs[dndLeadMinutesKey] = updated.dndLeadMinutes
            prefs[dndReleaseMinutesKey] = updated.dndReleaseMinutes
            prefs[dndSkipThresholdKey] = updated.dndSkipBreakThresholdMinutes
            prefs[showSaturdayKey] = if (updated.showSaturday) 1 else 0
            prefs[showSundayKey] = if (updated.showSunday) 1 else 0
        }
    }

    private fun Preferences.toModel(): UserPreferences {
        val visible = this[visibleFieldsKey]
            ?.split(',')
            ?.mapNotNull { runCatching { CourseDisplayField.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: defaults.visibleFields
        return UserPreferences(
            timetableName = this[timetableNameKey] ?: defaults.timetableName,
            backgroundMode = this[backgroundModeKey]?.let { runCatching { BackgroundMode.valueOf(it) }.getOrNull() }
                ?: defaults.backgroundMode,
            backgroundValue = this[backgroundValueKey] ?: defaults.backgroundValue,
            visibleFields = visible,
            reminderLeadMinutes = this[reminderLeadMinutesKey] ?: defaults.reminderLeadMinutes,
            dndEnabled = (this[dndEnabledKey] ?: if (defaults.dndEnabled) 1 else 0) == 1,
            dndLeadMinutes = this[dndLeadMinutesKey] ?: defaults.dndLeadMinutes,
            dndReleaseMinutes = this[dndReleaseMinutesKey] ?: defaults.dndReleaseMinutes,
            dndSkipBreakThresholdMinutes = this[dndSkipThresholdKey] ?: defaults.dndSkipBreakThresholdMinutes,
            showSaturday = (this[showSaturdayKey] ?: if (defaults.showSaturday) 1 else 0) == 1,
            showSunday = (this[showSundayKey] ?: if (defaults.showSunday) 1 else 0) == 1
        )
    }
}
