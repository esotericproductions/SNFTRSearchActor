package com.exoteric.sharedactor.interactors.purchases

import com.exoteric.sharedactor.datasource.cached.models.ClockHistoryEntity
import com.exoteric.sharedactor.datasource.dtos.ClockHistoryDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.flowers.ClockEventHistoryFlower
import com.exoteric.sharedactor.interactors.user.parseProfilesBlobBlocked
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.flow.flow

class FetchClockEventHistory(private val snftrDatabase: SnftrDatabase) :
    ClockEventHistoryFlower {
    override fun executeClockHistorySearch(
        histories: MutableList<ClockHistoryEntity>?,
        threadUuid: String,
        uid: String
    ): SnftrFlow<DataState<List<ClockHistoryDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockEventHistoryQueries
            // filter out any duplicate purchases before putting into cache
            val allCachedIP =
                queries.getAllClockHistory(threadUuid = threadUuid).executeAsList()
            val filteredPurchases =
                histories?.distinct()
                    ?.filter { fp -> fp.thymestamp !in allCachedIP.map { it.thymestamp } }
            println("$TAG executeClockHistorySearch(): existing: ${allCachedIP.size} " +
                        "--- adding: ${filteredPurchases?.size}")
            if (filteredPurchases != null) {
                for (entity in filteredPurchases) {
                    queries.insertClockHistory(
                        id = getId(),
                        threadUuid = threadUuid,
                        uuid = entity.uuid,
                        uid = entity.uid,
                        name = entity.name,
                        username = entity.username,
                        profilePic = entity.profilePic,
                        statusBlob = "", // placeholder
                        status = entity.status,
                        thymestamp = entity.thymestamp
                    )
                }
            }
            val cacheResult =
                queries.getAllClockHistory(threadUuid = threadUuid).executeAsList()
            println("$TAG executeClockHistorySearch(): cacheResult -> ${cacheResult.size}")
            // map here since ClockHistory_Entity is generated by SQLDelight
            val list: ArrayList<ClockHistoryDto> = ArrayList()
            for (entity in cacheResult) {
                val user = snftrDatabase.snftrUsersQueries.searchUsersByUid(uid).executeAsOneOrNull()
                val blocked = if(user == null) false else parseProfilesBlobBlocked(user.profilesBlob)?.map { it.uid }?.contains(entity.uid) ?: false
                list.add(
                    ClockHistoryDto(
                        uuid = entity.uuid,
                        uid = entity.uid,
                        name = entity.name,
                        username = entity.username,
                        profilePic = entity.profilePic,
                        status = entity.status,
                        blocked = blocked,
                        thymestamp = entity.thymestamp
                    )
                )
            }
            println("$TAG Success! executeClockHistorySearch(): ${list.size}")
            // emit List<SnftrIDPDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(
                DataState.error<List<ClockHistoryDto>>(
                    e.message ?: "$TAG Unknown Error - message null"
                )
            )
        }
    }.snftrFlow()

    companion object {
        const val TAG = "ClockHistory"
    }
}