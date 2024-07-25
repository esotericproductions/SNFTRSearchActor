package com.exoteric.sharedactor.interactors.chat.threads

import com.exoteric.pypnft.cached.ChatThread_Entity
import com.exoteric.sharedactor.datasource.cached.models.IDCThreadIconDetails
import com.exoteric.sharedactor.datasource.dtos.ClockIDUsrChatDto
import com.exoteric.sharedactor.datasource.dtos.ClockThreadDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.expressions.getUserExpressionsForSnftrDto
import com.exoteric.sharedactor.interactors.flowers.ClockThreadsCacheFlower
import com.exoteric.sharedactor.interactors.parseOriginatorBlob
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.ITEMS_PER_PG_PROVIDER_SEARCH
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
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
//            delay(500)
            // query the cache
            val queries = snftrDatabase.clockThreadQueries
            val cacheResult = queries
                .getAllThreadByThymeForSubscriber(userUid = userUid)
                .executeAsList()
            println("$TAG --> fetchCachedThreadsFromDb: " + cacheResult.size)
            // emit List<SnftrCollectionDto> from cache
            // Must manually map this since collections_Entity object is generated by SQL Delight
            val list = arrayListOf<ClockThreadDto>()
            for (entity in cacheResult.filter { it.cloud == 0L }) {
                list.add(
                    getUpdatedThreadDto(entity, snftrDatabase)
                )
            }
            println("$TAG --> fetchCachedThreadsFromDb: emitting -> " + list.size)
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

    fun getCachedThreadSyncForUuid(uuid: String, userUid: String): Boolean {
        val queries = snftrDatabase.clockThreadQueries
        val entity = queries.getThreadByUuid(uuid = uuid, userUid = userUid).executeAsOneOrNull()
        val synced = if (entity != null) {
            entity.synced == 1L
        } else {
            false
        }
        println("$TAG getCachedThreadSyncForUuid(): $synced")
        return synced
    }

    fun checkCachedThreadLatestTimeSynced(uuid: String, userUid: String, serverLatestTimestamp: Long): Boolean {
        val queries = snftrDatabase.clockThreadQueries
        val entity = queries.getThreadByUuid(uuid, userUid).executeAsOneOrNull()
        val synced = if (entity != null) {
            entity.latestTimestamp == serverLatestTimestamp
        } else {
            false
        }
        println("$TAG checkCachedThreadLatestTimeSynced(): $synced -> ${entity?.latestTimestamp} == $serverLatestTimestamp")
        return synced
    }

    fun updateThreadNotSynced(uuid: String, userUid: String) {
        val queries = snftrDatabase.clockThreadQueries
        queries.updateThreadSyncStatus(0L, userUid, uuid)
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
                val blob = parseOriginatorBlob(entity.originatorBlob)
                val latestUserData =
                    getCachedUserData(
                        uid = userUid,
                        entityProPic = entity.latestProPic,
                        entityName = blob.name,
                        entityUsername = blob.username,
                        snftrDatabase
                    )
                val newBlob = createOriginatorBlob(
                    name = latestUserData.name,
                    username = latestUserData.username,
                    uid = userUid
                )
                chat = ClockIDUsrChatDto(
                    userUid = entity.userUid,
                    posterUid = entity.posterUid,
                    chatUid = entity.chatUid,
                    type = entity.type,
                    threadUid = entity.threadUid,
                    message = latestPostQ ,
                    latestProPic = latestUserData.proPic,
                    messageData = entity.messageData,
                    scoresBlob = entity.scoresBlob,
                    membersBlob = entity.membersBlob,
                    originatorBlob = newBlob,
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
            return getUpdatedThreadDto(entity, snftrDatabase)
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
        val channel = query.getThreadForUpdateValidation(uuid = uuid, userUid = userUid).executeAsOneOrNull()
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

    fun updateCachedThreadForAggTime(
        latestAggTime: Long,
        latestStartTime: Long,
        latestTimestamp: Long,
        members: Long,
        latestProfilePic: String,
        originatorBlob: String,
        event: Long,
        userUid: String,
        uuid: String,
        completion: (result: Boolean) -> Unit
    ) {
        val query = snftrDatabase.clockThreadQueries
        query.updateCachedThreadForAggTime(
            latestAggTime = latestAggTime,
            latestStartTime = latestStartTime,
            latestTimestamp = latestTimestamp,
            members = members,
            latestProfilePic = latestProfilePic,
            originatorBlob = originatorBlob,
            event = event,
            userUid = userUid,
            uuid = uuid
        )
        val channel = query.getThreadForUpdateValidation(uuid, userUid).executeAsOneOrNull()
        if (channel != null) {
            val updated = latestAggTime == channel.latestAggTime
            completion(updated)
        } else {
            completion(false)
        }
    }

    fun updateCachedIconDetails(membersBlob: String,
                                messages: Long,
                                info: String,
                                uuid: String,
                                userUid: String,
                                completion: (result: Boolean) -> Unit) {
//        println("updateCachedIconDetails(): $membersBlob")
        val query = snftrDatabase.clockThreadQueries
        val channel0 = query.getThreadForUpdateValidation(uuid, userUid).executeAsOneOrNull()
        if (channel0 != null && channel0.uuid == uuid) {
            query.updateThreadForIconDetails(
                membersBlob = membersBlob,
                messages = messages,
                info = info,
                uuid = uuid,
                userUid = userUid
            )
            val channel = query.getThreadForUpdateValidation(uuid, userUid).executeAsOneOrNull()
            if (channel != null) {
                val notUpdated = membersBlob != channel.membersBlob
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

    fun hasCached(userUid: String): Boolean {
        val queries = snftrDatabase.clockThreadQueries
        val numberOfCached = queries
            .selectAll(
                userUid = userUid
            )
            .executeAsList()
        println("$TAG hasCached(): ${numberOfCached.size}")
        return numberOfCached.isNotEmpty()
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

data class CachedUserProfileInfo(val name: String, val username: String, val proPic: String)

fun getCachedUserData(
    uid: String,
    entityProPic: String,
    entityName: String,
    entityUsername: String,
    snftrDatabase: SnftrDatabase
): CachedUserProfileInfo {
    val query = snftrDatabase.snftrUsersQueries
    val user = query.searchUsersByUid(uid = uid).executeAsOneOrNull()
    return if (user != null
        && user.profilePic.isNotEmpty()
        && user.name.isNotEmpty()
        && user.username.isNotEmpty()) {
//        println("getCachedUserProfilePic(): ${user.profilePic}")
        CachedUserProfileInfo(user.name, user.username, user.profilePic)
    } else {
        CachedUserProfileInfo(entityName, entityUsername, entityProPic)
    }
}

fun getCachedUserProfilePic(uid: String, snftrDatabase: SnftrDatabase): String? {
    val query = snftrDatabase.snftrUsersQueries
    val user = query.searchUsersByUid(uid = uid).executeAsOneOrNull()
    return if (user != null && user.profilePic.isNotEmpty()) {
//        println("getCachedUserProfilePic(): ${user.profilePic}")
        user.profilePic
    } else {
        null
    }
}

fun getUpdatedThreadDto(
    entity: ChatThread_Entity,
    snftrDatabase: SnftrDatabase
): ClockThreadDto {
    val blob = parseOriginatorBlob(entity.originatorBlob)
    val latestUserData =
        getCachedUserData(
            uid = blob.uid,
            entityProPic = entity.latestProfilePic,
            entityName = blob.name,
            entityUsername = blob.username,
            snftrDatabase
        )
    val newBlob = createOriginatorBlob(
        name = latestUserData.name,
        username = latestUserData.username,
        uid = blob.uid
    )

    return ClockThreadDto(
        uuid = entity.uuid,
        isTimer = entity.type == 1L,
        userUid = entity.userUid,
        ownerUid = entity.ownerUid,
        messages = entity.messages.toInt(),
        membersBlob = entity.membersBlob,
        members = entity.members.toInt(),
        info = entity.info,
        name = entity.name,
        latestUrl = entity.latestUrl,
        latestPostQ = entity.latestPostQ,
        latestProfilePic = latestUserData.proPic,
        originatorBlob = newBlob,
        latestAggTime = entity.latestAggTime.toDouble(),
        latestStartTime = entity.latestStartTime.toDouble(),
        latestPauseTime = entity.latestPauseTime.toDouble(),
        latestStopTime = entity.latestStopTime.toDouble(),
        latestTimestamp = entity.latestTimestamp.toDouble(),
        cloud = entity.cloud == 1L,
        thymeStamp = entity.thymeStamp.toDouble(),
        event = entity.event.toInt(),
        synced = entity.synced == 1L
    )
}