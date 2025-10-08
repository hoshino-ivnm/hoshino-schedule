package com.misaka.kiraraschedule.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.misaka.kiraraschedule.AppContainer
import com.misaka.kiraraschedule.data.work.SchedulePlanner
import com.misaka.kiraraschedule.R
import com.misaka.kiraraschedule.util.minutesToTimeText
import com.misaka.kiraraschedule.util.termStartDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ScheduleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val noUpcomingText = context.getString(R.string.widget_no_upcoming)
        val unavailableText = context.getString(R.string.widget_unavailable)
        val widgetState = runCatching {
            withContext(Dispatchers.IO) {
                val container = AppContainer(context)
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val courses = container.courseRepository.observeAllCourses().first()
                val periods = container.periodRepository.observePeriods().first()
                val preferences = container.settingsRepository.preferences.first()
                val planner = SchedulePlanner()
                val now = ZonedDateTime.now(zone)

                val entries = planner.buildUpcomingClasses(
                    courses = courses,
                    periods = periods,
                    termStartDate = preferences.termStartDate(),
                    totalWeeks = preferences.totalWeeks,
                    zoneId = zone,
                    startDate = today,
                    daysAhead = 0
                ).asSequence()
                    .filter { it.startDateTime.toLocalDate() == today }
                    .filter { !it.startDateTime.isBefore(now) }
                    .sortedBy { it.startDateTime }
                    .take(2)
                    .map { scheduled ->
                        WidgetEntry(
                            label = scheduled.startDateTime.format(AM_PM_FORMAT),
                            name = scheduled.course.name,
                            subtitle = buildString {
                                append(
                                    minutesToTimeText(
                                        scheduled.startDateTime.hour * 60 + scheduled.startDateTime.minute
                                    )
                                )
                                append("-")
                                append(
                                    minutesToTimeText(
                                        scheduled.endDateTime.hour * 60 + scheduled.endDateTime.minute
                                    )
                                )
                                scheduled.course.teacher?.takeIf { it.isNotBlank() }?.let {
                                    append(" ")
                                    append(BULLET_SEPARATOR)
                                    append(" ")
                                    append(it)
                                }
                                scheduled.course.location?.takeIf { it.isNotBlank() }?.let {
                                    append(" ")
                                    append(BULLET_SEPARATOR)
                                    append(" ")
                                    append(it)
                                }
                            }
                        )
                    }
                    .toList()

                WidgetState(
                    title = preferences.timetableName,
                    date = today.format(DATE_FORMAT),
                    entries = entries
                )
            }
        }.getOrNull()

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(MaterialThemeColors.widgetBackground))
                    .padding(12.dp)
            ) {
                widgetState?.let { state ->
                    HeaderRow(title = state.title, date = state.date)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    if (state.entries.isEmpty()) {
                        Text(
                            text = noUpcomingText,
                            style = TextStyle(
                                color = ColorProvider(MaterialThemeColors.onWidget),
                                fontStyle = FontStyle.Italic
                            )
                        )
                    } else {
                        state.entries.forEach { entry ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Vertical.CenterVertically
                            ) {
                                Text(
                                    text = entry.label,
                                    style = TextStyle(
                                        color = ColorProvider(MaterialThemeColors.primary),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.padding(end = 6.dp)
                                )
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = entry.name,
                                        style = TextStyle(
                                            color = ColorProvider(MaterialThemeColors.onWidget),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    if (entry.subtitle.isNotBlank()) {
                                        Text(
                                            text = entry.subtitle,
                                            style = TextStyle(
                                                color = ColorProvider(MaterialThemeColors.onWidgetSecondary)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    Text(
                        text = unavailableText,
                        style = TextStyle(
                            color = ColorProvider(MaterialThemeColors.onWidget),
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
            }
        }
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")
        private val AM_PM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a")
        private const val BULLET_SEPARATOR: Char = '\u00B7'
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

private object MaterialThemeColors {
    val widgetBackground = 0xFF1C1B1F.toInt()
    val onWidget = 0xFFF5F5F5.toInt()
    val onWidgetSecondary = 0xFFB0B0B0.toInt()
    val primary = 0xFF80CBC4.toInt()
}

private data class WidgetState(
    val title: String,
    val date: String,
    val entries: List<WidgetEntry>
)

private data class WidgetEntry(
    val label: String,
    val name: String,
    val subtitle: String
)

@SuppressLint("RestrictedApi")
@Composable
private fun HeaderRow(title: String, date: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = ColorProvider(MaterialThemeColors.onWidget),
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = date,
            style = TextStyle(color = ColorProvider(MaterialThemeColors.onWidgetSecondary))
        )
    }
}
