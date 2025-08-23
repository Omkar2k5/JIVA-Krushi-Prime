package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for tb_templates table
 * Maps to MySQL table: tb_templates
 */
@Entity(tableName = "tb_templates")
@Serializable
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNo")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int,
    
    @ColumnInfo(name = "TempID")
    val tempId: String,
    
    @ColumnInfo(name = "Category")
    val category: String? = null,
    
    @ColumnInfo(name = "Msg")
    val msg: String? = null,
    
    @ColumnInfo(name = "InstanceID")
    val instanceId: String? = null,
    
    @ColumnInfo(name = "AccessToken")
    val accessToken: String? = null
)
