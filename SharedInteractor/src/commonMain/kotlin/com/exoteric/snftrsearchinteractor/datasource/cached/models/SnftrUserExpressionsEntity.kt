package com.exoteric.snftrsearchinteractor.datasource.cached.models

//goes to SnftrUserDb
data class SnftrUserExpressionsEntity(
    val cmmtUuid: String,
    val userUid: String,
    val isFav: Long,
    val thumbsup: Long,
    val thumbsdown: Long,
)
