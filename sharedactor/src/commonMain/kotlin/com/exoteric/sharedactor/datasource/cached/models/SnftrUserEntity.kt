package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrUserDb
data class SnftrUserEntity(
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val backgroundPic: String,
    val favsTime: Long,
    val email: String,
    val profilesBlob: String,
    val temperature: String,
    val pressure: String,
    val scoresBlob: String
)
