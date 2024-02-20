package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrBkmkDb
data class SnftrIDPEntity(
    val hashName: String,
    val currency: String,
    val price: String,
    val productId: String,
    val thymestamp: Long,
    val expiry: Long
)
