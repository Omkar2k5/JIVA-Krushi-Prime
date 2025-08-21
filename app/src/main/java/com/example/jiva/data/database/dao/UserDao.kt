package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for UserEntity operations
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE UserID = :userId")
    suspend fun getUserById(userId: Int): UserEntity?
    
    @Query("SELECT * FROM users WHERE MobileNumber = :mobileNumber LIMIT 1")
    suspend fun getUserByMobile(mobileNumber: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE CompanyCode = :companyCode LIMIT 1")
    suspend fun getUserByCompanyCode(companyCode: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE IsActive = 1")
    fun getActiveUsers(): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users WHERE UserID = :userId")
    suspend fun deleteUserById(userId: Int)
    
    @Query("UPDATE users SET IsActive = :isActive WHERE UserID = :userId")
    suspend fun updateUserActiveStatus(userId: Int, isActive: Boolean)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
