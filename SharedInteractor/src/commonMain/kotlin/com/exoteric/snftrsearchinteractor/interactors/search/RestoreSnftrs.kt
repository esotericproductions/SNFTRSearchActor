package com.exoteric.snftrsearchinteractor.interactors.search

import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchinteractor.domain.data.DataState
import com.exoteric.snftrsearchinteractor.domain.util.SnftrFlow
import com.exoteric.snftrsearchinteractor.domain.util.snftrFlow
import com.exoteric.snftrsearchinteractor.interactors.dtos.SnftrDto
import com.exoteric.snftrsearchinteractor.interactors.flowers.SnftrSearchCacheFlower
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * Restore a [SnftrDto] from SnftrDb.
 */
class RestoreSnftrs(private val snftrDatabase: SnftrDatabase) : SnftrSearchCacheFlower {

    @Throws(Exception::class)
    override fun executeSearch(page: Int, caller: String): SnftrFlow<DataState<List<SnftrDto>>> = flow {
        try {
            emit(DataState.loading())
            // only delay for iOS for pagination,
            // it causes problems on Android,
            // but it allows for proper paging UX in iOS
            delay(500)
            // query the cache
            val queries = snftrDatabase.snftrSearchQueries
            val cacheResult = queries.getAllByPage(
                page = page.toLong(),
                caller = caller
            ).executeAsList()

            println("$TAG --> ${cacheResult.size}")
            // emit List<SnftrDto> from cache
            // Must manually map this since pyp_Entity object is generated by SQL Delight
            val list: ArrayList<SnftrDto> = ArrayList()
            for(entity in cacheResult) {
                list.add(
                    SnftrDto(
                        id = entity.id.toInt(),
                        title = entity.title,
                        provider = entity.provider,
                        creator = entity.creator,
                        creatorProfilePic = entity.creatorProfilePic,
                        sourceUrlDetail = entity.source_url_detail,
                        sourceUrlThumb = entity.source_url_thumb,
                        sourceUrlOwner = entity.source_url_owner,
                        posterUid = "",
                        currentQuery = entity.current_query,
                        randomSeed = entity.random_seed.toInt(),
                        thymestamp = entity.thymeStamp,
                        latestProfilePic = "",
                        page = entity.page.toInt(),
                        caller = entity.caller
                    )
                )
            }
            emit(DataState.success(list))
        } catch (e: Exception){ emit(
            DataState.error<List<SnftrDto>>(
            e.message?: "$TAG: Unknown Error")) }
    }.snftrFlow()

    fun setCurrentQuery(qry: String, caller: String)  {
        val queries = snftrDatabase.snftrSettingsQueries
        val mostRecentPg = queries
            .getSettingByKey(key = MOST_RECENT_QRY + caller)
            .executeAsOneOrNull()
        if (mostRecentPg == null) {
            println("$TAG setCurrentQuery(qry: $qry): insertSettings")
            queries.insertSettings(
                category = CAT_PG_SEARCH + caller,
                key = MOST_RECENT_QRY + caller,
                value_ = qry,
                thymestamp = 0
            )
        } else {
            println("$TAG setCurrentQuery(qry: $qry): updateSettings")
            queries.updateSettings(
                key = MOST_RECENT_QRY + caller,
                value_ = qry
            )
        }
    }

    fun getCurrentQuery(caller: String): String {
        val queries = snftrDatabase.snftrSettingsQueries
        val mostRecentQry = queries
            .getSettingByKey(key = MOST_RECENT_QRY + caller)
            .executeAsOneOrNull()
        val qry = mostRecentQry?.value_ ?: ""
        println("$TAG getCurrentQuery() -> $qry")
        return qry
    }

    fun hasCached(page: Int, caller: String): Boolean {
        val queries = snftrDatabase.snftrSearchQueries
        val numberOfCached = queries
            .getAllCountForPage(
                page = page.toLong(),
                caller = caller
            )
            .executeAsOne()
        println("$TAG hasCached(page: $page): $numberOfCached")
        return numberOfCached > 0
    }

    fun mostRecentPgNumber(caller: String): Int? {
        val queries = snftrDatabase.snftrSettingsQueries
        val mostRecentPg = queries
            .getSettingByKey(key = MOST_RECENT_PG_SEARCH + caller)
            .executeAsOneOrNull()
        println("$TAG pageNumber(): ${mostRecentPg?.value_ ?: 1}")
        return mostRecentPg?.value_?.toInt()
    }

    /**
     * used in footer
     */
    fun totalPagesForQuery(caller: String): Int {
        val pages = snftrDatabase.snftrSearchQueries
            .totalPagesForQuery(caller = caller)
            .executeAsOneOrNull()?.page?.toInt() ?: 1
        println("$TAG totalPagesForQuery(): $pages")
        return pages
    }

    fun setCurrentPgNumber(page: Int, caller: String) {
        val queries = snftrDatabase.snftrSettingsQueries
        val mostRecentPg = queries
            .getSettingByKey(key = MOST_RECENT_PG_SEARCH + caller)
            .executeAsOneOrNull()
        if (mostRecentPg == null) {
            println("$TAG setCurrentPgNumber():" +
                    " mostRecentPg == null, current: $page")
            queries.insertSettings(
                category = CAT_PG_SEARCH + caller,
                key = MOST_RECENT_PG_SEARCH + caller,
                value_ = page.toString(),
                thymestamp = 0
            )
        } else if (mostRecentPg.value_ != page.toString()){
            println("$TAG setCurrentPgNumber():" +
                    " stale: ${mostRecentPg.value_}, current: $page")
            queries.updateSettings(
                key = MOST_RECENT_PG_SEARCH + caller,
                value_ = page.toString())
        }
    }

    companion object {
        const val TAG = "RestoreSnftrs"
        const val MOST_RECENT_PG_SEARCH = "most_recent_pg"
        const val CAT_PG_SEARCH = "category_page"
        const val MOST_RECENT_QRY = "most_recent_query"
    }
}

