package com.linkod.global.data

data class Category(
    val id: Int,
    val slug: String = "",
    val name: String,
    val base_price_minor: Int,
    val currency: String
)

data class JobCreateRequest(
    val category_id: Int,
    val address: String,
    val lat: Double,
    val lng: Double,
    val access_notes: String? = null,
    val unit_details: String? = null,
    val description: String? = (if (!unit_details.isNullOrBlank()) "Unit: $unit_details. " else "") + (access_notes ?: "")
)

data class JobResponse(
    val id: String,
    val customer_id: Int? = null,
    val provider_id: Int? = null,
    val category_id: Int? = null,
    val status: String,
    val address: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val price_minor: Int = 0,
    val currency: String = "PHP",
    val description: String? = null,
    val created_at: Any? = null,
    val updated_at: Any? = null
)

data class Offer(
    val id: String,
    val job_id: String,
    val provider_id: Int? = null,
    val status: String,
    val expires_at: Any? = null,
    val created_at: Any? = null
)

data class LocationRequest(
    val lat: Double,
    val lng: Double,
    val active: Boolean
)
