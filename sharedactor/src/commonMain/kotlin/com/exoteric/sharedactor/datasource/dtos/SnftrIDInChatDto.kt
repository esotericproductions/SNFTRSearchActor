package com.exoteric.sharedactor.datasource.dtos

/**
 * Internal object used to consolidate insight chat convos
 */
data class SnftrIDInChatDto (
    val userUid: String,
    val profiledUid: String,
    val role: String,
    val message: String,
    val messageData: String,
    val insightId: String,
    val thymestamp: Long
)

