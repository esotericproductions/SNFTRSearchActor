package com.exoteric.sharedactor.interactors.dtos

/**
 * Internal object used to consolidate provider api results
 */
data class ClockThreadDto (
    val uuid: String,
    val isDM: Boolean, // type
    val userUid: String,
    val ownerUid: String,
    val thymeStamp: Double,
    val messages: Int,
    val membersBlob: String,
    val members: Int,
    val info: String,
    val name: String,
    val latestUrl: String,
    val latestPostQ: String,
    val latestProfilePic: String,
    val originatorBlob: String
)

