package com.exoteric.sharedactor.datasource.dtos

/**
 * Internal object used to consolidate provider api results
 */
data class SnftrIDPDto (
    val hashName: String,
    val productId: String,
    val currency: String,
    val price: String,
    val thymestamp: Long,
    val expiry: Long,
)

