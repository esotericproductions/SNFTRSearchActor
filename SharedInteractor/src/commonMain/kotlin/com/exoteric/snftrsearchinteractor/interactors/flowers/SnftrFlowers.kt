package com.exoteric.snftrsearchinteractor.interactors.flowers

import com.exoteric.snftrsearchinteractor.domain.data.DataState
import com.exoteric.snftrsearchinteractor.domain.util.SnftrFlow
import com.exoteric.snftrsearchinteractor.interactors.dtos.SnftrDto

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

interface SnftrTrendsFlower {
    @Throws(Exception::class)
    fun executeTrendsSearch(page: Int,
                            isNewSearch: Boolean,
                            isFirstSearch: Boolean):
            SnftrFlow<DataState<List<SnftrDto>>>
}

interface SnftrTrendsCacheFlower {
    @Throws(Exception::class)
    fun executeTrendsCacheSearch(page: Int):
            SnftrFlow<DataState<List<SnftrDto>>>
}
