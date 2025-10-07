package com.misaka.kiraraschedule.data.repository

import com.misaka.kiraraschedule.data.local.CourseDao
import com.misaka.kiraraschedule.data.local.toEntity
import com.misaka.kiraraschedule.data.local.toModel
import com.misaka.kiraraschedule.data.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(private val courseDao: CourseDao) {

    fun observeAllCourses(): Flow<List<Course>> = courseDao.observeCourses()
        .map { list -> list.map { it.toModel() } }

    fun observeCoursesForDay(dayOfWeek: Int): Flow<List<Course>> =
        courseDao.observeCoursesForDay(dayOfWeek)
            .map { list -> list.map { it.toModel() } }

    suspend fun getAllCourses(): List<Course> =
        courseDao.getCourses().map { it.toModel() }

    suspend fun getCourse(id: Long): Course? = courseDao.getCourse(id)?.toModel()

    suspend fun upsert(course: Course) {
        val entity = course.toEntity()
        courseDao.upsertCourseWithTimes(
            course = entity,
            times = course.times.map { it.toEntity(entity.id).copy(id = 0) }
        )
    }

    suspend fun delete(course: Course) {
        courseDao.deleteCourse(course.toEntity())
    }
}
