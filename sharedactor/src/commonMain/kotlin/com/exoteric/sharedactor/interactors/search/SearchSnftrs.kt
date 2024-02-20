package com.exoteric.sharedactor.interactors.search

import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.sharedactor.datasource.cached.models.SnftrEntity
import com.exoteric.sharedactor.datasource.cached.models.SnftrEntityMapper
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.dtos.SnftrDto
import com.exoteric.sharedactor.interactors.dtos.mappers.SnftrDtoMapper
import com.exoteric.sharedactor.interactors.flowers.SnftrSearchFlower
import com.exoteric.snftrsearchlibr.bingGifsTag
import com.exoteric.snftrsearchlibr.bingTag
import com.exoteric.snftrsearchlibr.datasource.network.SnftrService
import com.exoteric.snftrsearchlibr.datasource.network.SnftrServiceImpl
import com.exoteric.snftrsearchlibr.getId
import com.exoteric.snftrsearchlibr.giphySTag
import com.exoteric.snftrsearchlibr.giphyTag
import com.exoteric.snftrsearchlibr.pexelsTag
import com.exoteric.snftrsearchlibr.pixabayTag
import com.exoteric.snftrsearchlibr.providers
import com.exoteric.snftrsearchlibr.tenorTag
import com.exoteric.snftrsearchlibr.unsplashTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow

