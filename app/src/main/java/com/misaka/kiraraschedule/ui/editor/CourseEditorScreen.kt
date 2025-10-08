package com.misaka.kiraraschedule.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misaka.kiraraschedule.R
import com.misaka.kiraraschedule.ui.colorpicker.ColorPickerDialog
import com.misaka.kiraraschedule.util.parseHexColorOrNull
import com.misaka.kiraraschedule.util.toHexString
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as JavaTextStyle


@Composable
fun CourseEditorRoute(
    viewModel: CourseEditorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CourseEditorScreen(
        uiState = state,
        onNameChange = viewModel::updateName,
        onTeacherChange = viewModel::updateTeacher,
        onLocationChange = viewModel::updateLocation,
        onNotesChange = viewModel::updateNotes,
        onColorChange = viewModel::updateColor,
        onAddSlot = viewModel::addTimeSlot,
        onSlotChange = viewModel::updateTimeSlot,
        onSlotWeeksChange = viewModel::updateTimeSlotWeeks,
        onRemoveSlot = viewModel::removeTimeSlot,
        onSave = { viewModel.save(onBack) },
        onDeleteCourse = { viewModel.deleteCourse(onBack) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditorScreen(
    uiState: CourseEditorUiState,
    onNameChange: (String) -> Unit,
    onTeacherChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onAddSlot: () -> Unit,
    onSlotChange: (String, Int?, Int?, Int?) -> Unit,
    onSlotWeeksChange: (String, Set<Int>) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteCourse: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val periodCount = uiState.periods.maxOfOrNull { it.sequence } ?: 0
    val totalWeeks = (uiState.preferences?.totalWeeks ?: 20).coerceAtLeast(1)
    val colorOptions = listOf(null, "#FFB300", "#FF7043", "#8BC34A", "#29B6F6", "#9C27B0")
    val titleRes =
        if (uiState.courseId == null) R.string.course_editor_new_title else R.string.course_editor_edit_title
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.course_editor_back)) }
                },
                actions = {
                    if (uiState.courseId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.course_editor_delete)
                            )
                        }
                    }
                    TextButton(onClick = onSave, enabled = uiState.canSave && !uiState.isSaving) {
                        Text(stringResource(R.string.course_editor_save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.course_editor_name)) },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.teacher,
                onValueChange = onTeacherChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.course_editor_teacher)) },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.location,
                onValueChange = onLocationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.course_editor_location)) },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.course_editor_notes)) },
                minLines = 3
            )

            Text(
                text = stringResource(R.string.course_editor_card_color),
                style = MaterialTheme.typography.titleMedium
            )
            val palette = remember(uiState.colorHex) {
                if (uiState.colorHex != null && !colorOptions.contains(uiState.colorHex)) {
                    colorOptions + uiState.colorHex
                } else {
                    colorOptions
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                palette.forEach { colorHex ->
                    val isSelected = uiState.colorHex == colorHex
                    val boxColor = colorHex?.let { parseHexColorOrNull(it) }
                        ?: MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(boxColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onColorChange(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorHex == null) {
                            Text(
                                text = stringResource(R.string.course_editor_color_auto),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            TextButton(onClick = { showColorPicker = true }) {
                Text(stringResource(R.string.course_editor_custom_color))
            }
            if (showColorPicker) {
                val initialColor =
                    parseHexColorOrNull(uiState.colorHex) ?: MaterialTheme.colorScheme.primary
                ColorPickerDialog(
                    initialColor = initialColor,
                    onDismiss = { showColorPicker = false },
                    onColorSelected = { color ->
                        onColorChange(color.toHexString())
                    }
                )
            }

            Text(
                text = stringResource(R.string.course_editor_time_slots),
                style = MaterialTheme.typography.titleMedium
            )
            if (periodCount == 0) {
                Text(stringResource(R.string.course_editor_time_slot_help))
            } else {
                uiState.timeSlots.forEach { slot ->
                    TimeSlotEditor(
                        slot = slot,
                        maxSequence = periodCount,
                        totalWeeks = totalWeeks,
                        onDayChange = { onSlotChange(slot.localId, it, null, null) },
                        onStartChange = { onSlotChange(slot.localId, null, it, null) },
                        onEndChange = { onSlotChange(slot.localId, null, null, it) },
                        onWeeksChange = { weeks -> onSlotWeeksChange(slot.localId, weeks) },
                        onRemove = { onRemoveSlot(slot.localId) }
                    )
                }
                Button(onClick = onAddSlot) { Text(stringResource(R.string.course_editor_add_time_slot)) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.course_editor_delete_title)) },
            text = { Text(stringResource(R.string.course_editor_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteCourse()
                }) {
                    Text(stringResource(R.string.course_editor_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.course_editor_delete_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TimeSlotEditor(
    slot: TimeSlotInput,
    maxSequence: Int,
    totalWeeks: Int,
    onDayChange: (Int) -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onWeeksChange: (Set<Int>) -> Unit,
    onRemove: () -> Unit
) {
    val locale = Locale.getDefault()
    var showWeeksDialog by remember { mutableStateOf(false) }
    val weekSummary = if (slot.weeks.isEmpty()) {
        stringResource(R.string.course_editor_weeks_all)
    } else {
        stringResource(
            R.string.course_editor_weeks_selected,
            slot.weeks.toList().sorted().joinToString(", ")
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DropdownSelector(
                label = stringResource(R.string.course_editor_day_label),
                options = DayOfWeek.entries.map {
                    it.value to it.getDisplayName(
                        JavaTextStyle.SHORT,
                        locale
                    )
                },
                selected = slot.dayOfWeek,
                onSelected = onDayChange
            )
            DropdownSelector(
                label = stringResource(R.string.course_editor_start_period),
                options = (1..maxSequence).map {
                    it to stringResource(
                        R.string.settings_period_title,
                        it
                    )
                },
                selected = slot.startPeriod,
                onSelected = onStartChange
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DropdownSelector(
                label = stringResource(R.string.course_editor_end_period),
                options = (slot.startPeriod..maxSequence).map {
                    it to stringResource(
                        R.string.settings_period_title,
                        it
                    )
                },
                selected = slot.endPeriod,
                onSelected = onEndChange
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.course_editor_remove_time_slot)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weekSummary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = { showWeeksDialog = true }) {
                Text(stringResource(R.string.course_editor_edit_weeks))
            }
        }
    }

    if (showWeeksDialog) {
        WeeksPickerDialog(
            maxWeeks = totalWeeks,
            selectedWeeks = slot.weeks,
            onDismiss = { showWeeksDialog = false },
            onConfirm = {
                onWeeksChange(it)
                showWeeksDialog = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: label

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .width(140.dp),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WeeksPickerDialog(
    maxWeeks: Int,
    selectedWeeks: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    val actualMax = maxWeeks.coerceAtLeast(1)
    var tempSelection by remember(
        selectedWeeks,
        actualMax
    ) { mutableStateOf(selectedWeeks.toSet()) }
    var sliderRange by remember(selectedWeeks, actualMax) {
        val start = (selectedWeeks.minOrNull() ?: 1).coerceAtLeast(1)
        val end = (selectedWeeks.maxOrNull() ?: actualMax).coerceAtMost(actualMax)
        mutableStateOf(start.toFloat()..end.toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelection) }) {
                Text(text = stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.course_editor_weeks_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val startWeek = sliderRange.start.roundToInt().coerceIn(1, actualMax)
                val endWeek = sliderRange.endInclusive.roundToInt().coerceIn(startWeek, actualMax)
                Text(
                    text = stringResource(
                        R.string.course_editor_weeks_range_label,
                        startWeek,
                        endWeek
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
                RangeSlider(
                    value = sliderRange,
                    onValueChange = { range ->
                        val coercedStart = range.start.coerceIn(1f, actualMax.toFloat())
                        val coercedEnd =
                            range.endInclusive.coerceIn(coercedStart, actualMax.toFloat())
                        sliderRange = coercedStart..coercedEnd
                    },
                    valueRange = 1f..actualMax.toFloat(),
                    steps = (actualMax - 2).coerceAtLeast(0)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        val updated = tempSelection.toMutableSet()
                        updated.addAll(startWeek..endWeek)
                        tempSelection = updated.toSet()
                    }) {
                        Text(stringResource(R.string.course_editor_weeks_apply_range))
                    }
                    TextButton(onClick = { tempSelection = (1..actualMax).toSet() }) {
                        Text(stringResource(R.string.course_editor_select_all))
                    }
                    TextButton(onClick = { tempSelection = emptySet() }) {
                        Text(stringResource(R.string.course_editor_clear_weeks))
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..actualMax).forEach { week ->
                        val selected = tempSelection.contains(week)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                tempSelection = tempSelection.toMutableSet().also { set ->
                                    if (selected) set.remove(week) else set.add(week)
                                }.toSet()
                            },
                            label = { Text(week.toString()) }
                        )
                    }
                }
            }
        }
    )
}





