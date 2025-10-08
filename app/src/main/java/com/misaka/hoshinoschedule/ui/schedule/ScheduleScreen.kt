package com.misaka.hoshinoschedule.ui.schedule

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.misaka.hoshinoschedule.R
import com.misaka.hoshinoschedule.data.model.Course
import com.misaka.hoshinoschedule.data.model.PeriodDefinition
import com.misaka.hoshinoschedule.data.settings.CourseDisplayField
import com.misaka.hoshinoschedule.data.settings.UserPreferences
import com.misaka.hoshinoschedule.ui.components.EmptyState
import com.misaka.hoshinoschedule.util.computeWeekNumber
import com.misaka.hoshinoschedule.util.isTimeActiveInWeek
import com.misaka.hoshinoschedule.util.minutesToTimeText
import com.misaka.hoshinoschedule.util.parseHexColorOrNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

private val TimeColumnWidth = 40.dp
private val DayHeaderHeight = 72.dp
private val MinCellHeight = 64.dp
private val CourseCornerRadius = 6.dp
private val CourseVerticalPadding = 2.dp
private const val PagerMiddlePage = Int.MAX_VALUE / 2

@Composable
fun ScheduleRoute(
    viewModel: ScheduleViewModel,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCourseList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleScreen(
        uiState = uiState,
        onAddCourse = onAddCourse,
        onCourseClicked = { course -> if (course.id > 0) onEditCourse(course.id) },
        onOpenSettings = onOpenSettings,
        onOpenCourseList = onOpenCourseList
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onAddCourse: () -> Unit,
    onCourseClicked: (Course) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCourseList: () -> Unit
) {
    val preferences = uiState.preferences ?: run {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(R.string.schedule_loading_state)
        )
        return
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val weekFields = WeekFields.of(locale)
    val today = LocalDate.now()
    val baseWeekStart = remember(today, locale) {
        today.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
    }
    val pagerState =
        rememberPagerState(initialPage = PagerMiddlePage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()
    val currentWeekOffset by remember {
        derivedStateOf { pagerState.currentPage - PagerMiddlePage }
    }
    val displayedWeekStart = remember(currentWeekOffset, baseWeekStart) {
        baseWeekStart.plusWeeks(currentWeekOffset.toLong())
    }
    val viewWeekNumber = remember(displayedWeekStart, uiState.termStartDate) {
        computeWeekNumber(uiState.termStartDate, displayedWeekStart)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScheduleBackground(preferences = preferences)
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ScheduleHeader(
                    today = today,
                    viewWeekNumber = viewWeekNumber,
                    currentWeekNumber = uiState.currentWeekNumber,
                    totalWeeks = uiState.totalWeeks,
                    onAddCourse = onAddCourse,
                    onOpenCourseList = onOpenCourseList,
                    onOpenSettings = onOpenSettings,
                    onTodayClicked = {
                        scope.launch { pagerState.animateScrollToPage(PagerMiddlePage) }
                    }
                )

                HorizontalPager(
                    state = pagerState,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        pagerSnapDistance = PagerSnapDistance.atMost(3)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val weekOffset = page - PagerMiddlePage
                    WeekPagerContent(
                        baseWeekStart = baseWeekStart,
                        weekOffset = weekOffset,
                        preferences = preferences,
                        periods = uiState.periods,
                        daySchedules = uiState.days.associateBy { it.dayOfWeek },
                        today = today,
                        locale = locale,
                        termStartDate = uiState.termStartDate,
                        totalWeeks = uiState.totalWeeks,
                        showNonCurrentWeekCourses = preferences.showNonCurrentWeekCourses,
                        onCourseClicked = onCourseClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader(
    today: LocalDate,
    viewWeekNumber: Int?,
    currentWeekNumber: Int?,
    totalWeeks: Int,
    onAddCourse: () -> Unit,
    onOpenCourseList: () -> Unit,
    onOpenSettings: () -> Unit,
    onTodayClicked: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("yyyy/MM/dd", locale) }
    val todayText = today.format(dateFormatter)
    val viewText = when {
        viewWeekNumber != null && currentWeekNumber != null && viewWeekNumber == currentWeekNumber ->
            stringResource(R.string.schedule_viewing_week_current, viewWeekNumber)

        viewWeekNumber != null && viewWeekNumber in 1..totalWeeks ->
            stringResource(R.string.schedule_viewing_week_label, viewWeekNumber)

        viewWeekNumber != null ->
            stringResource(R.string.schedule_viewing_week_beyond, viewWeekNumber)

        else -> stringResource(R.string.schedule_viewing_week_unknown)
    }
    val currentText = if (currentWeekNumber != null && currentWeekNumber != viewWeekNumber) {
        stringResource(R.string.schedule_current_week_label, currentWeekNumber)
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onTodayClicked() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = todayText,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = viewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                currentText?.let { current ->
                    Text(
                        text = current,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onAddCourse) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.schedule_add_course)
                )
            }
            IconButton(onClick = onOpenCourseList) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.schedule_view_courses)
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.schedule_settings_content_description)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun WeekPagerContent(
    baseWeekStart: LocalDate,
    weekOffset: Int,
    preferences: UserPreferences,
    periods: List<PeriodDefinition>,
    daySchedules: Map<DayOfWeek, DaySchedule>,
    today: LocalDate,
    locale: Locale,
    termStartDate: LocalDate?,
    totalWeeks: Int,
    showNonCurrentWeekCourses: Boolean,
    onCourseClicked: (Course) -> Unit
) {
    if (periods.isEmpty()) {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(R.string.schedule_empty_periods)
        )
        return
    }

    val startOfWeek = baseWeekStart.plusWeeks(weekOffset.toLong())
    val orderedDays = (0 until 7).map { startOfWeek.plusDays(it.toLong()) }
    val maxWeeks = totalWeeks.coerceAtLeast(1)

    val descriptors = orderedDays.map { date ->
        val weekNumber = computeWeekNumber(termStartDate, date)
        val withinTerm = termStartDate == null || (weekNumber != null && weekNumber in 1..maxWeeks)
        DayColumnDescriptor(
            day = date.dayOfWeek,
            date = date,
            weekNumber = weekNumber,
            isWithinTerm = withinTerm
        )
    }

    val displayedDays = descriptors.filter { descriptor ->
        val baseVisible = when (descriptor.day) {
            DayOfWeek.SATURDAY -> preferences.showSaturday
            DayOfWeek.SUNDAY -> preferences.showSunday
            else -> true
        }
        if (!baseVisible) return@filter false
        if (preferences.hideEmptyWeekends && descriptor.day in setOf(
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
        ) {
            val schedule = daySchedules[descriptor.day] ?: DaySchedule(descriptor.day, emptyList())
            val hasRenderableCourse = descriptor.isWithinTerm && schedule.items.any { item ->
                val startIndex = periods.indexOfFirst { it.sequence == item.time.startPeriod }
                val endIndex = periods.indexOfFirst { it.sequence == item.time.endPeriod }
                if (startIndex == -1 || endIndex == -1) return@any false
                val isActiveWeek = isTimeActiveInWeek(item.time.weeks, descriptor.weekNumber)
                showNonCurrentWeekCourses || isActiveWeek
            }
            if (!hasRenderableCourse) return@filter false
        }
        true
    }

    if (displayedDays.isEmpty()) {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(R.string.schedule_no_courses)
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val availableHeight = maxHeight - DayHeaderHeight
        val computedCellHeight = if (periods.isNotEmpty()) {
            availableHeight / periods.size
        } else {
            MinCellHeight
        }
        val shouldEnableVerticalScroll =
            periods.size > 10 || computedCellHeight < MinCellHeight
        val cellHeight = if (shouldEnableVerticalScroll) MinCellHeight else computedCellHeight
        val totalColumnHeight = cellHeight * periods.size
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()

        val baseModifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState)
        val rowModifier = if (shouldEnableVerticalScroll) {
            baseModifier.verticalScroll(verticalScrollState)
        } else {
            baseModifier
        }

        Row(modifier = rowModifier) {
            TimeColumn(periods = periods, cellHeight = cellHeight)
            displayedDays.forEach { descriptor ->
                val schedule =
                    daySchedules[descriptor.day] ?: DaySchedule(descriptor.day, emptyList())
                DayColumn(
                    descriptor = descriptor,
                    periods = periods,
                    schedule = schedule,
                    visibleFields = preferences.visibleFields,
                    columnHeight = totalColumnHeight,
                    cellHeight = cellHeight,
                    isToday = descriptor.date == today,
                    locale = locale,
                    showNonCurrentWeekCourses = showNonCurrentWeekCourses,
                    onCourseClicked = onCourseClicked,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TimeColumn(periods: List<PeriodDefinition>, cellHeight: Dp) {
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
                    .height(cellHeight)
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
    columnHeight: Dp,
    cellHeight: Dp,
    isToday: Boolean,
    locale: Locale,
    showNonCurrentWeekCourses: Boolean,
    onCourseClicked: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val headerDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("M/d", locale) }

    Column(modifier = modifier) {
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
                color = if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
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
            if (isToday) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                periods.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight)
                            .border(
                                width = if (index == periods.lastIndex) 0.dp else 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(0.dp)
                            )
                    )
                }
            }

            schedule.items.forEach { item ->
                if (!descriptor.isWithinTerm) return@forEach
                val isActiveWeek = isTimeActiveInWeek(item.time.weeks, descriptor.weekNumber)
                if (!showNonCurrentWeekCourses && !isActiveWeek) return@forEach
                val startIndex = periods.indexOfFirst { it.sequence == item.time.startPeriod }
                val endIndex = periods.indexOfFirst { it.sequence == item.time.endPeriod }
                if (startIndex == -1 || endIndex == -1) return@forEach

                val duration = max(1, endIndex - startIndex + 1)
                val blockHeight = (cellHeight * duration) - (CourseVerticalPadding * 2)
                CourseBlock(
                    item = item,
                    visibleFields = visibleFields,
                    modifier = Modifier
                        .offset(y = cellHeight * startIndex + CourseVerticalPadding)
                        .padding(horizontal = 6.dp)
                        .height(blockHeight)
                        .fillMaxWidth()
                        .zIndex(1f),
                    onClick = { onCourseClicked(item.course) },
                    isInactive = !isActiveWeek
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
    isInactive: Boolean = false
) {
    val backgroundColor = item.course.colorHex?.let { parseHexColorOrNull(it) }
    val (activeContainer, activeContent) = if (backgroundColor != null) {
        backgroundColor.copy(alpha = 0.92f) to Color.Black.copy(alpha = 0.87f)
    } else {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val containerColor = if (isInactive) activeContainer.copy(alpha = 0.6f) else activeContainer
    val contentColor = if (isInactive) activeContent.copy(alpha = 0.6f) else activeContent
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(CourseCornerRadius),
        onClick = onClick
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isInactive) {
                    Text(
                        text = stringResource(R.string.schedule_course_inactive_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
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
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ScheduleBackground(preferences: UserPreferences) {
    val fallbackColor = MaterialTheme.colorScheme.background
    when (preferences.backgroundMode) {
        com.misaka.hoshinoschedule.data.settings.BackgroundMode.COLOR -> {
            val color = remember(preferences.backgroundValue) {
                parseHexColorOrNull(preferences.backgroundValue) ?: fallbackColor
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
            )
        }

        com.misaka.hoshinoschedule.data.settings.BackgroundMode.IMAGE -> {
            val context = LocalContext.current
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = remember(preferences.backgroundValue) {
                    ImageRequest.Builder(context)
                        .data(preferences.backgroundValue)
                        .crossfade(true)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
            )
        }
    }
}

private data class DayColumnDescriptor(
    val day: DayOfWeek,
    val date: LocalDate,
    val weekNumber: Int?,
    val isWithinTerm: Boolean
)
