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
import com.misaka.kiraraschedule.util.minutesToTimeText
import com.misaka.kiraraschedule.util.termStartDate
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScheduleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainer(context)
        val courses = container.courseRepository.observeAllCourses().first()
        val periods = container.periodRepository.observePeriods().first()
        val preferences = container.settingsRepository.preferences.first()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val planner = SchedulePlanner()
        val upcoming = planner.buildUpcomingClasses(
            courses = courses,
            periods = periods,
            termStartDate = preferences.termStartDate(),
            totalWeeks = preferences.totalWeeks,
            zoneId = zone,
            startDate = today,
            daysAhead = 0
        )
        val now = java.time.ZonedDateTime.now(zone)
        val upcomingToday = upcoming
            .filter { it.startDateTime.toLocalDate() == today }
            .filter { !it.startDateTime.isBefore(now) }
            .sortedBy { it.startDateTime }
            .take(2)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(MaterialThemeColors.widgetBackground))
                    .padding(12.dp)
            ) {
                HeaderRow(title = preferences.timetableName, date = today.format(DATE_FORMAT))
                Spacer(modifier = GlanceModifier.height(8.dp))
                if (upcomingToday.isEmpty()) {
                    Text(
                        text = "No upcoming classes",
                        style = TextStyle(
                            color = ColorProvider(MaterialThemeColors.onWidget),
                            fontStyle = FontStyle.Italic
                        )
                    )
                } else {
                    upcomingToday.forEach { scheduled ->
                        val amPm = if (scheduled.startDateTime.hour < 12) "AM" else "PM"
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = amPm,
                                style = TextStyle(
                                    color = ColorProvider(MaterialThemeColors.primary),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.padding(end = 6.dp)
                            )
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = scheduled.course.name,
                                    style = TextStyle(
                                        color = ColorProvider(MaterialThemeColors.onWidget),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                val subtitle = buildString {
                                    append(minutesToTimeText(scheduled.startDateTime.hour * 60 + scheduled.startDateTime.minute))
                                    append("-")
                                    append(minutesToTimeText(scheduled.endDateTime.hour * 60 + scheduled.endDateTime.minute))
                                    scheduled.course.teacher?.takeIf { it.isNotBlank() }?.let {
                                        append(" · ")
                                        append(it)
                                    }
                                    scheduled.course.location?.takeIf { it.isNotBlank() }?.let {
                                        append(" · ")
                                        append(it)
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = TextStyle(color = ColorProvider(MaterialThemeColors.onWidgetSecondary))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")
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
