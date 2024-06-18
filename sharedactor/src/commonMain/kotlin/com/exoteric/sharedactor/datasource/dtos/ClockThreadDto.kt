package com.exoteric.sharedactor.datasource.dtos

/**
 * Internal object used to consolidate provider api results
 */
data class ClockThreadDto (
    val uuid: String,
    val isTimer: Boolean, // type
    val synced: Boolean, // type
    val userUid: String,
    val ownerUid: String,
    val messages: Int,
    val membersBlob: String,
    val members: Int,
    val info: String,
    val name: String,
    val latestUrl: String,
    val latestPostQ: String,
    val latestProfilePic: String,
    val originatorBlob: String,
    val thymeStamp: Double,
    val latestAggTime: Double,
    val latestTimestamp: Double,
    val latestStartTime: Double,
    val latestPauseTime: Double,
    val latestStopTime: Double,
    var startTime: Double,
    val event: Int
    )

