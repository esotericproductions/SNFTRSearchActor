package com.exoteric.sharedactor.interactors.dtos

import com.exoteric.sharedactor.interactors.SnftrObject

/**
 * Internal object used to consolidate provider api results
 */
data class SnftrUserDto (
    override val id: Int,
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val backgroundPic: String,
    val email: String,
    val pstrsTime: Long,
    val favsTime: Long,
    val cHistTime: Long,
    val cAttsTime: Long,
    val profilesBlob: String,
    val temperature: String,
    val pressure: String,
    val scoresBlob: String
) : SnftrObject

