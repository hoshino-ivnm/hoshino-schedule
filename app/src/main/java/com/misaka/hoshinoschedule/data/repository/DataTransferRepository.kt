package com.misaka.hoshinoschedule.data.repository

import android.content.Context
import android.net.Uri
import com.misaka.hoshinoschedule.data.local.CourseDao
import com.misaka.hoshinoschedule.data.local.PeriodDao
import com.misaka.hoshinoschedule.data.local.toEntity
import com.misaka.hoshinoschedule.data.local.toModel
import com.misaka.hoshinoschedule.data.model.Course
import com.misaka.hoshinoschedule.data.model.PeriodDefinition
import com.misaka.hoshinoschedule.data.settings.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataTransferRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val periodDao: PeriodDao,
    private val settingsRepository: SettingsRepository
) {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun exportTo(uri: Uri) {
        val backup = ScheduleBackup(
            courses = courseDao.getCourses().map { it.toModel() },
            periods = periodDao.getPeriodDefinitions().map { it.toModel() },
            preferences = settingsRepository.preferences.first()
        )
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.bufferedWriter().use { writer ->
                writer.write(json.encodeToString(backup))
            }
        } ?: error("Unable to open export destination")
    }

    suspend fun importFrom(uri: Uri) {
        val backup = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().use { reader ->
                json.decodeFromString<ScheduleBackup>(reader.readText())
            }
        } ?: error("Unable to read import source")

        courseDao.clearCourses()
        periodDao.clear()

        if (backup.periods.isNotEmpty()) {
            periodDao.insertAll(backup.periods.map { it.toEntity() })
        }

        backup.courses.forEach { course ->
            val courseId = courseDao.insertCourse(course.toEntity().copy(id = 0))
            val times = course.times.map { time ->
                time.toEntity(courseId).copy(id = 0, courseId = courseId)
            }
            if (times.isNotEmpty()) {
                courseDao.insertTimes(times)
            }
        }

        settingsRepository.replaceAll(backup.preferences)
    }
}

@Serializable
private data class ScheduleBackup(
    val courses: List<Course>,
    val periods: List<PeriodDefinition>,
    val preferences: UserPreferences
)
