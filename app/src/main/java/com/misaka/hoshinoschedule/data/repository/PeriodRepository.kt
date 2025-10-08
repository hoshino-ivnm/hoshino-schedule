package com.misaka.hoshinoschedule.data.repository

import com.misaka.hoshinoschedule.data.local.PeriodDao
import com.misaka.hoshinoschedule.data.local.toEntity
import com.misaka.hoshinoschedule.data.local.toModel
import com.misaka.hoshinoschedule.data.model.PeriodDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PeriodRepository(private val periodDao: PeriodDao) {

    fun observePeriods(): Flow<List<PeriodDefinition>> = periodDao.observePeriodDefinitions()
        .map { list -> list.map { it.toModel() } }

    suspend fun getPeriods(): List<PeriodDefinition> =
        periodDao.getPeriodDefinitions().map { it.toModel() }

    suspend fun replaceAll(periods: List<PeriodDefinition>) {
        periodDao.clear()
        periodDao.insertAll(periods.map { it.toEntity() })
    }

    suspend fun ensureDefaults() {
        val current = observePeriods().first()
        if (current.isEmpty()) {
            val defaults = (1..8).map { index ->
                PeriodDefinition(
                    sequence = index,
                    startMinutes = 8 * 60 + (index - 1) * 45,
                    endMinutes = 8 * 60 + index * 45,
                    label = "第${index}节"
                )
            }
            replaceAll(defaults)
        }
    }
}
