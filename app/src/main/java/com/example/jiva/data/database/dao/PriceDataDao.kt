package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.PriceDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for PriceDataEntity operations
 */
@Dao
interface PriceDataDao {
    
    @Query("SELECT * FROM price_data")
    fun getAllPriceData(): Flow<List<PriceDataEntity>>
    
    @Query("SELECT * FROM price_data WHERE itemId = :itemId")
    suspend fun getPriceDataByItemId(itemId: String): PriceDataEntity?
    
    @Query("SELECT * FROM price_data WHERE itemName LIKE '%' || :searchTerm || '%'")
    fun searchPriceDataByItemName(searchTerm: String): Flow<List<PriceDataEntity>>
    
    @Query("SELECT * FROM price_data ORDER BY itemName ASC")
    fun getAllPriceDataSortedByName(): Flow<List<PriceDataEntity>>
    
    @Query("SELECT * FROM price_data ORDER BY mrp DESC")
    fun getAllPriceDataSortedByMrp(): Flow<List<PriceDataEntity>>
    
    @Query("SELECT * FROM price_data ORDER BY creditSaleRate DESC")
    fun getAllPriceDataSortedByCreditRate(): Flow<List<PriceDataEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceData(priceData: PriceDataEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPriceData(priceDataList: List<PriceDataEntity>)
    
    @Update
    suspend fun updatePriceData(priceData: PriceDataEntity)
    
    @Delete
    suspend fun deletePriceData(priceData: PriceDataEntity)
    
    @Query("DELETE FROM price_data WHERE itemId = :itemId")
    suspend fun deletePriceDataByItemId(itemId: String)
    
    @Query("DELETE FROM price_data")
    suspend fun deleteAllPriceData()
}