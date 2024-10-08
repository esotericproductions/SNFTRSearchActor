package com.exoteric.sharedactor.interactors.chat

import com.exoteric.pypnft.cached.ClockChatMessagesQueries
import com.exoteric.pypnft.cached.IdawnChatMessage_Entity
import com.exoteric.sharedactor.datasource.cached.models.SnftrIDUsrChatEntity
import com.exoteric.sharedactor.datasource.dtos.ClockIDUsrChatDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.chat.threads.createOriginatorBlob
import com.exoteric.sharedactor.interactors.chat.threads.getCachedUserData
import com.exoteric.sharedactor.interactors.expressions.getUserExpressionsForSnftrDto
import com.exoteric.sharedactor.interactors.flowers.UsrChatMsgFlower
import com.exoteric.sharedactor.interactors.parseOriginatorBlob
import com.exoteric.sharedactor.interactors.user.parseProfilesBlobBlocked
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.flow.flow

class AddClockUserChatMessages(private val snftrDatabase: SnftrDatabase) : UsrChatMsgFlower {
    /**
     * Insert new incoming messages from the network.
     */
    override fun executeUsrChatMsgsSearch(
        iDmsgs: MutableList<SnftrIDUsrChatEntity>?,
        userUid: String,
        channelUid: String): SnftrFlow<DataState<List<ClockIDUsrChatDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockChatMessagesQueries
            // 0. get current cache
            val allCachedIDmsgs = queries.getAllIDAWNMessagesForUser(
                userUid = userUid,
                threadUid = channelUid
            ).executeAsList()
            // 1. filter msgs of any duplicates before putting into cache
            val filteredIDinChatMsgs =
                iDmsgs?.distinct()?.filter { fp ->
                    fp.chatUid !in allCachedIDmsgs.map { it.chatUid }
                            && fp.thymestamp !in allCachedIDmsgs.map { it.thymestamp }
                            && fp.thymestamp > 0
                }
            println("$TAG executeIDUsrChatMsgsSearch(): existing: ${allCachedIDmsgs.size} " +
                    "--- adding: ${filteredIDinChatMsgs?.size}")
            // 2. insert the filtered/distinct msgs
            insertFilteredArray(filteredIDinChatMsgs, queries)
            // 3. fetch from cache
            val cacheResult = queries.getAllIDAWNMessagesForUser(
                    userUid = userUid,
                    threadUid = channelUid
            ).executeAsList()
            // 4. map and decorate the cached entities
            val list: ArrayList<ClockIDUsrChatDto> = mapAndDecorateChatDtos(cacheResult, userUid)
            println("$TAG Success! executeIDinChatMsgsSearch(): ${list.size}")
            // 5. emit List<SnftrIDUsrChatDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockIDUsrChatDto>>(e.message ?: "$TAG Unknown Error - message null"))
        }
    }.snftrFlow()

    /**
     * Used via scroll listeners, when fetching older messages at top of chat
     * conversation, e.g. user scrolls to top of currently visible messages, the next batch
     * of older messages is loaded.  @see #fetchOlderCachedMessagesBatch for offline version.
     */
    override fun executeUsrChatMsgsOnScroll(
        iDmsgs: MutableList<SnftrIDUsrChatEntity>?,
        userUid: String,
        lastTime: Long,
        channelUid: String): SnftrFlow<DataState<List<ClockIDUsrChatDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockChatMessagesQueries
            // 0. get current cached batch size, timestamp descending from lastTime value
            val allCachedIDmsgs = queries.getNextBatchOlder(
                userUid = userUid,
                threadUid = channelUid,
                lastTime = lastTime
            ).executeAsList()
            // 1. filter iDmsgs of any duplicates before putting into cache
            val filteredIDinChatMsgs =
                iDmsgs?.distinct()?.filter { fp ->
                    fp.chatUid !in allCachedIDmsgs.map { it.chatUid }
                            && fp.thymestamp !in allCachedIDmsgs.map { it.thymestamp }
                            && fp.thymestamp > 0
                }
            println("$TAG executeIDUsrChatMsgsOnScroll(): existing: ${allCachedIDmsgs.size} " +
                    "--- adding: ${filteredIDinChatMsgs?.size} after lastTime: $lastTime")
            // two paths here:
            //  1. UPDATE thumbs of any rows already in db
            //  2. INSERT any new rows (i.e. filteredIDinChatMsgs conditions)
            if(!filteredIDinChatMsgs.isNullOrEmpty() && filteredIDinChatMsgs.size < 5) {
                val msgsGettingUpdates =
                    iDmsgs.distinct().filter { um ->
                        um.chatUid !in filteredIDinChatMsgs.map { it.chatUid }
                                && um.thymestamp !in filteredIDinChatMsgs.map { it.thymestamp }
                                && um.thymestamp > 0
                    }
                // update what's already cached
                updateChachedThumbs(msgsGettingUpdates, queries)
                // insert new rows
                insertFilteredArray(filteredIDinChatMsgs, queries)
            } else if(filteredIDinChatMsgs.isNullOrEmpty() && !iDmsgs.isNullOrEmpty()) {
                updateChachedThumbs(iDmsgs.distinct(), queries)
            } else {
                // 2. insert new rows
                insertFilteredArray(filteredIDinChatMsgs, queries)
            }
            // 3. fetch the time-descending batch, (e.g. "next 5") from cache
            val cacheResult = queries.getNextBatchOlder(
                userUid = userUid,
                threadUid = channelUid,
                lastTime = lastTime
            ).executeAsList()
            // 4. map and decorate the cached entities
            val list: ArrayList<ClockIDUsrChatDto> = mapAndDecorateChatDtos(cacheResult, userUid)
            println("$TAG Success! executeIDUsrChatMsgsOnScroll(): ${list.size}")
            // 5. emit List<SnftrIDUsrChatDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockIDUsrChatDto>>(e.message ?: "$TAG Unknown Error - message null"))
        }
    }.snftrFlow()

    /**
     * 1. Map the IdawnChatMessage_Entity items (straight from the db) to SnftrIDUsrChatDto,
     * 2. getUserExpressionsForSnftrDto to decorate expression fields: isFav, thumbs, flag.
     *
     * The returned ArrayList<SnftrIDUsrChatDto> is ready to emit (platform VM's).
     */
    private fun mapAndDecorateChatDtos(
        cacheResult: List<IdawnChatMessage_Entity>,
        userUid: String
    ): ArrayList<ClockIDUsrChatDto> {
        println("$TAG mapAndDecorateChatDtos.cacheResult size: ${cacheResult.size}")
        // map here since idawnChatMessage_Entity is generated
        val list: ArrayList<ClockIDUsrChatDto> = ArrayList()
        for (entity in cacheResult) {
            // is msg posterUid blocked?
            val user = snftrDatabase.snftrUsersQueries.searchUsersByUid(userUid).executeAsOneOrNull()
            val blocked = if(user == null) false else parseProfilesBlobBlocked(user.profilesBlob)?.map { it.uid }?.contains(entity.posterUid) ?: false
            // update user if in db
            val blob = parseOriginatorBlob(entity.originatorBlob)
            val latestUserData =
                getCachedUserData(
                    uid = blob.uid,
                    entityProPic = entity.latestProPic,
                    entityName = blob.name,
                    entityUsername = blob.username,
                    snftrDatabase
                )
            val newBlob = createOriginatorBlob(
                name = latestUserData.name,
                username = latestUserData.username,
                uid = blob.uid
            )
            getUserExpressionsForSnftrDto(
                uuid = entity.chatUid,
                userUid = userUid,
                snftrDatabase = snftrDatabase
            ) {
                list.add(
                    ClockIDUsrChatDto(
                        userUid = entity.userUid,
                        posterUid = entity.posterUid,
                        chatUid = entity.chatUid,
                        type = entity.type,
                        threadUid = entity.threadUid,
                        message = entity.message,
                        latestProPic = latestUserData.proPic,
                        messageData = entity.messageData,
                        scoresBlob = entity.scoresBlob,
                        membersBlob = entity.membersBlob,
                        originatorBlob = newBlob,
                        thymestamp = entity.thymestamp,
                        thumbsdownsCount = entity.thumbsdownsCount,
                        thumbsupsCount = entity.thumbsupsCount,
                        // see SnftrCommentDto for details on these fields...
                        isFav = it.fav,
                        thumbsups = it.up,
                        thumbsdowns = it.down,
                        flagged = it.flagged,
                        blocked = blocked
                    )
                )
            }
        }
        return list
    }

    /**
     * Simple insert fun.
     * If not null or empty, insert SnftrIDUsrChatEntity list elements into IDAWNChatMessagesQueries.
     */
    private fun insertFilteredArray(
        filteredIDinChatMsgs: List<SnftrIDUsrChatEntity>?,
        queries: ClockChatMessagesQueries
    ) {
        if (filteredIDinChatMsgs != null) {
            println("$TAG insertFilteredArray: ${filteredIDinChatMsgs.size}")
            for (entity in filteredIDinChatMsgs) {

//                val blob = parseOriginatorBlob(entity.originatorBlob)
//                if (userIsInDb(blob.uid)) {
//                    val userQueries = snftrDatabase.snftrUsersQueries
//                    userQueries.updateUserForContentUpdate(
//                        name = blob.name,
//                        handle = blob.username,
//                        profilePic = entity.latestProPic,
//                        uid = blob.uid
//                    )
//                }

                queries.insertIDAWNUsrMessage(
                    id = getId(),
                    userUid = entity.userUid,
                    posterUid = entity.posterUid,
                    chatUid = entity.chatUid,
                    type = entity.type,
                    latestProPic = entity.latestProPic,
                    threadUid = entity.threadUid,
                    message = entity.message,
                    messageData = entity.messageData,
                    scoresBlob = entity.scoresBlob,
                    membersBlob = entity.membersBlob,
                    originatorBlob = entity.originatorBlob,
                    thymestamp = entity.thymestamp,
                    thumbsdownsCount = entity.thumbsdownsCount,
                    thumbsupsCount = entity.thumbsupsCount
                )
            }
        }
    }

    /**
     * Simple update fun.
     * If not null or empty, update SnftrIDUsrChatEntity list elements' expressions in IDAWNChatMessagesQueries.
     */
    private fun updateChachedThumbs(
        unfiltered: List<SnftrIDUsrChatEntity>,
        queries: ClockChatMessagesQueries
    ) {
        println("$TAG updateChachedThumbs: ${unfiltered.size}")
        for (entity in unfiltered) {
            queries.updateThumbsByChatUid(
                chatUid = entity.chatUid,
                thumbsdownsCount = entity.thumbsdownsCount,
                thumbsupsCount = entity.thumbsupsCount
            )
        }
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
        const val TAG = "AddIDinChatMsgs"
    }
}