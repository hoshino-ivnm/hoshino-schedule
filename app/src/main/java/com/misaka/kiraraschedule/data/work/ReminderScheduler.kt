package com.misaka.kiraraschedule.data.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.model.PeriodDefinition
import com.misaka.kiraraschedule.data.settings.UserPreferences
import com.misaka.kiraraschedule.util.termStartDate
import com.misaka.kiraraschedule.widget.ScheduleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduler(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
    private val planner: SchedulePlanner = SchedulePlanner()
) {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val widget = ScheduleWidget()
    fun triggerTestNotification(title: String, subtitle: String? = null, delaySeconds: Long = 0) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putString(ReminderWorker.KEY_COURSE_NAME, title)
                    .putString(ReminderWorker.KEY_SUBTITLE, subtitle)
                    .putInt(
                        ReminderWorker.KEY_NOTIFICATION_ID,
                        (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                    )
                    .build()
            )
            .setInitialDelay(Duration.ofSeconds(delaySeconds.coerceAtLeast(0)))
            .addTag(TEST_NOTIFICATION_TAG)
            .build()
        workManager.enqueue(request)
    }

    fun triggerTestDnd(durationMinutes: Int) {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val enableTime = now.plusSeconds(1)
        val disableTime = enableTime.plusMinutes(durationMinutes.coerceAtLeast(1).toLong())
        enqueueDndToggle(enable = true, time = enableTime, now = now)
        enqueueDndToggle(enable = false, time = disableTime, now = now)
    }
    fun rescheduleAll(
        courses: List<Course>,
        periods: List<PeriodDefinition>,
        preferences: UserPreferences,
        daysAhead: Long = 14
    ) {
        workManager.cancelAllWorkByTag(REMINDER_TAG)
        workManager.cancelAllWorkByTag(DND_TAG)

        if (courses.isEmpty() || periods.isEmpty()) {
            widgetScope.launch { widget.updateAll(context) }
            return
        }

        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val upcoming = planner.buildUpcomingClasses(
            courses = courses,
            periods = periods,
            termStartDate = preferences.termStartDate(),
            totalWeeks = preferences.totalWeeks,
            zoneId = zoneId,
            startDate = now.toLocalDate(),
            daysAhead = daysAhead
        )

        scheduleReminders(upcoming, now, preferences)
        if (preferences.dndEnabled) {
            scheduleDnd(upcoming, now, preferences)
        }

        widgetScope.launch { widget.updateAll(context) }
    }

    private fun scheduleReminders(
        upcoming: List<ScheduledClass>,
        now: ZonedDateTime,
        preferences: UserPreferences
    ) {
        if (preferences.reminderLeadMinutes < 0) return
        upcoming.forEach { scheduled ->
            val triggerTime =
                scheduled.startDateTime.minusMinutes(preferences.reminderLeadMinutes.toLong())
            val duration = Duration.between(now, triggerTime)
            if (duration.isNegative) return@forEach
            val subtitle = buildString {
                scheduled.course.teacher?.takeIf { it.isNotBlank() }?.let { append(it) }
                if (scheduled.course.location?.isNotBlank() == true) {
                    if (isNotEmpty()) append(" | ")
                    append(scheduled.course.location)
                }
            }
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(ReminderWorker.KEY_COURSE_NAME, scheduled.course.name)
                        .putString(ReminderWorker.KEY_SUBTITLE, subtitle)
                        .putInt(
                            ReminderWorker.KEY_NOTIFICATION_ID,
                            (scheduled.startDateTime.toEpochSecond() % Int.MAX_VALUE).toInt()
                        )
                        .build()
                )
                .setInitialDelay(duration)
                .addTag(REMINDER_TAG)
                .build()
            workManager.enqueue(request)
        }
    }

    private fun scheduleDnd(
        upcoming: List<ScheduledClass>,
        now: ZonedDateTime,
        preferences: UserPreferences
    ) {
        if (upcoming.isEmpty()) return
        val threshold = preferences.dndSkipBreakThresholdMinutes.toLong()
        var currentEnableTime: ZonedDateTime? = null
        var currentDisableTime: ZonedDateTime? = null

        fun flushBlock() {
            val enableTime = currentEnableTime ?: return
            val disableTime = currentDisableTime ?: return
            enqueueDndToggle(enable = true, time = enableTime, now = now)
            enqueueDndToggle(enable = false, time = disableTime, now = now)
            currentEnableTime = null
            currentDisableTime = null
        }

        val sorted = upcoming.sortedBy { it.startDateTime }
        sorted.forEach { scheduled ->
            val enableTime =
                scheduled.startDateTime.minusMinutes(preferences.dndLeadMinutes.toLong())
            val disableTime =
                scheduled.endDateTime.plusMinutes(preferences.dndReleaseMinutes.toLong())
            if (currentEnableTime == null || currentDisableTime == null) {
                currentEnableTime = enableTime
                currentDisableTime = disableTime
                return@forEach
            }
            val previousDisable = currentDisableTime!!
            val gap = Duration.between(previousDisable, enableTime).toMinutes()
            if (gap <= threshold) {
                if (disableTime.isAfter(previousDisable)) {
                    currentDisableTime = disableTime
                }
            } else {
                flushBlock()
                currentEnableTime = enableTime
                currentDisableTime = disableTime
            }
        }
        flushBlock()
    }

    private fun enqueueDndToggle(enable: Boolean, time: ZonedDateTime, now: ZonedDateTime) {
        if (time.isBefore(now)) return
        val offset = Duration.between(now, time)
        if (offset.isNegative) return
        val request = OneTimeWorkRequestBuilder<DndToggleWorker>()
            .setInputData(Data.Builder().putBoolean(DndToggleWorker.KEY_ENABLE, enable).build())
            .setInitialDelay(offset)
            .addTag(DND_TAG)
            .build()
        workManager.enqueueUniqueWork(
            "${DND_TAG}_${time.toEpochSecond()}_${if (enable) "on" else "off"}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val REMINDER_TAG = "reminder_work"
        private const val DND_TAG = "dnd_work"
        private const val TEST_NOTIFICATION_TAG = "test_notification"
    }
}