@ExperimentalCoroutinesApi
class SearchSnftrs(
    private val snftrService: SnftrService = SnftrServiceImpl(),
    private val dtoMapper: SnftrDtoMapper = SnftrDtoMapper(),
    private val snftrDatabase: SnftrDatabase,
    private val snftrEntityMapper: SnftrEntityMapper = SnftrEntityMapper(),
    ) : SnftrSearchFlower {

    @Throws(Exception::class)
    override fun executeSearch(
        page: Int,
        query: String?,
        isNewSearch: Boolean,
        caller: String
    ): SnftrFlow<DataState<List<SnftrDto>>> = channelFlow<DataState<List<SnftrDto>>>  {
        try {
            if (isNewSearch) {
                snftrDatabase.snftrSearchQueries.deletePyps(caller = caller)
                snftrDatabase.snftrSettingsQueries.deleteAllSettingsByTab(category = RestoreSnftrs.CAT_PG_SEARCH + caller)
            }
            send(DataState.loading())
            // providers elements are snftr source api's
            coroutineScope {
                providers.map { provider ->
                    async(Dispatchers.IO) {
                        try {
                            getItemsByProvider(
                                page = page,
                                provider = provider,
                                query = query,
                                caller = caller,
                                flowCollector = this@channelFlow
                            )
                        } catch (e: Exception) {
                            // Handle exceptions per API call
                            send(DataState.error<List<SnftrDto>>(
                                e.message ?: "Error fetching from $provider")
                            )
                        }
                    }
                }.awaitAll() // Wait for all async operations to complete
            }
        } catch (e: Exception) {
            send(DataState.error<List<SnftrDto>>(
                e.message ?: "Unknown Error - message null")
            )
        }
    }.snftrFlow()

    private suspend fun getItemsByProvider(
        page: Int,
        provider: String,
        query: String?,
        caller: String,
        flowCollector: ProducerScope<DataState<List<SnftrDto>>>
    ) {
        // insert into cache, then emit from cache to VM's
        insertItemsIntoCache(page, provider, query, caller, flowCollector)
        emitImgsFromCache(page, provider, caller, flowCollector)
    }

    private suspend fun insertItemsIntoCache(
        page: Int,
        provider: String,
        query: String?,
        caller: String,
        flowCollector: ProducerScope<DataState<List<SnftrDto>>>
    ) {
        val imgs = when (provider) {
            unsplashTag -> getImgsFromUnsplash(page, query, caller)
            pexelsTag -> getImgsFromPexels(page, query, caller)
            giphyTag -> getGifsFromGiphy(page, query, caller)
            giphySTag -> getGiphyStickers(page, query, caller)
            tenorTag -> getGifsFromTenor(page, query, caller)
            pixabayTag -> getImgsFromPixabay(page, query, caller)
            bingTag -> getImgsFromBing(page, query, caller)
            bingGifsTag -> getGifsFromBing(page, query, caller)
            else -> {
                println("$TAG insertItemsIntoCache().emptyList(): $provider")
                emptyList<SnftrDto>()
            }
        }
        //filter out any duplicate urls before putting into cache
        val allCachedItems = snftrDatabase
            .snftrSearchQueries
            .getAllByPage(
                page = page.toLong(),
                caller = caller
            ).executeAsList()
        val filteredImgs =
            imgs?.filter { fp -> fp.sourceUrlThumb !in allCachedItems.map { it.source_url_thumb } }
        println("$TAG insertItemsIntoCache($provider -> $caller): existing: " +
                "${allCachedItems.size} --- adding: ${filteredImgs?.size}")
        // if empty just send empty dataState
        if (filteredImgs?.size == 0) {
            flowCollector.send(DataState.empty(provider = provider))
        } else {
            // else proceed with inserting into cache
            val entities = snftrEntityMapper.toEntityList(filteredImgs)
            if (entities != null) {
                for (entity in entities) {
                    snftrDatabase.snftrSearchQueries.insertSrch(
                        id = entity.id,
                        title = entity.title,
                        creator = entity.creator,
                        creatorProfilePic = entity.creatorProfilePic,
                        provider = entity.provider,
                        source_url_detail = entity.sourceUrl_detail.trim(),
                        source_url_thumb = entity.sourceUrl_thumb.trim(),
                        source_url_owner = entity.sourceUrl_owner.trim(),
                        current_query = entity.current_query.trim(),
                        // random_seed range is used for ternary logic in grid anims
                        random_seed = (1..3201).random().toLong(),
                        thymeStamp = entity.thymeStamp,
                        page = entity.page.toLong(),
                        caller = entity.caller ?: ""
                    )
                }
            }
        }
    }
    private suspend fun emitImgsFromCache(
        page: Int,
        provider: String,
        caller: String,
        flowCollector: ProducerScope<DataState<List<SnftrDto>>>
    ) {
        // query the cache
        val cacheResult = snftrDatabase.snftrSearchQueries
            .getSrchByType(
                provider = provider,
                page = page.toLong(),
                caller = caller)
            .executeAsList()
        // map here since Pyp_Entity is generated by SQLDelight
        val list = arrayListOf<SnftrDto>()
        for (entity in cacheResult) {
            list.add(
                SnftrDto(
                    id = entity.id.toInt(),
                    title = entity.title,
                    creator = entity.creator,
                    creatorProfilePic = entity.creatorProfilePic,
                    provider = entity.provider,
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
        println("$TAG success: emitImgsFromCache($provider) --> size: ${list.size}")
        // emit List<SnftrDto> from cache
        flowCollector.send(DataState.success(list))
    }

    // provider funs calling snftrService and returning dtoMapper.toDomainList(mapped)
    private suspend fun getGifsFromGiphy(
        page: Int,
        query: String?,
        caller: String
    ): List<SnftrDto>? {
        val mapped = snftrService.searchGiphy(page = page, query = query)?.data?.map {
            SnftrEntity(
                id = getId(),
                title = it.title,
                creator = it.user.username,
                creatorProfilePic = it.user.avatarUrl,
                provider = giphyTag,
                sourceUrl_detail = it.images.fixedHeight.url,
                sourceUrl_thumb = it.images.downsized.url,
                // ideally show the source, if empty, just provide the giphy web link
                sourceUrl_owner = it.sourcePostUrl.ifEmpty { it.url },
                current_query = query ?: "",
                thymeStamp = 0,
                page = page,
                caller = caller
            )
        }
        println("$TAG getGifsFromGiphy(query = $query, page: $page): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    // provider funs calling snftrService and returning dtoMapper.toDomainList(mapped)
    private suspend fun getGiphyStickers(
        page: Int,
        query: String?,
        caller: String
    ): List<SnftrDto>? {
        val mapped = snftrService
            .searchGiphyStickers(page = page, query = query)
            ?.data
            ?.map {
                SnftrEntity(
                    id = getId(),
                    title = it.title,
                    creator = it.user.username,
                    creatorProfilePic = it.user.avatarUrl,
                    provider = giphySTag,
                    sourceUrl_detail = it.images.fixedHeight.url,
                    sourceUrl_thumb = it.images.downsized.url,
                    sourceUrl_owner = it.sourcePostUrl.ifEmpty { it.url },
                    current_query = it.title,
                    thymeStamp = 0,
                    page = page,
                    caller = caller
                )
            }
        println("$TAG getGiphyStickers(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    private suspend fun getGifsFromTenor(page: Int,
                                         query: String?,
                                         caller: String): List<SnftrDto>? {

        val mapped = snftrService.searchTenor(page = page, query = query)?.results?.map { it ->
            SnftrEntity(
                id = getId(),
                title = it.contentDescription,
                creator = it.title,
                creatorProfilePic = it.contentDescription,
                provider = tenorTag,
                sourceUrl_detail = it.media[0].gif.url , //TODO this may not be ideal
                sourceUrl_thumb = it.media[0].nanogif.url,
                sourceUrl_owner = it.url,
                current_query = query ?: "",
                thymeStamp = 0,
                page = page,
                caller = caller
            )
        }
        println("$TAG getGifsFromTenor(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    private suspend fun getImgsFromPixabay(page: Int,
                                           query: String?,
                                           caller: String): List<SnftrDto>? {

        val mapped = snftrService.searchPixabay(page = page, query = query)?.hits?.map {
            SnftrEntity(
                id = getId(),
                title = pixabayTag,
                creator = it.user,
                creatorProfilePic = it.userImageURL,
                provider = pixabayTag,
                sourceUrl_detail = it.webformatURL, // TODO needs fallback to preview after ? time
                sourceUrl_thumb = it.previewURL,
                sourceUrl_owner = it.pageURL,
                current_query = query ?: "",
                thymeStamp = 0,
                page = page,
                caller = caller
            )
        }
        println("$TAG getImgsFromPixabay(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    private suspend fun getImgsFromBing(page: Int,
                                        query: String?,
                                        caller: String): List<SnftrDto>? {
        val mapped = snftrService.searchBing(page = page, query = query)?.value?.map {
            SnftrEntity(
                id = getId(),
                title = it.name,
                creator = "",
                creatorProfilePic = "",
                provider = bingTag,
                sourceUrl_detail = it.thumbnailUrl, // TODO need to provide access to the full res pic
                sourceUrl_thumb = it.thumbnailUrl,
                sourceUrl_owner = it.hostPageUrl,
                current_query = query ?: "",
                thymeStamp = 0,
                page = page,
                caller = caller
            )
        }
        println("$TAG getImgsFromBing(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    private suspend fun getGifsFromBing(page: Int,
                                        query: String?,
                                        caller: String): List<SnftrDto>? {
        val mapped = snftrService.searchBingGifs(page = page, query = query)?.value?.map {
            SnftrEntity(
                id = getId(),
                title = it.name,
                creator = "",
                creatorProfilePic = "",
                provider = bingGifsTag,
                sourceUrl_detail = it.contentUrl, // TODO need to provide access to the full res pic
                sourceUrl_thumb = it.thumbnailUrl,
                sourceUrl_owner = it.hostPageUrl,
                current_query = query ?: "",
                thymeStamp = 0,
                page = page,
                caller = caller
            )
        }
        println("$TAG getGifsFromBing(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    private suspend fun getImgsFromPexels(page: Int,
                                          query: String?,
                                          caller: String): List<SnftrDto>? {
        val mapped = snftrService.searchPexels(page = page, query = query)?.photos?.map {
                SnftrEntity(
                    id = getId(),
                    title = pexelsTag,
                    creator = it.photographer,
                    creatorProfilePic = it.photographerUrl,
                    provider = pexelsTag,
                    sourceUrl_detail = it.src.large,
                    sourceUrl_thumb = it.src.tiny,
                    sourceUrl_owner = it.photographerUrl,
                    current_query = query ?: "",
                    thymeStamp = 0,
                    page = page,
                    caller = caller
                )
            }

        println("$TAG getImgsFromPexels(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    private suspend fun getImgsFromUnsplash(page: Int,
                                            query: String?,
                                            caller: String): List<SnftrDto>? {
        val mapped = snftrService.searchUnsplash(page = page, query = query)?.results?.map {
            SnftrEntity(
                id = getId(),
                title = it.user.username ?: unsplashTag,
                creator = it.user.username ?: it.user.name ?: it.user.firstName ?: "",
                creatorProfilePic = it.user.links.self ?: "",
                provider = unsplashTag,
                sourceUrl_detail = it.urls.regular,
                sourceUrl_thumb = it.urls.thumb,
                sourceUrl_owner = it.user.links.html ?: "https://www.unsplash.com",
                current_query = query ?: "",
                thymeStamp = 0,
                page = page,
                caller = caller
            )
        }
        println("$TAG getImgsFromUnsplash(query = $query, page: $page, caller: $caller): ${mapped?.size}")
        return dtoMapper.toDomainList(mapped)
    }

    companion object {
        const val TAG = "SearchSnftrs"
    }
}
