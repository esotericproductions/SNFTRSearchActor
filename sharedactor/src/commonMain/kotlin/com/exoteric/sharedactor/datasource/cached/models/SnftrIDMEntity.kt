package com.exoteric.sharedactor.datasource.cached.models

//goes to IDAWNMessages db
data class SnftrIDMEntity(
    val userUid: String,
    val title: String,
    val message: String,
    val messageData: String,
    val temperature: String,
    val pressure: String,
    val thymestamp: Double,
    val docId: String
)
