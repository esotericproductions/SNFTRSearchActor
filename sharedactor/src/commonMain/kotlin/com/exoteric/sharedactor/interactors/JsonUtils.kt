package com.exoteric.sharedactor.interactors

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


data class OriginatorBlob(val username: String, val name: String, val uid: String)

fun parseOriginatorBlob(jsonString: String): OriginatorBlob {
    val json = Json
    val jsonTree = json.parseToJsonElement(jsonString)
    val username = jsonTree.jsonObject["username"]?.jsonPrimitive?.content ?: ""
    val name = jsonTree.jsonObject["name"]?.jsonPrimitive?.content ?: ""
    val uid = jsonTree.jsonObject["uid"]?.jsonPrimitive?.content ?: ""
    return OriginatorBlob(username, name, uid)
}

data class SupplementalChatMedia(
    val chatUuid: String,
    val threadUuid: String?,
    val mediaThumb: String,
    val mediaDetail: String,
    val mediaOwnerUrl: String,
    val mediaProvider: String
)

fun parseChatMediaBlob(jsonString: String): SupplementalChatMedia {

    val json = Json
    val jsonTree = json.parseToJsonElement(jsonString)
    val chatUuid = jsonTree.jsonObject["chatUuid"]?.jsonPrimitive?.content ?: ""
    val threadUuid = jsonTree.jsonObject["threadUuid"]?.jsonPrimitive?.content ?: ""
    val mediaProvider = jsonTree.jsonObject["mediaProvider"]?.jsonPrimitive?.content ?: ""
    val mediaThumb = jsonTree.jsonObject["mediaThumb"]?.jsonPrimitive?.content ?: ""
    val mediaDetail = jsonTree.jsonObject["mediaDetail"]?.jsonPrimitive?.content ?: ""
    val mediaOwnerUrl = jsonTree.jsonObject["mediaOwnerUrl"]?.jsonPrimitive?.content ?: ""
    return SupplementalChatMedia(
        chatUuid = chatUuid,
        threadUuid = threadUuid,
        mediaProvider = mediaProvider,
        mediaThumb = mediaThumb,
        mediaDetail = mediaDetail,
        mediaOwnerUrl = mediaOwnerUrl
    )
}