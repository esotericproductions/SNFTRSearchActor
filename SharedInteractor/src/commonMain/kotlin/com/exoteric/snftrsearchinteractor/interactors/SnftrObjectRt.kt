package com.exoteric.snftrsearchinteractor.interactors

// used for normal results/posts
interface SnftrObjectFatRt : SnftrObjectRt {
    override val id: Int
    override val title: String
    override val creator: String
    override val creatorProfilePic: String
    override val provider: String
    override val sourceUrlDetail: String
    override val sourceUrlThumb: String
    override val sourceUrlOwner: String
    override val posterUid: String
    val currentQuery: String
    override val randomSeed: Int
    override val thymestamp: Long
    val latestProfilePic: String
    override val page: Int
    val caller: String?
}

// used for favorites
interface SnftrObjectRt : SnftrObject {
    override val id: Int
    val title: String
    val creator: String
    val creatorProfilePic: String
    val provider: String
    val sourceUrlDetail: String
    val sourceUrlThumb: String
    val sourceUrlOwner: String
    val posterUid: String
    val randomSeed: Int
    val thymestamp: Long
    val page: Int
}

interface SnftrObject {
    val id: Int
}