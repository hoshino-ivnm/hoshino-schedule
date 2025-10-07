package com.misaka.kiraraschedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.misaka.kiraraschedule.KiraraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in HANDLED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? KiraraApp
                val container = app?.container
                if (container != null) {
                    val courses = container.courseRepository.getAllCourses()
                    val periods = container.periodRepository.getPeriods()
                    val preferences = container.settingsRepository.preferences.first()
                    container.reminderScheduler.rescheduleAll(
                        courses = courses,
                        periods = periods,
                        preferences = preferences
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
