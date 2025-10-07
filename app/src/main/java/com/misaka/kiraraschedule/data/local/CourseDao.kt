package com.misaka.kiraraschedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun observeCourses(): Flow<List<CourseWithTimes>>

    @Transaction
    @Query(
        "SELECT * FROM courses " +
                "INNER JOIN course_times ON courses.course_id = course_times.course_owner_id " +
                "WHERE course_times.day_of_week = :dayOfWeek " +
                "ORDER BY course_times.start_period ASC"
    )
    fun observeCoursesForDay(dayOfWeek: Int): Flow<List<CourseWithTimes>>

    @Transaction
    @Query("SELECT * FROM courses ORDER BY name ASC")
    suspend fun getCourses(): List<CourseWithTimes>

    @Transaction
    @Query("SELECT * FROM courses WHERE course_id = :id LIMIT 1")
    suspend fun getCourse(id: Long): CourseWithTimes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimes(times: List<CourseTimeEntity>)

    @Query("DELETE FROM course_times WHERE course_owner_id = :courseId")
    suspend fun deleteTimesForCourse(courseId: Long)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Transaction
    suspend fun upsertCourseWithTimes(course: CourseEntity, times: List<CourseTimeEntity>) {
        val courseId = if (course.id == 0L) insertCourse(course) else {
            updateCourse(course)
            course.id
        }
        deleteTimesForCourse(courseId)
        if (times.isNotEmpty()) {
            insertTimes(times.map { it.copy(courseId = courseId) })
        }
    }
}
