package com.misaka.kiraraschedule.ui.editor

import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.settings.UserPreferences

data class CourseEditorUiState(
    val courseId: Long? = null,
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val notes: String = "",
    val colorHex: String? = null,
    val timeSlots: List<TimeSlotInput> = emptyList(),
    val periods: List<PeriodDefinition> = emptyList(),
    val preferences: UserPreferences? = null,
    val isSaving: Boolean = false,
    val canSave: Boolean = false
)

data class TimeSlotInput(
    val localId: String,
    val existingId: Long? = null,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int
)
