package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrBkmkDb
data class ClockHistoryEntity(
    val uuid: String,
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val status: Long,
    val thymestamp: Long
)
