package com.exoteric.sharedactor.datasource.dtos

import com.exoteric.sharedactor.interactors.SnftrObject

/**
 * Internal object used to consolidate provider api results
 */
data class ClockUserDto (
    override val id: Int,
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val backgroundPic: String,
    val email: String,
    val favsTime: Long,
    val profilesBlob: String,
    val temperature: String,
    val pressure: String,
    val scoresBlob: String
) : SnftrObject

