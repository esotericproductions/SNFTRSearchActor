package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrDb
data class SnftrEntity(
    val id: Long,
    val title: String,
    val creator: String,
    val creatorProfilePic: String,
    val provider: String,
    val sourceUrl_detail: String,
    val sourceUrl_thumb: String,
    val sourceUrl_owner: String,
    val current_query: String,
    val thymeStamp: Long,
    val page: Int,
    val caller: String?
)
