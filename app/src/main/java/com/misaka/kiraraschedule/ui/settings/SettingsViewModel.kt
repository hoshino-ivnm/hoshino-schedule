package com.misaka.kiraraschedule.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.repository.DataTransferRepository
import com.misaka.kiraraschedule.data.repository.PeriodRepository
import com.misaka.kiraraschedule.data.repository.SettingsRepository
import com.misaka.kiraraschedule.data.settings.BackgroundMode
import com.misaka.kiraraschedule.data.settings.CourseDisplayField
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val periodRepository: PeriodRepository,
    private val dataTransferRepository: DataTransferRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.preferences,
        periodRepository.observePeriods()
    ) { prefs, periods ->
        SettingsUiState(
            preferences = prefs,
            periods = periods
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setTimetableName(name: String) {
        viewModelScope.launch { settingsRepository.setTimetableName(name) }
    }

    fun setBackgroundColor(hex: String) {
        viewModelScope.launch { settingsRepository.setBackground(BackgroundMode.COLOR, hex) }
    }

    fun setBackgroundImage(uri: String) {
        viewModelScope.launch { settingsRepository.setBackground(BackgroundMode.IMAGE, uri) }
    }

    fun setVisibleFields(fields: Set<CourseDisplayField>) {
        viewModelScope.launch { settingsRepository.setVisibleFields(fields) }
    }

    fun setReminderLead(minutes: Int) {
        viewModelScope.launch { settingsRepository.setReminderLeadMinutes(minutes) }
    }

    fun setDndConfig(enabled: Boolean, lead: Int, release: Int, threshold: Int) {
        viewModelScope.launch { settingsRepository.setDndConfig(enabled, lead, release, threshold) }
    }

    fun addPeriod() {
        val current = uiState.value.periods
        val nextSequence = current.size + 1
        val start = current.lastOrNull()?.endMinutes ?: (8 * 60)
        val end = start + 45
        val period = PeriodDefinition(
            id = 0,
            sequence = nextSequence,
            startMinutes = start,
            endMinutes = end,
            label = "Period $nextSequence"
        )
        viewModelScope.launch {
            periodRepository.replaceAll(current + period)
        }
    }

    fun updatePeriod(input: PeriodEditInput) {
        val startMinutes = input.startHour * 60 + input.startMinute
        val endMinutes = input.endHour * 60 + input.endMinute
        if (startMinutes >= endMinutes) return
        val updated = uiState.value.periods.map { existing ->
            if (existing.sequence == input.sequence) {
                existing.copy(
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    label = input.label
                )
            } else existing
        }.sortedBy { it.sequence }
        viewModelScope.launch {
            periodRepository.replaceAll(updated)
        }
    }

    fun removePeriod(sequence: Int) {
        val remaining = uiState.value.periods.filterNot { it.sequence == sequence }
        if (remaining.isEmpty()) return
        val normalized = remaining.sortedBy { it.sequence }.mapIndexed { index, period ->
            period.copy(sequence = index + 1)
        }
        viewModelScope.launch { periodRepository.replaceAll(normalized) }
    }

    fun export(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { dataTransferRepository.exportTo(uri) }
                .onSuccess { onComplete(true) }
                .onFailure { onComplete(false) }
        }
    }

    fun import(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { dataTransferRepository.importFrom(uri) }
                .onSuccess {
                    onComplete(true)
                }
                .onFailure { onComplete(false) }
        }
    }
}
