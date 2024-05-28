package com.exoteric.sharedactor.interactors.chat.threads

import com.exoteric.sharedactor.datasource.cached.models.IDCThreadIconDetails
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.datasource.dtos.ClockThreadDto
import com.exoteric.sharedactor.datasource.dtos.ClockIDUsrChatDto
import com.exoteric.sharedactor.interactors.expressions.getUserExpressionsForSnftrDto
import com.exoteric.sharedactor.interactors.flowers.ClockThreadsCacheFlower
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.ITEMS_PER_PG_PROVIDER_SEARCH
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Restore a [ClockThreadDto] from clockThreadQueries.
 */
class RestoreClockThreads(private val snftrDatabase: SnftrDatabase): ClockThreadsCacheFlower {
    @Throws(Exception::class)
    override fun fetchCachedThreadsFromDb(userUid: String):
            SnftrFlow<DataState<List<ClockThreadDto>>> = flow {
        try {
            emit(DataState.loading())
            delay(500)
            // query the cache
            val queries = snftrDatabase.clockThreadQueries
            val cacheResult = queries
                .getAllThreadByThymeForSubscriber(userUid = userUid)
                .executeAsList()
            println("$TAG --> fetchCachedThreadsFromDb: " + cacheResult.size)
            // emit List<SnftrCollectionDto> from cache
            // Must manually map this since collections_Entity object is generated by SQL Delight
            val list = arrayListOf<ClockThreadDto>()
            for (entity in cacheResult) {
                list.add(
                    ClockThreadDto(
                        uuid = entity.uuid,
                        isDM = entity.type == 1L,
                        userUid = userUid,
                        ownerUid = entity.ownerUid,
                        messages = entity.messages.toInt(),
                        membersBlob = entity.membersBlob,
                        members = entity.members.toInt(),
                        info = entity.info,
                        name = entity.name,
                        latestUrl = entity.latestUrl,
                        latestPostQ = entity.latestPostQ,
                        latestProfilePic = getCachedUserProfilePic(parseOriginatorBlob(entity.originatorBlob).uid, snftrDatabase) ?: entity.latestProfilePic,
                        originatorBlob = entity.originatorBlob,
                        latestAggTime = entity.latestAggTime.toDouble(),
                        latestStartTime = entity.latestStartTime.toDouble(),
                        latestPauseTime = entity.latestPauseTime.toDouble(),
                        latestTimestamp = entity.latestTimestamp.toDouble(),
                        startTime = entity.startTime.toDouble(),
                        thymeStamp = entity.thymeStamp.toDouble(),
                        event = entity.event.toInt()
                    )
                )
            }
            emit(DataState.success(list))
        } catch (e: Exception){ emit(DataState.error<List<ClockThreadDto>>(
            e.message?: "RestoreCThreads: Unknown Error")) }
    }.snftrFlow()

    fun getUpdatedThreadForLocalPost(chatUUID: String,
                                     latestTimestamp: Long,
                                     userUid: String?): ClockThreadDto? {
        if(userUid.isNullOrEmpty()) { return null }
        println("$TAG getUpdatedThreadForLocalPost(): updating...")
        val chatDto = getIDUsrChatMessageByUUID(chatUUID, userUid)
        if(chatDto != null) {
            val queries = snftrDatabase.clockThreadQueries
            val latestPostQ = if (chatDto.message.length > 150) {
                chatDto.message.substring(0, 150)
            } else {
                chatDto.message
            }
            queries.updateThreadForLocalPost(
                uuid = chatDto.threadUid,
                originator = chatDto.originatorBlob,
                latestPostQ = latestPostQ,
                latestProfilePic = chatDto.latestProPic,
                latestTimestamp = latestTimestamp,
                membersBlob = chatDto.membersBlob,
                userUid = userUid
            )
            val threadDto = getCachedCThreadByUUID(chatDto.threadUid, userUid)
            println("getUpdatedThreadForLocalPost(): updated!")
            return threadDto
        }
        return null
    }

