package com.exoteric.sharedactor.interactors.chat.threads

import com.exoteric.pypnft.cached.ChatThread_Entity
import com.exoteric.sharedactor.datasource.cached.models.SnftrIDCThreadEntity
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.datasource.dtos.ClockThreadDto
import com.exoteric.sharedactor.interactors.flowers.ClockThreadsFlower
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
class SearchIDCThreads(private val snftrDatabase: SnftrDatabase) : ClockThreadsFlower {
    override fun executeThreadsSearch(userUid: String,
                                      threads: MutableList<SnftrIDCThreadEntity>?):
            SnftrFlow<DataState<List<ClockThreadDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockThreadQueries
            delay(500)
            val allCachedThreads = queries.selectAll(userUid = userUid).executeAsList()
            val allCachedMappedToUUID = allCachedThreads.map { it.uuid }
            // list of collections that do not have any rows in the db with matching uuid columns
            // aka: new collections
            val filteredThreadsNew = getNewFilteredThreadsOnlyNew(threads, allCachedMappedToUUID)
            if (filteredThreadsNew != null) {
                for (entity in filteredThreadsNew) {
                    queries.insertThread(
                        id = getId(),
                        type = if(entity.isDM) 1L else 0L,
                        uuid = entity.uuid,
                        userUid = entity.userUid,
                        ownerUid = entity.ownerUid,
                        messages = entity.messages.toLong(),
                        membersBlob = entity.membersBlob,
                        members = entity.members.toLong(),
                        info = entity.info,
                        name = entity.name,
                        thymeStamp = entity.thymeStamp.toLong(),
                        latestUrl = entity.latestUrl,
                        latestPostQ = entity.latestPostQ,
                        latestProfilePic = entity.latestProfilePic,
                        originatorBlob = entity.originatorBlob,
                    )
                }
                println("$TAG insertNewCThreadsIntoCache(): existing: " +
                        "${allCachedThreads.size} --- adding: " +
                        "${filteredThreadsNew.size}")
            }
            // if there are cached items AND there are incoming non-new collection items
            if(allCachedMappedToUUID.isNotEmpty()) {
                // Reduce the incoming collections down to only those
                // WHERE db has a row with matching uuid column,
                // AND the thymestamp does not match.
                // This means the row exists, but its timestamp is stale
                // so it needs to be updated.
                val filteredThreadsUpdate =
                    getUpdateFilteredThreads(
                        threads,
                        allCachedMappedToUUID,
                        allCachedThreads
                    )
                if (filteredThreadsUpdate != null) {
                    for(entity in filteredThreadsUpdate) {
                        queries.updateThreadForThyme(
                            uuid = entity.uuid,
                            messages = entity.messages.toLong(),
                            originator = entity.originatorBlob,
                            latestUrl = entity.latestUrl,
                            latestPostQ = entity.latestPostQ,
                            latestProfilePic = entity.latestProfilePic,
                            thymeStamp = entity.thymeStamp.toLong(),
                            userUid = userUid
                        )
                    }
                    println(
                        "$TAG updateCThreadsInCache(): existing: " +
                        "${allCachedThreads.size} --- updating: " +
                        "${filteredThreadsUpdate.size}"
                    )
                }
            }
            // query the cache
            val cacheResult = queries
                .restoreAllThread(
                    userUid = userUid,
                    limit = 60,  // TODO: better limit!
                    offset = 0
                ).executeAsList()
            println("$TAG cacheResult: ${cacheResult.size}")
            // map here since collections_Entity is generated by SQLDelight
            val list = arrayListOf<ClockThreadDto>()
            for (entity in cacheResult) {
                list.add(
                    ClockThreadDto(
                        uuid = entity.uuid,
                        isDM = entity.type == 1L,
                        userUid = userUid,
                        ownerUid = entity.ownerUid,
                        thymeStamp = entity.thymeStamp.toDouble(),
                        messages = entity.messages.toInt(),
                        membersBlob = entity.membersBlob,
                        members = entity.members.toInt(),
                        info = entity.info,
                        name = entity.name,
                        latestUrl = entity.latestUrl,
                        latestPostQ = entity.latestPostQ,
                        latestProfilePic = getCachedUserProfilePic(parseOriginatorBlob(entity.originatorBlob).uid, snftrDatabase) ?: entity.latestProfilePic,
                        originatorBlob = entity.originatorBlob
                        )
                )
            }
            println("$TAG Success! executeCThreadSearch(): ${list.size}")
            // emit List<SnftrIDCThreadDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockThreadDto>>(e.message ?: "Unknown Error - message null"))
        }
    }.snftrFlow()

    // get only new collections; those not already in the db
    private fun getNewFilteredThreadsOnlyNew(
        collections: MutableList<SnftrIDCThreadEntity>?,
        allCachedMappedToUUID: List<String>
    ) = collections?.filter { collection -> (collection.uuid !in allCachedMappedToUUID) }

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
        allCachedThreads: List<ChatThread_Entity>
    ) = collections?.filter { collection ->
        (collection.uuid in allCachedMappedToUUID)
                && (collection.thymeStamp.toLong() !in allCachedThreads.map { it.thymeStamp })
    }

    companion object {
        const val TAG = "SearchCThreads"
    }
}
