package com.example.jiva.data.api.models

import kotlinx.serialization.Serializable

/**
 * Image upload response model
 */
@Serializable
data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String? = null,
    val error: String? = null
)
