package com.exoteric.sharedactor.interactors.flowers

import com.exoteric.sharedactor.datasource.cached.models.SnftrIDCThreadEntity
import com.exoteric.sharedactor.datasource.cached.models.SnftrIDUsrChatEntity
import com.exoteric.sharedactor.datasource.cached.models.SnftrUserEntity
import com.exoteric.sharedactor.datasource.dtos.ClockThreadDto
import com.exoteric.sharedactor.datasource.dtos.SnftrDto
import com.exoteric.sharedactor.datasource.dtos.SnftrIDUsrChatDto
import com.exoteric.sharedactor.datasource.dtos.ClockUserDto
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
        threads: MutableList<SnftrIDCThreadEntity>?):
            SnftrFlow<DataState<List<ClockThreadDto>>>
}


interface IDAWNUsrChatMsgFlower {
    @Throws(Exception::class)
    fun executeIDUsrChatMsgsSearch(iDmsgs: MutableList<SnftrIDUsrChatEntity>?,
                                   userUid: String,
                                   channelUid: String):
            SnftrFlow<DataState<List<SnftrIDUsrChatDto>>>
    @Throws(Exception::class)
    fun executeIDUsrChatMsgsOnScroll(iDmsgs: MutableList<SnftrIDUsrChatEntity>?,
                                     userUid: String,
                                     lastTime: Long,
                                     channelUid: String):
            SnftrFlow<DataState<List<SnftrIDUsrChatDto>>>
}

interface IDAWNUsrChatMsgCacheFlower {
    @Throws(Exception::class)
    fun fetchCachedIDUsrChatMessages(userUid: String,
                                     channelUid: String):
            SnftrFlow<DataState<List<SnftrIDUsrChatDto>>>
    @Throws(Exception::class)
    fun fetchOlderCachedMessagesBatch(userUid: String,
                                      channelUid: String,
                                      batch: Int,
                                      timestamp: Long):
            SnftrFlow<DataState<List<SnftrIDUsrChatDto>>>
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
}