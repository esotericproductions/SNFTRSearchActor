package com.exoteric.sharedactor.interactors.catts

import com.exoteric.sharedactor.datasource.cached.models.ClockChatAttsEntity
import com.exoteric.sharedactor.datasource.dtos.ClockChatAttsDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.favs.SearchSnftrFavorites
import com.exoteric.sharedactor.interactors.favs.setUpdatedUserFavTime
import com.exoteric.sharedactor.interactors.flowers.SnftrChatAttsFlower
import com.exoteric.sharedactor.interactors.user.SearchSnftrUser
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.ITEMS_PER_PG_FVRTS
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class SearchSnftrChatAtts(private val snftrDatabase: SnftrDatabase) : SnftrChatAttsFlower {
    override fun executeChatAttsSearch(cmmts: MutableList<ClockChatAttsEntity>?,
                                       uid: String,
                                       currentSnftrUserUid: String?,
                                       page: Int,
                                       caller: String,
                                       isNewSearch: Boolean):
            SnftrFlow<DataState<List<ClockChatAttsDto>>> = flow {
        try {
            emit(DataState.loading())
            // delete the stored page value for the given caller on new search, resetting to page 1
            if(isNewSearch) {
                snftrDatabase
                    .snftrSettingsQueries
                    .deleteAllSettingsByTab(category = RestoreSnftrChatAtts.CAT_PG_CATTS + caller)
            }
            // only delay for iOS for pagination,
            // it causes problems on Android,
            // but it allows for proper paging UX in iOS
            delay(500)
            val queries = snftrDatabase.clockChatAttsHistoryQueries
            // filter out any duplicate urls before putting into cache
            val allCachedCAtts = queries.selectAllForUserId(posterUid = uid).executeAsList()
            setUpdatedUserCAttsTime(uid, snftrDatabase)
            // 1. check the db for any rows where urlThumb == source_url_thumb
            // 2. filter out any duplicates
            val filteredCAtts =
                cmmts
                    ?.distinct()
                    ?.filter { fp -> fp.chatUid !in allCachedCAtts.map { it.chatUid } }
            println("$TAG executeChatAttsSearch(): existing: ${allCachedCAtts.size} " +
                    "--- adding: ${filteredCAtts?.size}")
            if (filteredCAtts != null) {
                for (entity in filteredCAtts) {
                    queries.insertChatAtt(
                        id = getId(),
                        chatUid = entity.chatUid, // documentID in firebase
                        threadUid = entity.threadUid,
                        originatorBlob = entity.originatorBlob,
                        posterUid = entity.posterUid,
                        mediaBlob = entity.mediaBlob,
                        meta = entity.meta,
                        // dependency for ternary logic in grid anims
                        random_seed = (1..3201).random().toLong(),
                        thymeStamp = entity.thymeStamp
                    )
                }
            }
            val cacheResult = queries
                .restoreAllChatAttsForPageByUid(
                    posterUid = uid,
                    offset = ((page - 1) * ITEMS_PER_PG_FVRTS).toLong(),
                    limit = ITEMS_PER_PG_FVRTS.toLong(),
                ).executeAsList()
            println("$TAG cacheResult: ${cacheResult.size}")
            // map here since chatAttHist_Entity is generated by SQLDelight
            val list = arrayListOf<ClockChatAttsDto>()
            for (entity in cacheResult) {
                list.add(
                    ClockChatAttsDto(
                        id = entity.id.toInt(),
                        chatUid = entity.chatUid, // documentID in firebase
                        threadUid = entity.threadUid,
                        originatorBlob = entity.originatorBlob,
                        posterUid = entity.posterUid,
                        mediaBlob = entity.mediaBlob,
                        meta = entity.meta,
                        // dependency for ternary logic in grid anims
                        randomSeed = entity.random_seed.toInt(),
                        thymestamp = entity.thymeStamp,
                        page = 0
                    )
                )
            }
            println("$TAG Success! executeCmmtsHSearch(): ${list.size}")
            // emit List<SnftrCHistDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockChatAttsDto>>(e.message ?: "$TAG Unknown Error - message null"))
        }
    }.snftrFlow()

    fun addSnftrCAttSingle(entity: ClockChatAttsEntity,
                           myCallback: (added: Boolean) -> Unit) {
        val query = snftrDatabase.clockChatAttsHistoryQueries
        if (query.getChatAttByUuid(entity.chatUid).executeAsOneOrNull() == null) {
            query.insertChatAtt(
                id = getId(),
                chatUid = entity.chatUid, // documentID in firebase
                threadUid = entity.threadUid,
                originatorBlob = entity.originatorBlob,
                posterUid = entity.posterUid,
                mediaBlob = entity.mediaBlob,
                meta = entity.meta,
                // dependency for ternary logic in grid anims
                random_seed = (1..3201).random().toLong(),
                thymeStamp = entity.thymeStamp
            )
            setUpdatedUserCAttsTime(entity.posterUid, snftrDatabase)
            val check = query.getChatAttByUuid(chatUid = entity.chatUid).executeAsOneOrNull()
            val wasItAdded = check?.chatUid == entity.chatUid
            println("$TAG addSnftrCAttSingle(): for entity with time: ${entity.thymeStamp} -- added? $wasItAdded")
            myCallback(wasItAdded)
        }
    }

    fun updateUserCAttsTime(uid: String, cAttsTime: Long, completion: (updated: Boolean) -> Unit) {
        println("$TAG updateUserCAttsTime():")
        val queries = snftrDatabase.snftrUsersQueries
        queries.updateUserForCAttsThyme(cAttsTime, uid)
        val user = queries.searchUsersByUid(uid).executeAsOneOrNull()
        if (user != null) {
            val updatedCAttsTime = user.cAttsTime
            println("$TAG updateUserCAttsTime() -> updated cAttsTime!")
            val updated = updatedCAttsTime == cAttsTime
            if (updated) setUpdatedUserCAttsTime(uid, snftrDatabase)
            completion(updated)
        } else {
            println("$TAG updateUserCAttsTime(): update failed!")
            completion(false)
        }
    }

    companion object {
        const val TAG = "SearchChatAtts"
    }
}


fun setUpdatedUserCAttsTime(uid: String, snftrDatabase: SnftrDatabase) {
    val userQ = snftrDatabase.snftrUsersQueries
    val newestIncoming = (userQ
        .searchUsersByUid(uid = uid)
        .executeAsOneOrNull()?.cAttsTime ?: -1) / 1000
    if (newestIncoming > 0) {
        // update its local copy if valid
        val query = snftrDatabase.clockChatAttsHistoryQueries
        val latestLocal = query
            .getNewest(posterUid = uid)
            .executeAsOneOrNull()
        if (latestLocal != null) {
            query.updateNewest(
                chatUid = latestLocal.chatUid,
                posterUid = uid,
                timestamp = newestIncoming
            )
        }
    }
}