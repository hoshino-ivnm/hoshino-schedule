package com.misaka.hoshinoschedule

import android.content.Context
import com.misaka.hoshinoschedule.data.local.KiraraDatabase
import com.misaka.hoshinoschedule.data.repository.CourseRepository
import com.misaka.hoshinoschedule.data.repository.DataTransferRepository
import com.misaka.hoshinoschedule.data.repository.PeriodRepository
import com.misaka.hoshinoschedule.data.repository.SettingsRepository
import com.misaka.hoshinoschedule.data.settings.SettingsDataSource
import com.misaka.hoshinoschedule.data.settings.userPreferencesDataStore
import com.misaka.hoshinoschedule.data.work.ReminderScheduler

class AppContainer(context: Context) {
    private val database = KiraraDatabase.get(context)
    private val dataStore = context.userPreferencesDataStore

    val settingsRepository = SettingsRepository(SettingsDataSource(dataStore))
    val periodRepository = PeriodRepository(database.periodDao())
    val courseRepository = CourseRepository(database.courseDao())
    val dataTransferRepository = DataTransferRepository(
        context = context,
        courseDao = database.courseDao(),
        periodDao = database.periodDao(),
        settingsRepository = settingsRepository
    )
    val reminderScheduler = ReminderScheduler(context)
}
