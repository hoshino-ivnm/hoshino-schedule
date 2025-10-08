package com.misaka.hoshinoschedule.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "course_id")
    val id: Long = 0,
    val name: String,
    val teacher: String?,
    val location: String?,
    val notes: String?,
    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null
)

@Entity(
    tableName = "course_times",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["course_id"],
            childColumns = ["course_owner_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["course_owner_id"])]
)
data class CourseTimeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "time_id")
    val id: Long = 0,
    @ColumnInfo(name = "course_owner_id")
    val courseId: Long,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    @ColumnInfo(name = "start_period")
    val startPeriod: Int,
    @ColumnInfo(name = "end_period")
    val endPeriod: Int,
    @ColumnInfo(name = "weeks")
    val weeks: String? = null
)

@Entity(tableName = "period_definitions")
data class PeriodDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "period_id")
    val id: Long = 0,
    @ColumnInfo(name = "sequence")
    val sequence: Int,
    @ColumnInfo(name = "start_minutes")
    val startMinutes: Int,
    @ColumnInfo(name = "end_minutes")
    val endMinutes: Int,
    @ColumnInfo(name = "label")
    val label: String? = null
)
