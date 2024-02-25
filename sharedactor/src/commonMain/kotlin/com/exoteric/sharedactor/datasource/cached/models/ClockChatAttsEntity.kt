package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrCommentsHistoryDb
data class ClockChatAttsEntity(
    val chatUid: String,
    val mediaBlob: String,
    val originatorBlob: String,
    val posterUid: String,
    val threadUid: String,
    val thymeStamp: Long,
    val meta: String,
    )
