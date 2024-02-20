package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrUserDb
data class SnftrUserEntity(
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val backgroundPic: String,
    val pstrsTime: Long,
    val favsTime: Long,
    val cHistTime: Long,
    val cAttsTime: Long,
    val email: String,
    val profilesBlob: String,
    val temperature: String,
    val pressure: String,
    val scoresBlob: String
)
