package com.exoteric.sharedactor.datasource.dtos

/**
 * Internal object used to consolidate provider api results
 */
data class SnftrIDMDto (
    val userUid: String,
    val title: String,
    val message: String,
    val messageData: String,
    val temperature: String,
    val pressure: String,
    val thymestamp: Double,
    val docId: String
)

