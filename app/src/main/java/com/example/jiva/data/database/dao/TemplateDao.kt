package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.TemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for TemplateEntity operations
 */
@Dao
interface TemplateDao {
    
    @Query("SELECT * FROM tb_templates")
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    
    @Query("SELECT * FROM tb_templates WHERE SrNo = :srNo")
    suspend fun getTemplateBySrNo(srNo: Int): TemplateEntity?
    
    @Query("SELECT * FROM tb_templates WHERE TempID = :tempId")
    suspend fun getTemplateByTempId(tempId: String): TemplateEntity?
    
    @Query("SELECT * FROM tb_templates WHERE CmpCode = :cmpCode")
    fun getTemplatesByCompany(cmpCode: Int): Flow<List<TemplateEntity>>
    
    @Query("SELECT * FROM tb_templates WHERE Category = :category")
    fun getTemplatesByCategory(category: String): Flow<List<TemplateEntity>>
    
    @Query("SELECT * FROM tb_templates WHERE Category LIKE '%' || :searchTerm || '%'")
    fun searchTemplatesByCategory(searchTerm: String): Flow<List<TemplateEntity>>
    
    @Query("SELECT DISTINCT Category FROM tb_templates WHERE Category IS NOT NULL ORDER BY Category")
    suspend fun getAllCategories(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)
    
    @Update
    suspend fun updateTemplate(template: TemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
    
    @Query("DELETE FROM tb_templates WHERE SrNo = :srNo")
    suspend fun deleteTemplateBySrNo(srNo: Int)
    
    @Query("DELETE FROM tb_templates WHERE TempID = :tempId")
    suspend fun deleteTemplateByTempId(tempId: String)
    
    @Query("DELETE FROM tb_templates")
    suspend fun deleteAllTemplates()
}
