package com.misaka.hoshinoschedule.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.misaka.hoshinoschedule.KiraraApp
import com.misaka.hoshinoschedule.ui.courselist.CourseListViewModel
import com.misaka.hoshinoschedule.ui.editor.CourseEditorViewModel
import com.misaka.hoshinoschedule.ui.schedule.ScheduleViewModel
import com.misaka.hoshinoschedule.ui.settings.SettingsViewModel

object AppViewModelProvider {

    val scheduleFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KiraraApp)
            val container = app.container
            ScheduleViewModel(
                courseRepository = container.courseRepository,
                periodRepository = container.periodRepository,
                settingsRepository = container.settingsRepository,
                reminderScheduler = container.reminderScheduler
            )
        }
    }

    val courseListFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KiraraApp)
            val container = app.container
            CourseListViewModel(
                courseRepository = container.courseRepository
            )
        }
    }

    val courseEditorFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KiraraApp)
            val container = app.container
            CourseEditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                courseRepository = container.courseRepository,
                periodRepository = container.periodRepository,
                settingsRepository = container.settingsRepository
            )
        }
    }

    val settingsFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KiraraApp)
            val container = app.container
            SettingsViewModel(
                settingsRepository = container.settingsRepository,
                periodRepository = container.periodRepository,
                dataTransferRepository = container.dataTransferRepository,
                reminderScheduler = container.reminderScheduler
            )
        }
    }
}
