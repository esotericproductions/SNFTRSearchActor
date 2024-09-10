package com.exoteric.sharedactor.interactors.user

import com.exoteric.snftrdblib.cached.SnftrDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@Serializable
data class ProfilesBlob(val following: ArrayList<ProfilesInnards>?, val blocked: ArrayList<ProfilesInnards>?)

@Serializable
data class ProfilesInnards(val uid: String, val time: String)

fun createProfilesBlob(following: ArrayList<ProfilesInnards>?, blocked: ArrayList<ProfilesInnards>?): String {
    val profilesBlob = ProfilesBlob(following, blocked)
    return Json.encodeToString(ProfilesBlob.serializer(), profilesBlob)
}

fun parseProfilesBlobFollowing(jsonString: String?): ArrayList<ProfilesInnards>? {
    return if (jsonString.isNullOrEmpty()) {
        null
    } else {
        val profilesBlob = Json.decodeFromString(serializer<ProfilesBlob>(), jsonString)
        profilesBlob.following
    }
}

fun parseProfilesBlobBlocked(jsonString: String?): ArrayList<ProfilesInnards>? {
    return if (jsonString.isNullOrEmpty()) {
        null
    } else {
        val profilesBlob = Json.decodeFromString(serializer<ProfilesBlob>(), jsonString)
        profilesBlob.blocked
    }
}

fun getTimeForUID(jsonString: String?, uid: String): String {
    if(jsonString.isNullOrEmpty()) { return "0" }
    val profilesBlob = Json.decodeFromString(serializer<ProfilesBlob>(), jsonString)
    val followingItem = profilesBlob.following?.find { it.uid == uid }
    return followingItem?.time ?: "0"
}

fun getCachedUserProfilePic(uid: String, snftrDatabase: SnftrDatabase): String? {
    val query = snftrDatabase.snftrUsersQueries
    val user = query.searchUsersByUid(uid = uid).executeAsOneOrNull()
    return if (user != null && user.profilePic.isNotEmpty()) {
        user.profilePic
    } else {
        null
    }
}

data class OriginatorBlob(val username: String, val name: String, val uid: String)

fun parseOriginatorBlob(jsonString: String): OriginatorBlob {
    val json = Json
    val jsonTree = json.parseToJsonElement(jsonString)
    val username = jsonTree.jsonObject["username"]?.jsonPrimitive?.content ?: ""
    val name = jsonTree.jsonObject["name"]?.jsonPrimitive?.content ?: ""
    val uid = jsonTree.jsonObject["uid"]?.jsonPrimitive?.content ?: ""
    return OriginatorBlob(username, name, uid)
}
fun createOriginatorBlob(name: String, username: String, uid: String): String {
    val jsonDict = mapOf(
        "name" to name,
        "username" to username,
        "uid" to uid
    )
    return Json.encodeToString<Map<String, String>>(jsonDict)
}