package com.exoteric.sharedactor.datasource.cached.models

//goes to IDAWNInsightChat db
data class SnftrIDUsrChatEntity(
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
    val thumbsupsCount: Long,
    val thumbsdownsCount: Long
)
