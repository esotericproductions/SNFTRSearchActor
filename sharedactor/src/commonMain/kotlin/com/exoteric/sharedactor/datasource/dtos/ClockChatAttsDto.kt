package com.exoteric.sharedactor.datasource.dtos

import com.exoteric.sharedactor.interactors.SnftrObject

/**
 * Internal object used for snftr user chat attachments history grid
 */
data class ClockChatAttsDto(
    override val id: Int,
    val chatUid: String,
    val mediaBlob: String,
    val originatorBlob: String,
    val posterUid: String,
    val threadUid: String,
    val meta: String,
    // grid stuffs
    val randomSeed: Int,
    val thymestamp: Long,
    val page: Int
) : SnftrObject

