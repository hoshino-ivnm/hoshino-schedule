package com.misaka.kiraraschedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.misaka.kiraraschedule.R
import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.settings.BackgroundMode
import com.misaka.kiraraschedule.data.settings.CourseDisplayField
import com.misaka.kiraraschedule.ui.components.EmptyState
import com.misaka.kiraraschedule.util.minutesToTimeText
import com.misaka.kiraraschedule.util.parseHexColorOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

private val TimeColumnWidth = 72.dp
private val DayColumnWidth = 120.dp
private val DayHeaderHeight = 72.dp
private val CellHeight = 64.dp
private val CourseCornerRadius = 16.dp
private val CourseVerticalPadding = 6.dp

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
    if (preferences == null) {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(R.string.schedule_loading_state)
        )
        return
    }

    var pendingDelete by remember { mutableStateOf<Course?>(null) }
    val backgroundColor = parseHexColorOrNull(preferences.backgroundValue)
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val today = LocalDate.now()
    val weekFields = WeekFields.of(locale)
    val firstDayOfWeek = weekFields.firstDayOfWeek
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val orderedDays = (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
    val daySchedules = uiState.days.associateBy { it.dayOfWeek }
    val displayedDays = orderedDays.filter {
        when (it) {
            DayOfWeek.SATURDAY -> preferences.showSaturday
            DayOfWeek.SUNDAY -> preferences.showSunday
            else -> true
        }
    }.map { day ->
        val offset = ((day.value - firstDayOfWeek.value + 7) % 7).toLong()
        DayColumnDescriptor(
            day = day,
            date = startOfWeek.plusDays(offset)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (preferences.backgroundMode) {
            BackgroundMode.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
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
            BackgroundMode.COLOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor ?: MaterialTheme.colorScheme.background)
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(onClick = onAddCourse) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_add_course))
                }
            }
        ) { padding ->
            if (uiState.periods.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    text = stringResource(R.string.schedule_empty_periods)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScheduleHeader(
                        today = today,
                        locale = locale,
                        weekNumber = today.get(weekFields.weekOfWeekBasedYear()),
                        onOpenSettings = onOpenSettings
                    )
                    TimetableGrid(
                        periods = uiState.periods,
                        dayDescriptors = displayedDays,
                        daySchedules = daySchedules,
                        visibleFields = preferences.visibleFields,
                        today = today,
                        locale = locale,
                        onCourseClicked = onCourseClicked,
                        onDeleteCourse = { pendingDelete = it }
                    )
                }
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
                    Text(stringResource(R.string.schedule_remove_course_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.schedule_remove_course_cancel))
                }
            },
            title = { Text(stringResource(R.string.schedule_remove_course_title)) },
            text = { Text(stringResource(R.string.schedule_remove_course_message, course.name)) }
        )
    }
}

@Composable
private fun ScheduleHeader(
    today: LocalDate,
    locale: Locale,
    weekNumber: Int,
    onOpenSettings: () -> Unit
) {
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("yyyy/MM/dd", locale) }
    val subtitleFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM d", locale) }
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = today.format(dateFormatter),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.schedule_week_label, weekNumber, dayName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = today.format(subtitleFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.schedule_settings_content_description))
        }
    }
}

@Composable
private fun TimetableGrid(
    periods: List<PeriodDefinition>,
    dayDescriptors: List<DayColumnDescriptor>,
    daySchedules: Map<DayOfWeek, DaySchedule>,
    visibleFields: Set<CourseDisplayField>,
    today: LocalDate,
    locale: Locale,
    onCourseClicked: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    if (dayDescriptors.isEmpty()) {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(R.string.schedule_no_courses)
        )
        return
    }

    val totalHeight = CellHeight * periods.size
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        TimeColumn(periods = periods)
        dayDescriptors.forEachIndexed { index, descriptor ->
            val schedule = daySchedules[descriptor.day] ?: DaySchedule(descriptor.day, emptyList())
            DayColumn(
                descriptor = descriptor,
                periods = periods,
                schedule = schedule,
                visibleFields = visibleFields,
                columnHeight = totalHeight,
                isToday = descriptor.date == today,
                locale = locale,
                onCourseClicked = onCourseClicked,
                onDeleteCourse = onDeleteCourse
            )
            if (index != dayDescriptors.lastIndex) {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}


@Composable
private fun TimeColumn(periods: List<PeriodDefinition>) {
    Column(
        modifier = Modifier.width(TimeColumnWidth)
    ) {
        Box(
            modifier = Modifier
                .height(DayHeaderHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.schedule_time_column_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        periods.forEach { period ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CellHeight)
                    .padding(vertical = CourseVerticalPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.schedule_period_label, period.sequence),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.schedule_period_time_range,
                            minutesToTimeText(period.startMinutes),
                            minutesToTimeText(period.endMinutes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    descriptor: DayColumnDescriptor,
    periods: List<PeriodDefinition>,
    schedule: DaySchedule,
    visibleFields: Set<CourseDisplayField>,
    columnHeight: androidx.compose.ui.unit.Dp,
    isToday: Boolean,
    locale: Locale,
    onCourseClicked: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    val headerDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("M/d", locale) }
    Column(
        modifier = Modifier.width(DayColumnWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(DayHeaderHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = descriptor.date.format(headerDateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = descriptor.day.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(columnHeight)
        ) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                periods.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CellHeight)
                            .border(
                                width = if (index == periods.lastIndex) 0.dp else 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(0.dp)
                            )
                    )
                }
            }
            schedule.items.forEach { item ->
                val startIndex = periods.indexOfFirst { it.sequence == item.time.startPeriod }
                val endIndex = periods.indexOfFirst { it.sequence == item.time.endPeriod }
                if (startIndex == -1 || endIndex == -1) return@forEach
                val duration = max(1, endIndex - startIndex + 1)
                val blockHeight = (CellHeight * duration) - (CourseVerticalPadding * 2)
                CourseBlock(
                    item = item,
                    visibleFields = visibleFields,
                    modifier = Modifier
                        .offset(y = CellHeight * startIndex + CourseVerticalPadding)
                        .padding(horizontal = 6.dp)
                        .height(blockHeight)
                        .fillMaxWidth()
                        .zIndex(1f),
                    onClick = { onCourseClicked(item.course) },
                    onDelete = { onDeleteCourse(item.course) }
                )
            }
        }
    }
}

@Composable
private fun CourseBlock(
    item: DayScheduleItem,
    visibleFields: Set<CourseDisplayField>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = item.course.colorHex?.let { parseHexColorOrNull(it) }
    val (containerColor, contentColor) = if (backgroundColor != null) {
        backgroundColor.copy(alpha = 0.9f) to Color.Black.copy(alpha = 0.87f)
    } else {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(CourseCornerRadius),
        onClick = onClick
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.schedule_delete_course_accessibility)
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.schedule_course_time_range,
                        minutesToTimeText(item.startMinutes),
                        minutesToTimeText(item.endMinutes)
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
                val detailLines = buildList {
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
                detailLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private data class DayColumnDescriptor(
    val day: DayOfWeek,
    val date: LocalDate
)