    /**
     * 1. fetch the most recent chat message from the chatsDB for the given threadId
     * 2. parse the chat message data and update the threadsDB with current data
     * 3. send an updated SnftrIDUsrChatDto to UI layer
     */
    private fun getIDUsrChatMessageByUUID(uuid: String, userUid: String?): ClockIDUsrChatDto? {
        if(userUid.isNullOrEmpty()) { return null }
        println("$TAG getIDUsrChatMessageByUUID()")
        val queries = snftrDatabase.clockChatMessagesQueries
        val entity = queries
            .getChatCommentByUuid(uuid)
            .executeAsOneOrNull()
        var chat: ClockIDUsrChatDto? = null
        if(entity != null) {
            getUserExpressionsForSnftrDto(
                uuid = entity.chatUid,
                userUid = userUid,
                snftrDatabase = snftrDatabase
            ) {
                // this matches logic in AnalyzeChatMessage.js:58
                val latestPostQ = if (entity.message.length > 150) {
                    entity.message.substring(0, 150)
                } else {
                    entity.message
                }
                chat = ClockIDUsrChatDto(
                    userUid = entity.userUid,
                    posterUid = entity.posterUid,
                    chatUid = entity.chatUid,
                    type = entity.type,
                    threadUid = entity.threadUid,
                    message = latestPostQ ,
                    latestProPic = getCachedUserProfilePic(parseOriginatorBlob(entity.originatorBlob).uid, snftrDatabase) ?: entity.latestProPic,
                    messageData = entity.messageData,
                    scoresBlob = entity.scoresBlob,
                    membersBlob = entity.membersBlob,
                    originatorBlob = entity.originatorBlob,
                    thymestamp = (entity.thymestamp * 1000),
                    thumbsdownsCount = entity.thumbsdownsCount,
                    thumbsupsCount = entity.thumbsupsCount,
                    // see SnftrCommentDto for details on these fields...
                    isFav = it.fav,
                    thumbsups = it.up,
                    thumbsdowns = it.down,
                    flagged = it.flagged
                )
            }
            return chat
        }
        return null
    }

    private fun getCachedCThreadByUUID(uuid: String, userUid: String?): ClockThreadDto? {
        if(userUid.isNullOrEmpty()) { return null }
//        println("$TAG getCachedCThreadByUUID()")
        val queries = snftrDatabase.clockThreadQueries
        val entity = queries.getThreadByUuid(uuid, userUid)
            .executeAsOneOrNull()
        if(entity != null) {
            return ClockThreadDto(
                uuid = entity.uuid,
                isDM = entity.type == 1L,
                userUid = userUid,
                ownerUid = entity.ownerUid,
                messages = entity.messages.toInt(),
                membersBlob = entity.membersBlob,
                members = entity.members.toInt(),
                info = entity.info,
                name = entity.name,
                latestUrl = entity.latestUrl,
                latestPostQ = entity.latestPostQ,
                latestProfilePic = getCachedUserProfilePic(entity.ownerUid, snftrDatabase) ?: entity.latestProfilePic,
                originatorBlob = entity.originatorBlob,
                latestAggTime = entity.latestAggTime.toDouble(),
                latestStartTime = entity.latestStartTime.toDouble(),
                latestPauseTime = entity.latestPauseTime.toDouble(),
                latestTimestamp = entity.latestTimestamp.toDouble(),
                startTime = entity.startTime.toDouble(),
                thymeStamp = entity.thymeStamp.toDouble(),
                event = entity.event.toInt()
            )
        }
        return null
    }

    fun isThreadCachedAndViewed(uuid: String,
                                userUid: String?,
                                latestPostQ: String): Int {
        if(userUid.isNullOrEmpty()) { return 0 }
        val thread = getCachedCThreadByUUID(uuid, userUid)
        val isCached = thread != null && thread.latestPostQ == latestPostQ
//        println("$TAG isThreadCachedAndViewed(): $isCached")
        return if(isCached) 1 else 0
    }

    fun getCachedCThreadsIconValsByUuid(uuid: String,
                                        userUid: String,
                                        completion: (result: IDCThreadIconDetails) -> Unit) {
        val query = snftrDatabase.clockThreadQueries
        val channel = query.getThreadForUpdateValidation(uuid, userUid).executeAsOneOrNull()
        if(channel != null) {
            completion(
                IDCThreadIconDetails(
                    membersBlob = channel.membersBlob,
                    messages = channel.messages.toInt(),
                    info = channel.info,
                    ownerUid = channel.ownerUid,
                    name = channel.name
                )
            )
        } else {
            completion(IDCThreadIconDetails("",0, "", "", "")) // just send an empty obj as response
        }
    }

    fun updateCachedIconDetails(membersBlob: String,
                                messages: Long,
                                info: String,
                                uuid: String,
                                userUid: String,
                                completion: (result: Boolean) -> Unit) {
        println("updateCachedIconDetails(): $uuid -> membersBlob: $membersBlob")
        val query = snftrDatabase.clockThreadQueries
        val channel0 = query.getThreadForUpdateValidation(uuid, userUid).executeAsOneOrNull()
        if (channel0 != null && channel0.uuid == uuid && channel0.membersBlob != membersBlob) {
            query.updateThreadForIconDetails(
                membersBlob = membersBlob,
                messages = messages,
                info = info,
                uuid = uuid,
                userUid = userUid
            )
            val channel = query.getThreadForUpdateValidation(uuid, userUid).executeAsOneOrNull()
            if (channel != null) {
                val notUpdated = channel.membersBlob == membersBlob && channel.uuid == uuid
                completion(!notUpdated)
            } else {
                completion(false)
            }
        } else {
            completion(false)
        }
    }

