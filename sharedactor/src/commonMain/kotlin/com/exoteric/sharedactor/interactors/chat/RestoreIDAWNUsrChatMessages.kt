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
import com.exoteric.sharedactor.interactors.flowers.IDAWNUsrChatMsgCacheFlower
import com.exoteric.sharedactor.interactors.parseOriginatorBlob
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.flow.flow

class RestoreClockUsrChatMessages(private val snftrDatabase: SnftrDatabase) : IDAWNUsrChatMsgCacheFlower {
    @Throws(Exception::class)
    override fun fetchCachedIDUsrChatMessages(userUid: String,
                                              channelUid: String):
            SnftrFlow<DataState<List<ClockIDUsrChatDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockChatMessagesQueries
            val cacheResult = queries.getAllIDAWNMessagesForUser(
                userUid = userUid,
                threadUid = channelUid
            ).executeAsList()            // emit List<SnftrIDUsrChatDto> from cache
            // Must manually map this since idawnChatMessage_Entity object is generated by SQL Delight
            val list: ArrayList<ClockIDUsrChatDto> = ArrayList()
            for(entity in cacheResult) {
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
                            flagged = it.flagged
                        )
                    )
                }
            }
            println("$TAG Success! fetchCachedIDUsrChatMessages(): emitting ${list.size}")
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockIDUsrChatDto>>(e.message ?: "Unknown Error - comments null"))
        }
    }.snftrFlow()

    /**
     * This is intended for edge cases, when internet not available: fetch a batch of
     * chat messages from the cache, ordered by timestamp: e.g. the next 5 older messages.
     */
    @Throws(Exception::class)
    override fun fetchOlderCachedMessagesBatch(
        userUid: String,
        channelUid: String,
        batch: Int,
        timestamp: Long
    ): SnftrFlow<DataState<List<ClockIDUsrChatDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockChatMessagesQueries
            val cacheResult = queries.getNextBatchOlder(
                threadUid = channelUid,
                userUid = userUid,
                lastTime = timestamp
            ).executeAsList()            // emit List<SnftrIDUsrChatDto> from cache
            // Must manually map this since idawnChatMessage_Entity object is generated by SQL Delight
            val list: ArrayList<ClockIDUsrChatDto> = ArrayList()
            for(entity in cacheResult) {
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
                            latestProPic = entity.latestProPic,
                            messageData = entity.messageData,
                            scoresBlob = entity.scoresBlob,
                            membersBlob = entity.membersBlob,
                            originatorBlob = entity.originatorBlob,
                            thymestamp = entity.thymestamp,
                            thumbsdownsCount = entity.thumbsdownsCount,
                            thumbsupsCount = entity.thumbsupsCount,
                            // see SnftrCommentDto for details on these fields...
                            isFav = it.fav,
                            thumbsups = it.up,
                            thumbsdowns = it.down,
                            flagged = it.flagged
                        )
                    )
                }
            }
            println("$TAG Success! fetchOlderCachedMessagesBatch(): emitting ${list.size}")
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(DataState.error<List<ClockIDUsrChatDto>>(e.message ?: "Unknown Error - comments null"))
        }
    }.snftrFlow()

    fun mostRecentIDUsrMsgTime(channelUid: String): Long {
        val queries = snftrDatabase.snftrSettingsQueries
        val mostRecentTime = queries
            .getSettingByKey(key = channelUid)
            .executeAsOneOrNull()
        println("$TAG mostRecentIDUsrMsgTime(): ${mostRecentTime?.thymestamp ?: -1}")
        return mostRecentTime?.thymestamp ?: -1
    }

    fun fetchOverCache(channelUid: String,
                       uid: String,
                       latestServerTime: Long): Boolean {
        val latestLocalTime = newestCachedTimestamp(
            channelUid = channelUid,
            uid = uid
        )
        val fetchOverCache = (latestServerTime > latestLocalTime)
        println("$TAG fetchOverCache(server: $latestServerTime " +
                "local: $latestLocalTime) -> $fetchOverCache")
        return fetchOverCache
    }

    /**
     * User is sending a new message; this is added directly to the db from platform chat vm.
     */
    fun addIDUsrMSingle(entity: SnftrIDUsrChatEntity,
                        myCallback: (added: Boolean) -> Unit) {
        val query = snftrDatabase.clockChatMessagesQueries
        if (query.searchIDAWNMessageByMessage(
                userUid = entity.userUid,
                // TODO: more deliberation needed on how best to filter here...
                message = entity.message,
                threadUid = entity.threadUid
            ).executeAsOneOrNull() == null) {
            query.insertIDAWNUsrMessage(
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
            val latestPostQ = if (entity.message.length > 150) {
                entity.message.substring(0, 150)
            } else {
                entity.message
            }
            val check = query.searchIDAWNMessageByMessage(
                userUid = entity.userUid,
                message = latestPostQ,
                threadUid = entity.threadUid
            ).executeAsOneOrNull()
            val wasItAdded = check?.thymestamp == entity.thymestamp
            println("$TAG addIDUsrMSingle(): for entity -- added? $wasItAdded for time: ${check?.thymestamp} and entity ${entity.thymestamp}")
            myCallback(wasItAdded)
        }
    }

    /**
     * User has added or removed a thumbsup or thumbsdown.  Update the db,
     * assert change is valid, reply in callback.
     */
    fun updateIDUsrChatThumbs(userUid: String,
                              chatUid: String,
                              isTUp: Boolean,
                              incremented: Boolean,
                              callback: (updated: Boolean) -> Unit) {
        val query = snftrDatabase.clockChatMessagesQueries
        val cached = query.searchIDAWNMessageByChatUid(
            userUid = userUid,
            chatUid = chatUid
        ).executeAsOneOrNull()
        if (cached != null) {
            // first update the db values
            updateForThumbType(isTUp, incremented, query, chatUid)
            // then check the db to assert update as expected
            checkValid(query, userUid, chatUid, cached, isTUp, incremented, callback)
        }
    }
    // increment or decrement depending on thumb type
    private fun updateForThumbType(
        isTUp: Boolean,
        incremented: Boolean,
        query: ClockChatMessagesQueries,
        chatUid: String
    ) {
        if (isTUp) {
            if (incremented) {
                query.incrChatThumbsups(chatUid = chatUid)
            } else {
                query.decrChattThumbups(chatUid = chatUid)
            }
        } else {
            if (incremented) {
                query.incrChatThumbsdowns(chatUid = chatUid)
            } else {
                query.decrChattThumbsdowns(chatUid = chatUid)
            }
        }
    }
    // assert the incr or decr update happened, send result in callback.
    private fun checkValid(
        query: ClockChatMessagesQueries,
        userUid: String,
        chatUid: String,
        cached: IdawnChatMessage_Entity,
        isTUp: Boolean,
        incremented: Boolean,
        callback: (updated: Boolean) -> Unit
    ) {
        val check = query.searchIDAWNMessageByChatUid(
            userUid = userUid,
            chatUid = chatUid
        ).executeAsOneOrNull()
        // 4 clauses: tUps || not, incr or decr.
        val wasItUpdated = if(isTUp && incremented) check?.thumbsupsCount == (cached.thumbsupsCount + 1)
        else if(isTUp && !incremented) check?.thumbsupsCount == (cached.thumbsupsCount - 1)
        else if(!isTUp && incremented) check?.thumbsdownsCount == (cached.thumbsdownsCount + 1)
        else check?.thumbsdownsCount == (cached.thumbsdownsCount - 1)
        println("$TAG checkValidIDUsrMSingleThumbs(isTup: $isTUp, incr: $incremented): updated? $wasItUpdated")
        callback(wasItUpdated)
    }


    fun newestCachedTimestamp(channelUid: String, uid: String): Long {
        val newest = snftrDatabase.clockChatMessagesQueries
            .getNewest(threadUid = channelUid, uid = uid)
            .executeAsOneOrNull()?.thymestamp ?: -1
        println("$TAG newestCachedTimestamp(uuid): $newest")
        return newest
    }

    fun oldestCachedTimestamp(channelUid: String, uid: String): Long {
        val oldest = snftrDatabase.clockChatMessagesQueries
            .getOldest(threadUid = channelUid, uid = uid)
            .executeAsOneOrNull()?.thymestamp ?: -1
        println("$TAG oldestCachedTimestamp(): $oldest")
        return oldest
    }

    fun getIDUsrChatMsgCount(channelUid: String, uid: String): Long {
        val count = snftrDatabase.clockChatMessagesQueries
            .getAllIDAWNMessagesCountForUser(userUid = uid, threadUid = channelUid)
            .executeAsOneOrNull() ?: 0L
        println("$TAG getIDUsrChatMsgCount() -> $count")
        return count
    }

    fun checkChatExistsByUuid(
        userUid: String,
        message: String,
        threadUid: String): Boolean {
        val query = snftrDatabase.clockChatMessagesQueries
        val check = query.searchIDAWNMessageByMessage(
            userUid = userUid,
            message = message,
            threadUid = threadUid
        ).executeAsOneOrNull()
        val exists = if(check != null) {
            val latestPostQ = if (check.message.length > 150) {
                check.message.substring(0, 150)
            } else {
                check.message
            }
            latestPostQ == message
        } else {
            false
        }
//        println("$TAG checkChatExistsByUuid() -> $exists")
        return exists
    }

    // Expressions for chat messages
    fun insertSnftrChatExpressions(
        isFav: Long,
        tUp: Long,
        tDown: Long,
        flagged: Long,
        chatUuid: String,
        userUid: String,
        myCallback: (updated: Boolean) -> Unit) {
        val query = snftrDatabase.snftrUserExpressionsQueries
        val entity = query.getCmmtUserExpressionsForUserUid(
            cmmtUuid = chatUuid,
            userUid = userUid
        ).executeAsOneOrNull()
        if (entity != null) {
            println("insertSnftrCmmtExpressions():" +
                    " this expressions row already exists for ($chatUuid) & user -> $userUid! ----> d: ${entity.thumbsdowns} u: ${entity.thumbsups}")
            query.updateCmmtThumbsUPForUserUid(tUp, chatUuid, userUid)
            query.updateCmmtThumbsDOWNForUserUid(tUp, chatUuid, userUid)
            myCallback(false)
            return
        }
        println("insertSnftrChatExpressions($chatUuid): inserting for -> $userUid")
        query.insertUserExpressions(
            id = getId(),
            uuid = chatUuid,
            userUid = userUid,
            isFav = isFav,
            thumbsups = tUp,
            thumbsdowns = tDown,
            flagged = flagged
        )
        val updatedExpr = query
            .getCmmtUserExpressionsForUserUid(chatUuid, userUid)
            .executeAsOneOrNull()
        myCallback(updatedExpr != null && updatedExpr.isFav == isFav && updatedExpr.thumbsups == tUp && updatedExpr.thumbsdowns == tDown)
    }

    fun updateSnftrChatSingleForThumbsups(thumbsups: Long,
                                          chatUuid: String,
                                          userUid: String,
                                          myCallback: (updated: Boolean) -> Unit) {
        // ** NOTE: not incrementing the comment thumbsups for v1
        //          only the userExpressions row
        val queryExpressions = snftrDatabase.snftrUserExpressionsQueries
        val expr = queryExpressions.getCmmtUserExpressionsForUserUid(chatUuid, userUid).executeAsOneOrNull()
        if (expr != null) {
            println("updateSnftrChatSingleForThumbsups(chatUuid): updating with -> $thumbsups!")
            queryExpressions.updateCmmtThumbsUPForUserUid(thumbsups, chatUuid, userUid)
        } else {
            println("updateSnftrChatSingleForThumbsups(chatUuid): inserting with $thumbsups!")
            queryExpressions.insertUserExpressions(
                id = getId(),
                uuid = chatUuid,
                userUid = userUid,
                isFav = 0,
                thumbsups = thumbsups,
                thumbsdowns = 0,
                flagged = 0
            )
        }
        val updatedExpr = queryExpressions.getCmmtUserExpressionsForUserUid(chatUuid, userUid).executeAsOneOrNull()
        myCallback(updatedExpr != null && updatedExpr.thumbsups == thumbsups)
    }

    fun updateSnftrChatSingleForThumbsdowns(thumbsdowns: Long,
                                            chatUuid: String,
                                            userUid: String,
                                            myCallback: (updated: Boolean) -> Unit) {
        val queryComments = snftrDatabase.clockChatMessagesQueries
        if(queryComments.getChatCommentByUuid(chatUid = chatUuid).executeAsOneOrNull() == null) {
            println("updateSnftrChatSingleForThumbsdowns(chatUuid): " +
                    "uh oh the comment is not in the db!")
            return
        }
        if (thumbsdowns == 1L) {
            println("updateSnftrChatSingleForThumbsdowns(chatUuid): " +
                    "incrCommentThumbsdowns")
            queryComments.incrChatThumbsdowns(chatUid = chatUuid)
        } else if (thumbsdowns == 0L) {
            println("updateSnftrChatSingleForThumbsdowns(chatUuid): " +
                    "decrCommentThumbsdowns")
            queryComments.incrChatThumbsdowns(chatUid = chatUuid)
        }
        val query = snftrDatabase.snftrUserExpressionsQueries
        val expr = query
            .getCmmtUserExpressionsForUserUid(chatUuid, userUid)
            .executeAsOneOrNull()
        if (expr != null) {
            println("updateSnftrChatSingleForThumbsdowns(chatUuid):" +
                    " updating with -> $thumbsdowns!")
            query.updateCmmtThumbsDOWNForUserUid(thumbsdowns, chatUuid, userUid)
        } else {
            println("updateSnftrChatSingleForThumbsdowns(chatUuid):" +
                    " inserting with $thumbsdowns!")
            query.insertUserExpressions(
                id = getId(),
                uuid = chatUuid,
                userUid = userUid,
                isFav = 0,
                thumbsups = 0,
                thumbsdowns = thumbsdowns,
                flagged = 0
            )
        }

        val updatedExpr = query.getCmmtUserExpressionsForUserUid(chatUuid, userUid).executeAsOneOrNull()
        myCallback(updatedExpr != null && updatedExpr.thumbsdowns == thumbsdowns)
    }

    fun updateSnftrChatSingleForFlag(flagged: Long,
                                     chatUuid: String,
                                     userUid: String,
                                     myCallback: (updated: Boolean) -> Unit) {
        val queryComments = snftrDatabase.clockChatMessagesQueries
        if(queryComments
                .getChatCommentByUuid(chatUid = chatUuid)
                .executeAsOneOrNull() == null) {
            println("updateSnftrChatSingleForFlag(chatUuid): " +
                    "uh oh the comment is not in the db!")
            return
        }
        val query = snftrDatabase.snftrUserExpressionsQueries
        val expr = query
            .getCmmtUserExpressionsForUserUid(chatUuid, userUid)
            .executeAsOneOrNull()
        if (expr != null) {
            println("updateSnftrChatSingleForFlag(chatUuid):" +
                    " updating with -> $flagged!")
            query.updateCmmtFlaggedForUserUid(flagged, chatUuid, userUid)
        } else {
            println("updateSnftrChatSingleForFlag(chatUuid):" +
                    " inserting with $flagged!")
            query.insertUserExpressions(
                id = getId(),
                uuid = chatUuid,
                userUid = userUid,
                isFav = 0,
                thumbsups = 0,
                thumbsdowns = 0,
                flagged = flagged
            )
        }
        val updatedExpr = query.getCmmtUserExpressionsForUserUid(chatUuid, userUid).executeAsOneOrNull()
        myCallback(updatedExpr != null && updatedExpr.flagged == flagged)
    }

    fun updateSnftrChatSingleFAV(isFav: Long,
                                 chatUuid: String,
                                 userUid: String,
                                 myCallback: (updated: Boolean) -> Unit) {
        val query = snftrDatabase.snftrUserExpressionsQueries
        val expr = query.getCmmtUserExpressionsForUserUid(chatUuid, userUid).executeAsOneOrNull()
        if (expr != null) {
            println("updateSnftrChatSingleFAV(chatUuid): updating -> $isFav!")
            query.updateCmmtFAVForUserUid(isFav = isFav, chatUuid, userUid)
        } else {
            println("updateSnftrChatSingleFAV(chatUuid): inserting -> $isFav!")
            query.insertUserExpressions(
                id = getId(),
                uuid = chatUuid,
                userUid = userUid,
                isFav = isFav,
                thumbsups = 0,
                thumbsdowns = 0,
                flagged = 0
            )
        }
        val updatedExpr = query.getCmmtUserExpressionsForUserUid(chatUuid, userUid).executeAsOneOrNull()
        myCallback(updatedExpr != null && updatedExpr.isFav == isFav)
    }

    companion object {
        const val TAG = "RestoreUsrChatMsgs"
    }
}