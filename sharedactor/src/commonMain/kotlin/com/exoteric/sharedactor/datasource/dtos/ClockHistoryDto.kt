package com.exoteric.sharedactor.datasource.dtos

/**
 * Internal object used to consolidate provider api results
 */
data class ClockHistoryDto (
    val uuid: String,
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val status: Long,
    val blocked: Boolean,
    val thymestamp: Long
)

