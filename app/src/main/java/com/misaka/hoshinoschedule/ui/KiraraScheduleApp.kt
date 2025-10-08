package com.misaka.hoshinoschedule.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misaka.hoshinoschedule.ui.courselist.CourseListRoute
import com.misaka.hoshinoschedule.ui.editor.CourseEditorRoute
import com.misaka.hoshinoschedule.ui.schedule.ScheduleRoute
import com.misaka.hoshinoschedule.ui.settings.SettingsRoute

object Routes {
    const val SCHEDULE = "schedule"
    const val COURSE_EDITOR = "courseEditor"
    const val SETTINGS = "settings"
    const val COURSE_LIST = "courses"
    const val COURSE_ID = "courseId"
}

@Composable
fun KiraraScheduleApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SCHEDULE) {
        composable(Routes.SCHEDULE) {
            val viewModel: com.misaka.hoshinoschedule.ui.schedule.ScheduleViewModel =
                viewModel(factory = AppViewModelProvider.scheduleFactory)
            ScheduleRoute(
                viewModel = viewModel,
                onAddCourse = { navController.navigate("${Routes.COURSE_EDITOR}") },
                onEditCourse = { courseId ->
                    navController.navigate("${Routes.COURSE_EDITOR}?${Routes.COURSE_ID}=$courseId")
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCourseList = { navController.navigate(Routes.COURSE_LIST) }
            )
        }
        composable(
            route = "${Routes.COURSE_EDITOR}?${Routes.COURSE_ID}={${Routes.COURSE_ID}}",
            arguments = listOf(
                navArgument(Routes.COURSE_ID) {
                    type = NavType.LongType
                    defaultValue = -1
                }
            )
        ) {
            val viewModel: com.misaka.hoshinoschedule.ui.editor.CourseEditorViewModel =
                viewModel(factory = AppViewModelProvider.courseEditorFactory)
            CourseEditorRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.COURSE_LIST) {
            val viewModel: com.misaka.hoshinoschedule.ui.courselist.CourseListViewModel =
                viewModel(factory = AppViewModelProvider.courseListFactory)
            CourseListRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddCourse = { navController.navigate("${Routes.COURSE_EDITOR}") },
                onEditCourse = { courseId ->
                    navController.navigate("${Routes.COURSE_EDITOR}?${Routes.COURSE_ID}=$courseId")
                }
            )
        }
        composable(Routes.SETTINGS) {
            val viewModel: com.misaka.hoshinoschedule.ui.settings.SettingsViewModel =
                viewModel(factory = AppViewModelProvider.settingsFactory)
            SettingsRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
