package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrFavoritesDb
data class FavsEntity(
    val provider: String,
    val title: String,
    val originator: String,
    val creator: String,
    val creatorProfilePic: String,
    val userId: String,
    val uid: String,
    val uuid: String,
    val urlDetail: String,
    val urlThumb: String,
    val urlOwner: String,
    val thymeStamp: Long
    )
