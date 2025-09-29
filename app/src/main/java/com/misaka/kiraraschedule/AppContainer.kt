package com.misaka.kiraraschedule

import android.content.Context
import com.misaka.kiraraschedule.data.local.KiraraDatabase
import com.misaka.kiraraschedule.data.repository.CourseRepository
import com.misaka.kiraraschedule.data.repository.DataTransferRepository
import com.misaka.kiraraschedule.data.repository.PeriodRepository
import com.misaka.kiraraschedule.data.repository.SettingsRepository
import com.misaka.kiraraschedule.data.settings.SettingsDataSource
import com.misaka.kiraraschedule.data.settings.userPreferencesDataStore
import com.misaka.kiraraschedule.data.work.ReminderScheduler

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
