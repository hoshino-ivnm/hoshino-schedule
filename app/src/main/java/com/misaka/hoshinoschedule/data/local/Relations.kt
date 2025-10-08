package com.misaka.hoshinoschedule.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Aggregated model that surfaces a course alongside its scheduled time slots.
 */
data class CourseWithTimes(
    @Embedded
    val course: CourseEntity,
    @Relation(
        parentColumn = "course_id",
        entityColumn = "course_owner_id"
    )
    val times: List<CourseTimeEntity>
)
