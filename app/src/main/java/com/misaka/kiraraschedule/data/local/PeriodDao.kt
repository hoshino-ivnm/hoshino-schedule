package com.misaka.kiraraschedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_definitions ORDER BY sequence ASC")
    fun observePeriodDefinitions(): Flow<List<PeriodDefinitionEntity>>

    @Query("SELECT * FROM period_definitions ORDER BY sequence ASC")
    suspend fun getPeriodDefinitions(): List<PeriodDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(period: PeriodDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(periods: List<PeriodDefinitionEntity>)

    @Update
    suspend fun update(period: PeriodDefinitionEntity)

    @Delete
    suspend fun delete(period: PeriodDefinitionEntity)

    @Query("DELETE FROM period_definitions")
    suspend fun clear()
}
