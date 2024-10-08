package com.exoteric.sharedactor.interactors.flowers

import com.exoteric.sharedactor.datasource.cached.models.ClockChatAttsEntity
import com.exoteric.sharedactor.datasource.cached.models.FavsEntity
import com.exoteric.sharedactor.datasource.cached.models.SnftrIDCThreadEntity
import com.exoteric.sharedactor.datasource.cached.models.ClockHistoryEntity
import com.exoteric.sharedactor.datasource.cached.models.SnftrIDUsrChatEntity
import com.exoteric.sharedactor.datasource.cached.models.SnftrUserEntity
import com.exoteric.sharedactor.datasource.dtos.ClockChatAttsDto
import com.exoteric.sharedactor.datasource.dtos.ClockThreadDto
import com.exoteric.sharedactor.datasource.dtos.SnftrDto
import com.exoteric.sharedactor.datasource.dtos.ClockIDUsrChatDto
import com.exoteric.sharedactor.datasource.dtos.ClockUserDto
import com.exoteric.sharedactor.datasource.dtos.FavsDto
import com.exoteric.sharedactor.datasource.dtos.ClockHistoryDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow

interface SnftrSearchFlower {
    @Throws(Exception::class)
    fun executeSearch(page: Int,
                      query: String?,
                      isNewSearch: Boolean,
                      caller: String):
            SnftrFlow<DataState<List<SnftrDto>>>
}

interface SnftrSearchCacheFlower {
    @Throws(Exception::class)
    fun executeSearch(page: Int,
                      caller: String):
            SnftrFlow<DataState<List<SnftrDto>>>
}

interface ClockThreadsCacheFlower {
    @Throws(Exception::class)
    fun fetchCachedThreadsFromDb(userUid: String):
            SnftrFlow<DataState<List<ClockThreadDto>>>
}

interface ClockThreadsFlower {
    @Throws(Exception::class)
    fun executeThreadsSearch(
        userUid: String,
        isFirstSearch: Boolean,
        threads: MutableList<SnftrIDCThreadEntity>?):
            SnftrFlow<DataState<List<ClockThreadDto>>>
}


interface UsrChatMsgFlower {
    @Throws(Exception::class)
    fun executeUsrChatMsgsSearch(iDmsgs: MutableList<SnftrIDUsrChatEntity>?,
                                   userUid: String,
                                   channelUid: String):
            SnftrFlow<DataState<List<ClockIDUsrChatDto>>>
    @Throws(Exception::class)
    fun executeUsrChatMsgsOnScroll(iDmsgs: MutableList<SnftrIDUsrChatEntity>?,
                                     userUid: String,
                                     lastTime: Long,
                                     channelUid: String):
            SnftrFlow<DataState<List<ClockIDUsrChatDto>>>
}

interface UsrChatMsgCacheFlower {
    @Throws(Exception::class)
    fun fetchCachedUsrChatMessages(userUid: String,
                                     channelUid: String):
            SnftrFlow<DataState<List<ClockIDUsrChatDto>>>
    @Throws(Exception::class)
    fun fetchOlderCachedMessagesBatch(userUid: String,
                                      channelUid: String,
                                      batch: Int,
                                      timestamp: Long):
            SnftrFlow<DataState<List<ClockIDUsrChatDto>>>
}

interface SnftrUserFlower {
    @Throws(Exception::class)
    fun executeUserSearch(user: SnftrUserEntity
    ): SnftrFlow<DataState<List<ClockUserDto>>>

    @Throws(Exception::class)
    fun executeUserFollowingSearch(uid: String): SnftrFlow<DataState<List<ClockUserDto>>>

    @Throws(Exception::class)
    fun executeUserRestoredSearch(users: MutableList<SnftrUserEntity>?,
                                  currentUid: String):
            SnftrFlow<DataState<List<ClockUserDto>>>

    @Throws(Exception::class)
    fun executeNewUsersSearch(users: MutableList<SnftrUserEntity>?,
                              currentUid: String):
            SnftrFlow<DataState<List<ClockUserDto>>>
}


interface SnftrFavsFlower {
    @Throws(Exception::class)
    fun executeFavsSearch(favs: MutableList<FavsEntity>?,
                          uid: String,
                          page: Int,
                          caller: String,
                          isNewSearch: Boolean):
            SnftrFlow<DataState<List<FavsDto>>>
}

interface SnftrFavsCacheFlower {
    @Throws(Exception::class)
    fun fetchCachedFavsFromDb(uid: String,
                              page: Int):
            SnftrFlow<DataState<List<FavsDto>>>
}

interface SnftrChatAttsFlower {
    @Throws(Exception::class)
    fun executeChatAttsSearch(cmmts: MutableList<ClockChatAttsEntity>?,
                              uid: String,
                              currentSnftrUserUid: String?,
                              page: Int,
                              caller: String,
                              isNewSearch: Boolean):
            SnftrFlow<DataState<List<ClockChatAttsDto>>>
}

interface CachedChatAttsFlower {
    @Throws(Exception::class)
    fun fetchCachedChatAttsFromDb(uid: String,
                                  currentSnftrUserUid: String?,
                                  page: Int):
            SnftrFlow<DataState<List<ClockChatAttsDto>>>
}

interface ClockEventHistoryFlower {
    @Throws(Exception::class)
    fun executeClockHistorySearch(histories: MutableList<ClockHistoryEntity>?,
                                  threadUuid: String,
                                  uid: String):
            SnftrFlow<DataState<List<ClockHistoryDto>>>
}

interface CLockEventHistoryCacheFlower {
    @Throws(Exception::class)
    fun fetchCachedClockHistory(
        threadUuid: String,
        uid: String
    ): SnftrFlow<DataState<List<ClockHistoryDto>>>
}
