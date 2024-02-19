package com.exoteric.snftrsearchinteractor.datasource.cached.models

//goes to IDAWNChatThread.sq
data class SnftrIDCThreadEntity(
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
    val originatorBlob: String)

data class IDCThreadIconDetails(
    val membersBlob: String,
    val messages: Int,
    val info: String,
    val ownerUid: String,
    val name: String
)


