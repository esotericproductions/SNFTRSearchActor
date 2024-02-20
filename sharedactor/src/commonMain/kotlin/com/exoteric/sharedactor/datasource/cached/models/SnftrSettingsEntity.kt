package com.exoteric.sharedactor.datasource.cached.models

//goes to SettingsDb
data class SnftrSettingsEntity(
    val category: String,
    val key: String,
    val value: String,
    val thymestamp: Long
)
