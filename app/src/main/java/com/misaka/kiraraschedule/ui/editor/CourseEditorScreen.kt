package com.misaka.kiraraschedule.ui.editor

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
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
        onRemoveSlot = viewModel::removeTimeSlot,
        onSave = { viewModel.save(onBack) },
        onBack = onBack
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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
    onRemoveSlot: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val periodCount = uiState.periods.maxOfOrNull { it.sequence } ?: 0
    val colorOptions = listOf(null, "#FFB300", "#FF7043", "#8BC34A", "#29B6F6", "#9C27B0")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.courseId == null) "New course" else "Edit course") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = uiState.canSave && !uiState.isSaving) {
                        Text("Save")
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
                label = { Text("Course name") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.teacher,
                onValueChange = onTeacherChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teacher") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.location,
                onValueChange = onLocationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 3
            )

            Text(text = "Card color", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colorOptions.forEach { colorHex ->
                    val isSelected = uiState.colorHex == colorHex
                    val boxColor = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
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
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Text(text = "Time slots", style = MaterialTheme.typography.titleMedium)
            if (periodCount == 0) {
                Text("Set up periods in settings before adding time slots.")
            } else {
                uiState.timeSlots.forEach { slot ->
                    TimeSlotEditor(
                        slot = slot,
                        maxSequence = periodCount,
                        onDayChange = { onSlotChange(slot.localId, it, null, null) },
                        onStartChange = { onSlotChange(slot.localId, null, it, null) },
                        onEndChange = { onSlotChange(slot.localId, null, null, it) },
                        onRemove = { onRemoveSlot(slot.localId) }
                    )
                }
                Button(onClick = onAddSlot) {
                    Text("Add time slot")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSlotEditor(
    slot: TimeSlotInput,
    maxSequence: Int,
    onDayChange: (Int) -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
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
                label = "Day",
                options = DayOfWeek.values().map { it.value to it.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()) },
                selected = slot.dayOfWeek,
                onSelected = onDayChange
            )
            DropdownSelector(
                label = "Start",
                options = (1..maxSequence).map { it to "Period $it" },
                selected = slot.startPeriod,
                onSelected = onStartChange
            )
            DropdownSelector(
                label = "End",
                options = (slot.startPeriod..maxSequence).map { it to "Period $it" },
                selected = slot.endPeriod,
                onSelected = onEndChange
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove time slot")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                .width(120.dp),
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
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


