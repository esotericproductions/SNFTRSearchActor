package com.exoteric.sharedactor.interactors.dtos

import com.exoteric.sharedactor.interactors.SnftrObjectFatRt

/**
 * Internal object used to consolidate provider api results
 */
data class SnftrDto(
    override val id: Int,
    override val title: String,
    override val creator: String,
    override val creatorProfilePic: String,
    override val provider: String,
    override val sourceUrlDetail: String,
    override val sourceUrlThumb: String,
    override val sourceUrlOwner: String,
    override val posterUid: String,
    override val currentQuery: String,
    override val randomSeed: Int,
    override val thymestamp: Long,
    override val latestProfilePic: String,
    override val page: Int,
    override val caller: String?
    ) : SnftrObjectFatRt

