package com.misaka.hoshinoschedule.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import com.misaka.hoshinoschedule.AppContainer
import com.misaka.hoshinoschedule.R
import com.misaka.hoshinoschedule.data.work.SchedulePlanner
import com.misaka.hoshinoschedule.util.minutesToTimeText
import com.misaka.hoshinoschedule.util.termStartDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val noUpcomingText = safeGetString(context, R.string.widget_no_upcoming) ?: "No upcoming classes"
        val unavailableText = safeGetString(context, R.string.widget_unavailable) ?: "Widget unavailable"

        val widgetState: WidgetState? = runCatching {
            withContext(Dispatchers.IO) {
                val container = AppContainer(context)

                val zone = runCatching { ZoneId.systemDefault() }.getOrDefault(ZoneId.of("UTC"))
                val today = runCatching { LocalDate.now(zone) }.getOrDefault(LocalDate.now())
                val now = runCatching { ZonedDateTime.now(zone) }.getOrDefault(ZonedDateTime.now())

                val courses = runCatching { container.courseRepository.observeAllCourses().first() }
                    .getOrDefault(emptyList())

                val periods = runCatching { container.periodRepository.observePeriods().first() }
                    .getOrDefault(emptyList())

                val preferences = runCatching { container.settingsRepository.preferences.first() }
                    .getOrNull()

                val termStart = runCatching { preferences?.termStartDate() }.getOrNull() ?: today
                val totalWeeks = runCatching { preferences?.totalWeeks ?: 20 }.getOrDefault(20)
                    .coerceIn(1, 52)
                val timetableName = runCatching { preferences?.timetableName?.takeIf { it.isNotBlank() } }
                    .getOrNull() ?: "Timetable"

                val planner = SchedulePlanner()

                val raw = planner.buildUpcomingClasses(
                    courses = courses,
                    periods = periods,
                    termStartDate = termStart,
                    totalWeeks = totalWeeks,
                    zoneId = zone,
                    startDate = today,
                    daysAhead = 0
                )

                val entries = raw.asSequence()
                    .filter { it.startDateTime.toLocalDate() == today }
                    .filter { !it.startDateTime.isBefore(now) }
                    .sortedBy { it.startDateTime }
                    .take(2)
                    .map { scheduled ->
                        val label = runCatching { scheduled.startDateTime.format(AM_PM_FORMAT) }
                            .getOrDefault("")

                        val timeSpan = runCatching {
                            val startMin = scheduled.startDateTime.hour * 60 + scheduled.startDateTime.minute
                            val endMin = scheduled.endDateTime.hour * 60 + scheduled.endDateTime.minute
                            "${minutesToTimeText(startMin)}-${minutesToTimeText(endMin)}"
                        }.getOrDefault("")

                        val subtitle = buildString {
                            append(timeSpan)
                            scheduled.course.teacher?.takeIf { it.isNotBlank() }?.let {
                                append(" ").append(BULLET_SEPARATOR).append(" ").append(it)
                            }
                            scheduled.course.location?.takeIf { it.isNotBlank() }?.let {
                                append(" ").append(BULLET_SEPARATOR).append(" ").append(it)
                            }
                        }

                        WidgetEntry(
                            label = label,
                            name = scheduled.course.name.ifBlank { "Untitled" },
                            subtitle = subtitle
                        )
                    }
                    .toList()

                WidgetState(
                    title = timetableName,
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
        private val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())
        private val AM_PM_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("a", Locale.getDefault())
        private const val BULLET_SEPARATOR: Char = '\u00B7'
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

private object MaterialThemeColors {
    val widgetBackground: Color = Color(0xFF1C1B1F)
    val onWidget: Color = Color(0xFFF5F5F5)
    val onWidgetSecondary: Color = Color(0xFFB0B0B0)
    val primary: Color = Color(0xFF80CBC4)
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

private fun safeGetString(context: Context, resId: Int): String? = try {
    context.getString(resId)
} catch (_: Throwable) {
    null
}
