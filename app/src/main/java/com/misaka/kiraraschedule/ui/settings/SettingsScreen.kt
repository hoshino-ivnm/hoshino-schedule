package com.misaka.kiraraschedule.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.settings.BackgroundMode
import com.misaka.kiraraschedule.data.settings.CourseDisplayField
import com.misaka.kiraraschedule.data.settings.UserPreferences
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTimetableNameChange: (String) -> Unit,
    onBackgroundColorSelected: (String) -> Unit,
    onBackgroundImageSelect: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onVisibleFieldsChange: (Set<CourseDisplayField>) -> Unit,
    onReminderLeadChange: (Int) -> Unit,
    onDndConfigChange: (Boolean, Int, Int, Int) -> Unit,
    onRequestDndAccess: () -> Unit,
    onAddPeriod: () -> Unit,
    onUpdatePeriod: (PeriodEditInput) -> Unit,
    onRemovePeriod: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val scrollState = rememberScrollState()
    val prefs = state.preferences
    val colorOptions = remember {
        listOf(UserPreferences().backgroundValue, "#FFB300", "#FF7043", "#8BC34A", "#29B6F6", "#9C27B0")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(colors = sectionCardColors(), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Timetable", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = prefs.timetableName,
                        onValueChange = onTimetableNameChange,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Background", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        colorOptions.forEach { hex ->
                            val selected = prefs.backgroundMode == BackgroundMode.COLOR && prefs.backgroundValue == hex
                            ColorOption(colorHex = hex, selected = selected) {
                                onBackgroundColorSelected(hex)
                            }
                        }
                        TextButton(onClick = onBackgroundImageSelect) { Text("Pick image") }
                        if (prefs.backgroundMode == BackgroundMode.IMAGE) {
                            TextButton(onClick = onClearBackgroundImage) { Text("Use color") }
                        }
                    }
                    if (prefs.backgroundMode == BackgroundMode.IMAGE) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(prefs.backgroundValue)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }

            Card(colors = sectionCardColors(), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Course card fields", style = MaterialTheme.typography.titleMedium)
                    state.availableFields.forEach { field ->
                        val checked = prefs.visibleFields.contains(field)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = {
                                val newSet = prefs.visibleFields.toMutableSet()
                                if (it) newSet.add(field) else newSet.remove(field)
                                if (newSet.isEmpty()) newSet.add(CourseDisplayField.NAME)
                                onVisibleFieldsChange(newSet)
                            })
                            Text(fieldLabel(field))
                        }
                    }
                }
            }

            Card(colors = sectionCardColors(), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reminders", style = MaterialTheme.typography.titleMedium)
                    Text("Remind before class: ${prefs.reminderLeadMinutes} minutes")
                    Slider(
                        value = prefs.reminderLeadMinutes.toFloat(),
                        onValueChange = { onReminderLeadChange(it.toInt()) },
                        valueRange = 0f..120f
                    )
                }
            }

            Card(colors = sectionCardColors(), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Do Not Disturb", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Switch(checked = prefs.dndEnabled, onCheckedChange = { enabled ->
                            onDndConfigChange(enabled, prefs.dndLeadMinutes, prefs.dndReleaseMinutes, prefs.dndSkipBreakThresholdMinutes)
                            if (enabled) onRequestDndAccess()
                        })
                        Text(if (prefs.dndEnabled) "Enabled" else "Disabled")
                        if (prefs.dndEnabled.not()) {
                            TextButton(onClick = onRequestDndAccess) { Text("Grant access") }
                        }
                    }
                    if (prefs.dndEnabled) {
                        SliderWithValue(
                            label = "Enable before class",
                            value = prefs.dndLeadMinutes,
                            range = 0..30
                        ) { onDndConfigChange(true, it, prefs.dndReleaseMinutes, prefs.dndSkipBreakThresholdMinutes) }
                        SliderWithValue(
                            label = "Disable after class",
                            value = prefs.dndReleaseMinutes,
                            range = 0..30
                        ) { onDndConfigChange(true, prefs.dndLeadMinutes, it, prefs.dndSkipBreakThresholdMinutes) }
                        SliderWithValue(
                            label = "Keep DND if break shorter than",
                            value = prefs.dndSkipBreakThresholdMinutes,
                            range = 0..60
                        ) { onDndConfigChange(true, prefs.dndLeadMinutes, prefs.dndReleaseMinutes, it) }
                    }
                }
            }

            Card(colors = sectionCardColors(), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Periods", style = MaterialTheme.typography.titleMedium)
                    state.periods.forEach { period ->
                        PeriodEditorRow(period = period, onUpdate = onUpdatePeriod, onRemove = onRemovePeriod)
                    }
                    TextButton(onClick = onAddPeriod) { Text("Add period") }
                }
            }

            Card(colors = sectionCardColors(), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Data", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = onExport) { Text("Export") }
                        Button(onClick = onImport) { Text("Import") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun sectionCardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
)

@Composable
private fun SliderWithValue(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: $value minutes")
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun ColorOption(colorHex: String, selected: Boolean, onClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(colorHex))
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun fieldLabel(field: CourseDisplayField): String = when (field) {
    CourseDisplayField.NAME -> "Course name"
    CourseDisplayField.TEACHER -> "Teacher"
    CourseDisplayField.LOCATION -> "Location"
    CourseDisplayField.NOTES -> "Notes"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodEditorRow(
    period: PeriodDefinition,
    onUpdate: (PeriodEditInput) -> Unit,
    onRemove: (Int) -> Unit
) {
    var startText by remember(period.id) { mutableStateOf(minutesToText(period.startMinutes)) }
    var endText by remember(period.id) { mutableStateOf(minutesToText(period.endMinutes)) }
    var labelText by remember(period.id) { mutableStateOf(period.label.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Period ${period.sequence}", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it.filterTimeInput() },
                label = { Text("Start (HH:MM)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it.filterTimeInput() },
                label = { Text("End (HH:MM)") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = labelText,
            onValueChange = { labelText = it },
            label = { Text("Label") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val startMinutes = parseTimeToMinutes(startText)
                val endMinutes = parseTimeToMinutes(endText)
                if (startMinutes != null && endMinutes != null && startMinutes < endMinutes) {
                    onUpdate(
                        PeriodEditInput(
                            id = period.id,
                            sequence = period.sequence,
                            startHour = startMinutes / 60,
                            startMinute = startMinutes % 60,
                            endHour = endMinutes / 60,
                            endMinute = endMinutes % 60,
                            label = labelText.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }) { Text("Apply") }
            TextButton(onClick = { onRemove(period.sequence) }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove")
            }
        }
    }
}

private fun minutesToText(minutes: Int): String = String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)

private fun String.filterTimeInput(): String {
    val filtered = filter { it.isDigit() || it == ':' }
    return if (filtered.length <= 5) filtered else filtered.take(5)
}

private fun parseTimeToMinutes(input: String): Int? {
    val parts = input.split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}
