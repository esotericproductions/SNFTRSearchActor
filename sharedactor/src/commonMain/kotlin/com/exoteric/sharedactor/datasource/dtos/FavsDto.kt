package com.exoteric.sharedactor.datasource.dtos

import com.exoteric.sharedactor.interactors.SnftrObjectRt


/**
 * Internal object used to consolidate provider api results
 */
data class FavsDto(
    override val id: Int,
    val uid: String, // uid for querying in favorites rtdb
    val cmmtUuid: String, // uid for quering in bookmarks rtdb (not added if DetailCaller == .strends)
    override val title: String,
    override val creator: String,
    override val creatorProfilePic: String,
    override val provider: String,
    override val sourceUrlDetail: String,
    override val sourceUrlThumb: String,
    override val sourceUrlOwner: String,
    override val posterUid: String,
    override val randomSeed: Int,
    override val thymestamp: Long,
    override val page: Int
    ) : SnftrObjectRt

