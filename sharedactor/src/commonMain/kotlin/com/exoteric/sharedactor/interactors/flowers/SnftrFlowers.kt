package com.exoteric.sharedactor.interactors.flowers

import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.interactors.dtos.SnftrDto

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
