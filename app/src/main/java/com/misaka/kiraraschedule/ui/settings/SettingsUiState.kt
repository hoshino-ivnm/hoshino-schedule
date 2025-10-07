package com.misaka.kiraraschedule.ui.settings

import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.settings.CourseDisplayField
import com.misaka.kiraraschedule.data.settings.UserPreferences

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val periods: List<PeriodDefinition> = emptyList(),
    val availableFields: List<CourseDisplayField> = CourseDisplayField.entries,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val message: String? = null
)

data class PeriodEditInput(
    val id: Long,
    val sequence: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val label: String?
) {
    fun toMinutesPair(): Pair<Int, Int> =
        (startHour * 60 + startMinute) to (endHour * 60 + endMinute)
}

fun PeriodDefinition.toEditInput(): PeriodEditInput = PeriodEditInput(
    id = id,
    sequence = sequence,
    startHour = startMinutes / 60,
    startMinute = startMinutes % 60,
    endHour = endMinutes / 60,
    endMinute = endMinutes % 60,
    label = label
)
