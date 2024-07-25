package com.exoteric.sharedactor.interactors.chat.threads

import com.exoteric.pypnft.cached.ChatThread_Entity
import com.exoteric.sharedactor.datasource.cached.models.SnftrIDCThreadEntity
import com.exoteric.sharedactor.datasource.dtos.ClockThreadDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.flowers.ClockThreadsFlower
import com.exoteric.sharedactor.interactors.parseOriginatorBlob
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
class SearchClockThreads(private val snftrDatabase: SnftrDatabase) : ClockThreadsFlower {
    override fun executeThreadsSearch(userUid: String,
                                      isFirstSearch: Boolean,
                                      threads: MutableList<SnftrIDCThreadEntity>?):
            SnftrFlow<DataState<List<ClockThreadDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockThreadQueries
            delay(if((threads?.size ?: 0) > 1) 500 else 0)
            val allCachedThreads =
                queries.selectAll(userUid = userUid).executeAsList().filter { it.cloud == 1L }
            println("$TAG - executeThreadsSearch().allCachedThreads -> ${allCachedThreads.size}")
            val allCachedMappedToUUID = allCachedThreads.map { it.uuid }
            // list of collections that do not have any rows in the db with matching uuid columns
            // aka: new collections
            val filteredThreadsNew =
                getNewFilteredThreadsOnlyNew(threads, allCachedMappedToUUID)
            if (filteredThreadsNew != null) {
//                for (entity in allCachedThreads) {
//                    updateUserLatestProPicFromEntity(entity)
//                }
                println("$TAG - executeThreadsSearch().filteredThreadsNew -> ${filteredThreadsNew.size}")
                for (entity in filteredThreadsNew) {
                    queries.insertThread(
                        id = getId(),
                        type = if(entity.isTimer) 1L else 0L,
                        synced = if(entity.synced) 1L else 0L,
                        uuid = entity.uuid,
                        userUid = entity.userUid,
                        ownerUid = entity.ownerUid,
                        messages = entity.messages.toLong(),
                        membersBlob = entity.membersBlob,
                        members = entity.members.toLong(),
                        info = entity.info,
                        name = entity.name,
                        latestUrl = entity.latestUrl,
                        latestPostQ = entity.latestPostQ,
                        latestProfilePic = entity.latestProfilePic,
                        originatorBlob = entity.originatorBlob,
                        latestAggTime = entity.latestAggTime.toLong(),
                        latestStartTime = entity.latestStartTime.toLong(),
                        latestPauseTime = entity.latestPauseTime.toLong(),
                        latestTimestamp = entity.latestTimestamp.toLong(),
                        latestStopTime = entity.latestStopTime.toLong(),
                        cloud = if(entity.cloud) 1L else 0L,
                        thymeStamp = entity.thymeStamp.toLong(),
                        event = entity.event.toLong()
                    )
                }
                println("$TAG - insertNewCThreadsIntoCache(): existing: " +
                        "${allCachedThreads.size} --- adding: " +
                        "${filteredThreadsNew.size}")
            }
            // if there are cached items AND there are incoming non-new collection items
//            if(allCachedMappedToUUID.isNotEmpty()) {
            println("$TAG - executeThreadsSearch().allCachedMappedToUUID -> ${allCachedMappedToUUID.size}")
            // Reduce the incoming collections down to only those
            // WHERE db has a row with matching uuid column,
            // AND the thymestamp does not match.
            // This means the row exists, but its timestamp is stale
            // so it needs to be updated.
            val filteredThreadsUpdate =
                getUpdateFilteredThreads(
                    threads,
                    allCachedMappedToUUID,
                    allCachedThreads,
                    isFirstSearch
                )?.filter { it.cloud }
            if (filteredThreadsUpdate != null) {
                for(entity in filteredThreadsUpdate) {
                    queries.updateThreadForThyme(
                        uuid = entity.uuid,
                        event = entity.event.toLong(),
                        synced = if(entity.synced) 1L else 0L,
                        messages = entity.messages.toLong(),
                        originator = entity.originatorBlob,
                        latestUrl = entity.latestUrl,
                        latestPostQ = entity.latestPostQ,
                        latestProfilePic = entity.latestProfilePic,
                        latestTimestamp = entity.latestTimestamp.toLong(),
                        latestStartTime = entity.latestStartTime.toLong(),
                        latestPauseTime = entity.latestPauseTime.toLong(),
                        latestStopTime = entity.latestStopTime.toLong(),
                        latestAggTime = entity.latestAggTime.toLong(),
                        members = entity.members.toLong(),
                        userUid = userUid
                    )
                }
                println(
                    "$TAG - updateCThreadsInCache(): existing: " +
                    "${allCachedThreads.size} --- updating: " +
                    "${filteredThreadsUpdate.size}"
                )
            }
//            }
            // query the cache
            val cacheResult = queries
                .restoreAllThread(
                    userUid = userUid,
                    limit = 60,  // TODO: better limit! Needs pagination...
                    offset = 0
                ).executeAsList()
            println("$TAG - cacheResult: ${cacheResult.size}")
            // map here since collections_Entity is generated by SQLDelight
            val list = arrayListOf<ClockThreadDto>()
            val results =
                if(!filteredThreadsUpdate.isNullOrEmpty()) {
                    cacheResult.filter { it -> it.uuid in filteredThreadsUpdate.map { it.uuid } }
                } else cacheResult.filter { it -> threads?.map { it.uuid }?.contains(it.uuid) == true }
            for (entity in results) {
                list.add(
                    getUpdatedThreadDto(entity, snftrDatabase)
                )
            }
            println("$TAG - Success! executeCThreadSearch(): ${list.size}")
            // emit List<SnftrIDCThreadDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockThreadDto>>(e.message ?: "Unknown Error - message null"))
        }
    }.snftrFlow()

    private fun updateUserLatestProPicFromEntity(entity: ChatThread_Entity) {
        val blob = parseOriginatorBlob(entity.originatorBlob)
        if (userIsInDb(blob.uid)) {
            val userQueries = snftrDatabase.snftrUsersQueries
            userQueries.updateUserForContentUpdate(
                name = blob.name,
                handle = blob.username,
                profilePic = entity.latestProfilePic,
                uid = blob.uid
            )
        }
    }

    // get only new collections; those not already in the db
    private fun getNewFilteredThreadsOnlyNew(
        collections: MutableList<SnftrIDCThreadEntity>?,
        allCachedMappedToUUID: List<String>
    ) = collections?.filter { collection -> (collection.uuid !in allCachedMappedToUUID)}

    // get only the collections that are already in the db
    private fun getNewFilteredThreadsPreExisting(
        collections: MutableList<SnftrIDCThreadEntity>?,
        allCachedMappedToUUID: List<String>
    ) = collections?.filter { collection -> (collection.uuid in allCachedMappedToUUID) }

    // filter the incoming [SnftrCollectionEntity] down to:
    // those items which have a row in the db with 1) a matching uuid
    // and 2) a non-matching timestamp (uuid is in cache && timestamp is stale)
    private fun getUpdateFilteredThreads(
        collections: MutableList<SnftrIDCThreadEntity>?,
        allCachedMappedToUUID: List<String>,
        allCachedThreads: List<ChatThread_Entity>,
        isFirstSearch: Boolean
    ) = collections?.filter { collection ->
        if(isFirstSearch) true
        else (collection.uuid in allCachedMappedToUUID)
                && collection.latestTimestamp.toLong() !in allCachedThreads.map { it.latestTimestamp }
                || (collection.latestStartTime.toLong() !in allCachedThreads.map { it.latestStartTime }
                    && collection.latestTimestamp.toLong() in allCachedThreads.map { it.latestTimestamp })
    }

    private fun userIsInDb(userUid: String): Boolean {
        val queries = snftrDatabase.snftrUsersQueries
        val allFollowing = queries
            .getFollowingUsers(listOf(userUid))
            .executeAsList()
        // only return the list of uid's from users that are not in the db
        return userUid in allFollowing.map { usr -> usr.uid }
    }

    companion object {
        const val TAG = "SearchCThreads"
    }
}

