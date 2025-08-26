package com.example.jiva.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import com.example.jiva.data.database.entities.OutstandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutstandingDao {

    @Query("SELECT * FROM Outstanding WHERE yearString = :year ORDER BY accountName ASC")
    fun getAll(year: String): Flow<List<OutstandingEntity>>

    @Query("SELECT * FROM Outstanding WHERE yearString = :year AND accountName LIKE '%' || :search || '%' ORDER BY accountName ASC")
    fun searchByName(year: String, search: String): Flow<List<OutstandingEntity>>

    // Lightweight paging for low-end devices
    @Query("SELECT * FROM Outstanding WHERE yearString = :year ORDER BY accountName ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(year: String, limit: Int, offset: Int): List<OutstandingEntity>

    @Query("SELECT COUNT(*) FROM Outstanding WHERE yearString = :year")
    suspend fun count(year: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<OutstandingEntity>)

    @Query("DELETE FROM Outstanding WHERE yearString = :year")
    suspend fun clearYear(year: String)

    @Transaction
    suspend fun replaceForYear(year: String, list: List<OutstandingEntity>) {
        clearYear(year)
        if (list.isNotEmpty()) insertAll(list)
    }
}