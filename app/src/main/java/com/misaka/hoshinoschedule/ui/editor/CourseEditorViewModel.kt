package com.misaka.hoshinoschedule.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misaka.hoshinoschedule.data.model.Course
import com.misaka.hoshinoschedule.data.model.CourseTime
import com.misaka.hoshinoschedule.data.repository.CourseRepository
import com.misaka.hoshinoschedule.data.repository.PeriodRepository
import com.misaka.hoshinoschedule.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CourseEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val periodRepository: PeriodRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val courseId: Long? = savedStateHandle.get<Long>(COURSE_ID_KEY)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        CourseEditorUiState(courseId = courseId)
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodRepository.observePeriods().collect { periods ->
                _uiState.update { state ->
                    state.copy(
                        periods = periods,
                        timeSlots = state.timeSlots.ifEmpty {
                            listOf(
                                TimeSlotInput(
                                    localId = UUID.randomUUID().toString(),
                                    dayOfWeek = periods.firstOrNull()?.sequence ?: 1,
                                    startPeriod = periods.firstOrNull()?.sequence ?: 1,
                                    endPeriod = periods.firstOrNull()?.sequence ?: 1,
                                    weeks = emptySet()
                                )
                            )
                        }
                    ).revalidate()
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
        if (courseId != null) {
            viewModelScope.launch { loadCourse(courseId) }
        }
    }

    private suspend fun loadCourse(id: Long) {
        val course = courseRepository.getCourse(id) ?: return
        _uiState.update {
            it.copy(
                name = course.name,
                teacher = course.teacher.orEmpty(),
                location = course.location.orEmpty(),
                notes = course.notes.orEmpty(),
                colorHex = course.colorHex,
                timeSlots = course.times.map { time ->
                    TimeSlotInput(
                        localId = UUID.randomUUID().toString(),
                        existingId = time.id.takeIf { idValue -> idValue > 0 },
                        dayOfWeek = time.dayOfWeek,
                        startPeriod = time.startPeriod,
                        endPeriod = time.endPeriod,
                        weeks = time.weeks.toSet()
                    )
                }
            ).ensureTimeSlots().revalidate()
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value).revalidate() }

    fun updateTeacher(value: String) = _uiState.update { it.copy(teacher = value) }

    fun updateLocation(value: String) = _uiState.update { it.copy(location = value) }

    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun updateColor(hex: String?) = _uiState.update { it.copy(colorHex = hex) }

    fun addTimeSlot() = _uiState.update { state ->
        val defaultPeriod = state.periods.firstOrNull()?.sequence ?: 1
        state.copy(
            timeSlots = state.timeSlots + TimeSlotInput(
                localId = UUID.randomUUID().toString(),
                dayOfWeek = 1,
                startPeriod = defaultPeriod,
                endPeriod = defaultPeriod,
                weeks = emptySet()
            )
        ).revalidate()
    }

    fun updateTimeSlot(
        localId: String,
        dayOfWeek: Int? = null,
        startPeriod: Int? = null,
        endPeriod: Int? = null
    ) {
        _uiState.update { state ->
            val updated = state.timeSlots.map { slot ->
                if (slot.localId != localId) return@map slot
                val newStart = startPeriod ?: slot.startPeriod
                val newEndPre = endPeriod ?: slot.endPeriod
                val normalizedEnd = if (newEndPre < newStart) newStart else newEndPre
                slot.copy(
                    dayOfWeek = dayOfWeek ?: slot.dayOfWeek,
                    startPeriod = newStart,
                    endPeriod = normalizedEnd
                )
            }
            state.copy(timeSlots = updated).revalidate()
        }
    }

    fun updateTimeSlotWeeks(localId: String, weeks: Set<Int>) {
        _uiState.update { state ->
            val updated = state.timeSlots.map { slot ->
                if (slot.localId != localId) return@map slot
                slot.copy(weeks = weeks)
            }
            state.copy(timeSlots = updated).revalidate()
        }
    }

    fun removeTimeSlot(localId: String) {
        _uiState.update { state ->
            val reduced = state.timeSlots.filterNot { it.localId == localId }
            state.copy(timeSlots = reduced.ifEmpty { state.timeSlots }).revalidate()
        }
    }

    fun deleteCourse(onFinished: () -> Unit) {
        val id = courseId ?: return
        viewModelScope.launch {
            courseRepository.getCourse(id)?.let { course ->
                courseRepository.delete(course)
            }
            onFinished()
        }
    }

    fun save(onFinished: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave || state.periods.isEmpty()) return
        val course = Course(
            id = state.courseId ?: 0,
            name = state.name.trim(),
            teacher = state.teacher.trim().ifBlank { null },
            location = state.location.trim().ifBlank { null },
            notes = state.notes.trim().ifBlank { null },
            colorHex = state.colorHex,
            times = state.timeSlots.map { slot ->
                CourseTime(
                    id = slot.existingId ?: 0,
                    dayOfWeek = slot.dayOfWeek,
                    startPeriod = slot.startPeriod,
                    endPeriod = slot.endPeriod,
                    weeks = slot.weeks.toList().sorted()
                )
            }
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            courseRepository.upsert(course)
            _uiState.update { it.copy(isSaving = false) }
            onFinished()
        }
    }

    private fun CourseEditorUiState.ensureTimeSlots(): CourseEditorUiState {
        return if (timeSlots.isEmpty()) {
            val defaultPeriod = periods.firstOrNull()?.sequence ?: 1
            copy(
                timeSlots = listOf(
                    TimeSlotInput(
                        localId = UUID.randomUUID().toString(),
                        dayOfWeek = 1,
                        startPeriod = defaultPeriod,
                        endPeriod = defaultPeriod,
                        weeks = emptySet()
                    )
                )
            )
        } else this
    }

    private fun CourseEditorUiState.revalidate(): CourseEditorUiState {
        val validTimeSlots = timeSlots.filter {
            it.startPeriod <= it.endPeriod
        }
        val canSaveCourse = name.isNotBlank() && validTimeSlots.isNotEmpty()
        return copy(canSave = canSaveCourse)
    }

    companion object {
        const val COURSE_ID_KEY = "courseId"
    }
}