    /**
     * Remove a member from a cached chat thread membersBlob
     */
    fun removeMemberFromCThread(uuid: String,
                                userUid: String,
                                memberUid: String,
                                completion: (result: Boolean) -> Unit) {
        val query = snftrDatabase.clockThreadQueries
        val entity = query.getThreadByUuid(uuid, userUid)
            .executeAsOneOrNull()
        if(entity != null) {
            val members = stringToArray(entity.membersBlob)
            if(members != null) {
                val updatedMembers = members.filter { member -> member != memberUid }
                val updatedBlob = arrayToString(updatedMembers)
                query.updateMembersBlobForRemoval(updatedBlob, uuid)
                val secondEntity = query.getThreadByUuid(uuid, userUid)
                    .executeAsOneOrNull()
                if(secondEntity != null) {
                    if(stringToArray(secondEntity.membersBlob)?.contains(memberUid) == true) {
                        println("removeMemberFromCThread(): member not removed!")
                        completion(false)
                    } else {
                        println("removeMemberFromCThread(): member removed!")
                        completion(true)
                    }
                }

            }
        }
    }

    fun resetForNewLogin(): Boolean {
        snftrDatabase.clockThreadQueries.deleteForLogout()
        println("$TAG resetForNewLogin()")
        return true
    }
    fun getDMItemCount(uid: String): Int {
        val itemCount = snftrDatabase.clockThreadQueries
            .selectCountAll(type = 1, uid = uid)
            .executeAsOneOrNull()?.toInt() ?: -1
        println("$TAG getItemCount() -> $itemCount")
        return itemCount
    }
    fun getGroupItemCount(uid: String): Int {
        val itemCount = snftrDatabase.clockThreadQueries
            .selectCountAll(type = 0, uid = uid)
            .executeAsOneOrNull()?.toInt() ?: -1
        println("$TAG getItemCount() -> $itemCount")
        return itemCount
    }

    fun hasCachedDMs(uid: String?): Boolean {
        if (uid == null) return false
        val itemsInCache = getDMItemCount(uid = uid)
        // enough to fill a whole new page?
        val goToNextPageWithCache = itemsInCache >= ITEMS_PER_PG_PROVIDER_SEARCH
        println("$TAG hasCachedDMs(): $itemsInCache items in total")
        return goToNextPageWithCache
    }

    fun hasCached(type: Long,
                  userUid: String): Boolean {
        val queries = snftrDatabase.clockThreadQueries
        val numberOfCached = queries
            .getAllThreadCount(
                type = type,
                userUid = userUid
            )
            .executeAsOne()
        println("$TAG hasCached(): $numberOfCached")
        return numberOfCached > 0
    }

    fun newestCachedTimestamp(type: Long,
                              userUid: String): Long {
        val newest = snftrDatabase.clockThreadQueries
            .getNewest(
                type = type,
                userUid = userUid
            ).executeAsOneOrNull()?.thymeStamp ?: 0
        println("$TAG newestCachedTimestamp(uid): $newest")
        return newest
    }

    fun oldestCachedTimestamp(type: Long,
                              userUid: String): Long {
        val oldest = snftrDatabase.clockThreadQueries
            .getOldest(
                type = type,
                userUid = userUid
            ).executeAsOneOrNull()?.thymeStamp ?: 0
        println("$TAG oldestCachedTimestamp(uid): $oldest")
        return oldest
    }

    fun deleteCThreadsForUserUnsub(collectionUuid: String, userUid: String) {
        val query = snftrDatabase.clockThreadQueries
        query.deleteThreadForUnsub(uuid = collectionUuid, userUid = userUid)
    }

    companion object {
        const val TAG = "RestoreCThreads"
        const val MOST_RECENT_PG_CTHREADS = "most_recent_pg_cthreads"
        const val CAT_PG_CTHREADS = "cat_pg_cthreads"
    }
}

fun createOriginatorBlob(name: String,
                         username: String,
                         uid: String): String {
    val json = buildJsonObject {
        put("name", name)
        put("username", username)
        put("uid", uid)
    }
    return json.toString()
}

fun arrayToString(array: List<String>): String {
    return Json.encodeToString(array)
}

fun stringToArray(jsonString: String): List<String>? {
    return try {
        Json.decodeFromString<List<String>>(jsonString)
    } catch (e: Exception) {
        println("Error converting JSON string to array: ${e.message}")
        null
    }
}

fun getCachedUserProfilePic(uid: String, snftrDatabase: SnftrDatabase): String? {
    println("getCachedUserProfilePic(): $uid")
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