package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.PriceDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for PriceDataEntity operations
 */
@Dao
interface PriceDataDao {

    @Query("SELECT * FROM tb_pricelist")
    fun getAllPriceData(): Flow<List<PriceDataEntity>>

    @Query("SELECT * FROM tb_pricelist WHERE YearString = :yearString ORDER BY ItemName ASC")
    fun getByYear(yearString: String): Flow<List<PriceDataEntity>>

    @Query("SELECT * FROM tb_pricelist WHERE ItemID = :itemId")
    suspend fun getPriceDataByItemId(itemId: String): PriceDataEntity?

    @Query("SELECT * FROM tb_pricelist WHERE ItemName LIKE '%' || :searchTerm || '%'")
    fun searchPriceDataByItemName(searchTerm: String): Flow<List<PriceDataEntity>>

    @Query("SELECT * FROM tb_pricelist ORDER BY ItemName ASC")
    fun getAllPriceDataSortedByName(): Flow<List<PriceDataEntity>>

    @Query("SELECT * FROM tb_pricelist ORDER BY MRP DESC")
    fun getAllPriceDataSortedByMrp(): Flow<List<PriceDataEntity>>

    @Query("SELECT * FROM tb_pricelist ORDER BY CreditSaleRate DESC")
    fun getAllPriceDataSortedByCreditRate(): Flow<List<PriceDataEntity>>

    @Query("SELECT DISTINCT ItemName FROM tb_pricelist ORDER BY ItemName ASC")
    suspend fun getAllItemNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceData(priceData: PriceDataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPriceData(priceDataList: List<PriceDataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(priceDataList: List<PriceDataEntity>)

    @Query("DELETE FROM tb_pricelist WHERE YearString = :yearString")
    suspend fun deleteByYear(yearString: String)

    @Update
    suspend fun updatePriceData(priceData: PriceDataEntity)

    @Delete
    suspend fun deletePriceData(priceData: PriceDataEntity)

    @Query("DELETE FROM tb_pricelist WHERE ItemID = :itemId")
    suspend fun deletePriceDataByItemId(itemId: String)

    @Query("DELETE FROM tb_pricelist")
    suspend fun deleteAllPriceData()
}