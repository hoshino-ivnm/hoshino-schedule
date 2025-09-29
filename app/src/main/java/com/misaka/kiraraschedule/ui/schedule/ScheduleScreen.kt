package com.misaka.kiraraschedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.settings.BackgroundMode
import com.misaka.kiraraschedule.data.settings.CourseDisplayField
import com.misaka.kiraraschedule.ui.components.EmptyState
import com.misaka.kiraraschedule.util.minutesToTimeText
import com.misaka.kiraraschedule.util.parseHexColorOrNull
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleRoute(
    viewModel: ScheduleViewModel,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleScreen(
        uiState = uiState,
        onAddCourse = onAddCourse,
        onCourseClicked = { course -> if (course.id > 0) onEditCourse(course.id) },
        onDeleteCourse = { course -> viewModel.deleteCourse(course) },
        onOpenSettings = onOpenSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onAddCourse: () -> Unit,
    onCourseClicked: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onOpenSettings: () -> Unit
) {
    val preferences = uiState.preferences
    var pendingDelete by remember { mutableStateOf<Course?>(null) }
    val backgroundColor = parseHexColorOrNull(preferences?.backgroundValue ?: "")

    Box(modifier = Modifier.fillMaxSize()) {
        when (preferences?.backgroundMode) {
            BackgroundMode.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(preferences.backgroundValue)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor ?: MaterialTheme.colorScheme.background)
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(text = preferences?.timetableName ?: "Schedule") },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Open settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddCourse) {
                    Icon(Icons.Default.Add, contentDescription = "Add course")
                }
            }
        ) { padding ->
            if (uiState.days.all { it.items.isEmpty() }) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    text = "Tap the + button to add a course"
                )
            } else {
                DayScheduleView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    days = uiState.days,
                    visibleFields = preferences?.visibleFields ?: CourseDisplayField.entries.toSet(),
                    onCourseClicked = onCourseClicked,
                    onDeleteCourse = { pendingDelete = it }
                )
            }
        }
    }

    pendingDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCourse(course)
                    pendingDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Remove course") },
            text = { Text("Remove ${course.name} from the timetable?") }
        )
    }
}

@Composable
private fun DayScheduleView(
    modifier: Modifier,
    days: List<DaySchedule>,
    visibleFields: Set<CourseDisplayField>,
    onCourseClicked: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(days) { day ->
            DayColumn(
                day = day,
                visibleFields = visibleFields,
                onCourseClicked = onCourseClicked,
                onDeleteCourse = onDeleteCourse
            )
        }
    }
}

@Composable
private fun DayColumn(
    day: DaySchedule,
    visibleFields: Set<CourseDisplayField>,
    onCourseClicked: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    Column(modifier = Modifier.width(200.dp)) {
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (day.items.isEmpty()) {
            Text(
                text = "No classes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                day.items.forEach { item ->
                    CourseCard(
                        item = item,
                        visibleFields = visibleFields,
                        onClick = { onCourseClicked(item.course) },
                        onDelete = { onDeleteCourse(item.course) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    item: DayScheduleItem,
    visibleFields: Set<CourseDisplayField>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = item.course.colorHex?.let { parseHexColorOrNull(it) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (backgroundColor != null) {
            CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.85f))
        } else {
            CardDefaults.cardColors()
        },
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.course.name,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete course")
                }
            }
            Text(
                text = "${minutesToTimeText(item.startMinutes)} - ${minutesToTimeText(item.endMinutes)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val lines = buildList {
                    if (CourseDisplayField.TEACHER in visibleFields) {
                        item.course.teacher?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    if (CourseDisplayField.LOCATION in visibleFields) {
                        item.course.location?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    if (CourseDisplayField.NOTES in visibleFields) {
                        item.course.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
                lines.forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
