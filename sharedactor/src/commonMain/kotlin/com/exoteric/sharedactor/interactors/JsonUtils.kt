package com.exoteric.sharedactor.interactors

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class ClockThreadInfo(val uuid: String, val type: String, val alarmTimes: List<Long>?)

data class OriginatorBlob(val username: String, val name: String, val uid: String)

fun parseThreadInfoBlob(jsonString: String): ClockThreadInfo {
    val json = Json
    val jsonTree = json.parseToJsonElement(jsonString)
    val jsonObject = jsonTree.jsonObject
    val alarmTimes = jsonObject["alarmTimes"]
        ?.jsonArray
        ?.mapNotNull { it.jsonPrimitive.longOrNull }
    val type = jsonTree.jsonObject["type"]?.jsonPrimitive?.content ?: ""
    val threadUuid = jsonTree.jsonObject["uuid"]?.jsonPrimitive?.content ?: ""
    println("parseThreadInfoBlob: $type for $threadUuid")
    return ClockThreadInfo(threadUuid, type, alarmTimes)
}

fun generateThreadInfoJson(threadUuid: String, type: String, alarmTimes: List<Long>?): String {
    val jsonObject = buildJsonObject {
        put("uuid", threadUuid)
        put("type", type)
        if (alarmTimes != null) {
            put("alarmTimes", JsonArray(alarmTimes.map { JsonPrimitive(it) }))
        }
    }
    return Json.encodeToString(JsonElement.serializer(), jsonObject)
}

fun clockThreadInfoToJson(clockThreadInfo: ClockThreadInfo): String {
    val jsonObject = buildJsonObject {
        put("uuid", clockThreadInfo.uuid)
        put("type", clockThreadInfo.type)
        clockThreadInfo.alarmTimes?.let {
            put("alarmTimes", JsonArray(it.map { time -> JsonPrimitive(time) }))
        }
    }
    return Json.encodeToString(JsonElement.serializer(), jsonObject)
}

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

fun chatMediaToJson(supplementalChatMedia: SupplementalChatMedia): String {
    val json = Json
    val jsonObject = buildJsonObject {
        put("chatUuid", supplementalChatMedia.chatUuid)
        put("threadUuid", supplementalChatMedia.threadUuid)
        put("mediaProvider", supplementalChatMedia.mediaProvider)
        put("mediaThumb", supplementalChatMedia.mediaThumb)
        put("mediaDetail", supplementalChatMedia.mediaDetail)
        put("mediaOwnerUrl", supplementalChatMedia.mediaOwnerUrl)
    }
    return json.encodeToString(JsonObject.serializer(), jsonObject)
}

fun isValidJson(jsonString: String): Boolean {
    return try {
        val json = Json
        val jsonTree = json.parseToJsonElement(jsonString)
        val type = jsonTree.jsonObject["type"]?.jsonPrimitive?.content ?: ""
        println("isValidJson: $type")
        true
    } catch (e: Exception) {
        println("isValidJson: FALSE --> $jsonString")

        false // If any exception occurs, it's not a valid JSON
    }
}