package com.misaka.kiraraschedule.ui.courselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misaka.kiraraschedule.data.model.Course
import com.misaka.kiraraschedule.data.repository.CourseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CourseListUiState(
    val courses: List<Course> = emptyList()
)

class CourseListViewModel(
    private val courseRepository: CourseRepository
) : ViewModel() {

    val uiState: StateFlow<CourseListUiState> = courseRepository.observeAllCourses()
        .map { CourseListUiState(courses = it.sortedBy { course -> course.name }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CourseListUiState()
        )
}
