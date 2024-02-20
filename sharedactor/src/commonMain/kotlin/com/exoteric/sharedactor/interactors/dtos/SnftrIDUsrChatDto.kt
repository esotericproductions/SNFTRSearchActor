package com.exoteric.sharedactor.interactors.dtos

/**
 * Internal object used to consolidate insight chat convos
 */
data class SnftrIDUsrChatDto (
    val userUid: String,
    val posterUid: String,
    val latestProPic: String,
    val chatUid: String,
    val threadUid: String,
    val message: String,
    val messageData: String,
    val scoresBlob: String,
    val membersBlob: String,
    val originatorBlob: String,
    val type: Long,
    val thymestamp: Long,
    // isFav, thumbsups, thumbsdowns: are only used client side,
    // only used when fetching from cache
    // (dto's surfaced to UI from network calls get all 0's)
    // those from cache will get a supplemental db call to
    // SnftrUserExpressionsDb to assign values if any.
    val isFav: Long,
    val thumbsups: Long,
    val thumbsdowns: Long,
    val flagged: Long,
    // used for caching locally?
    val thumbsupsCount: Long,
    val thumbsdownsCount: Long
)

