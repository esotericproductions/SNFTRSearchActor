package com.exoteric.sharedactor.interactors.purchases

import com.exoteric.sharedactor.datasource.cached.models.SnftrIDPEntity
import com.exoteric.sharedactor.datasource.dtos.SnftrIDPDto
import com.exoteric.sharedactor.domain.data.DataState
import com.exoteric.sharedactor.domain.util.SnftrFlow
import com.exoteric.sharedactor.domain.util.snftrFlow
import com.exoteric.sharedactor.interactors.flowers.SnftrIDPurchasesHistoryFlower
import com.exoteric.snftrdblib.cached.SnftrDatabase
import com.exoteric.snftrsearchlibr.getId
import kotlinx.coroutines.flow.flow

class FetchLatestPurchases(private val snftrDatabase: SnftrDatabase) :
    SnftrIDPurchasesHistoryFlower {
    override fun executeIDPurchasesSearch(
        idPurchases: MutableList<SnftrIDPEntity>?,
        hashName: String
    ): SnftrFlow<DataState<List<SnftrIDPDto>>> = flow {
        try {
            emit(DataState.loading())
            val queries = snftrDatabase.clockPurchasesQueries
            // filter out any duplicate purchases before putting into cache
            val allCachedIP = queries.getAllPurchases(userHash = hashName).executeAsList()
            val filteredPurchases =
                idPurchases?.distinct()?.filter {
                    fp -> fp.thymestamp !in allCachedIP.map { it.thymestamp }
                }
            println(
                "$TAG executeIDPurchasesSearch(): existing: ${allCachedIP.size} " +
                        "--- adding: ${filteredPurchases?.size}"
            )
            if (filteredPurchases != null) {
                for (entity in filteredPurchases) {
                    queries.insertPurchase(
                        id = getId(),
                        userHash = entity.hashName,
                        productId = entity.productId,
                        currency = entity.currency,
                        price = entity.price,
                        thymestamp = entity.thymestamp,
                        expiry = entity.expiry
                    )
                }
            }
            val cacheResult = queries.getAllPurchases(userHash = hashName).executeAsList()
            println("$TAG cacheResult: ${cacheResult.size}")
            // map here since idawnPurchase_Entity is generated by SQLDelight
            val list: ArrayList<SnftrIDPDto> = ArrayList()
            for (entity in cacheResult) {
                list.add(
                    SnftrIDPDto(
                        hashName = entity.userHash,
                        productId = entity.productId,
                        price = entity.price,
                        currency = entity.currency,
                        thymestamp = entity.thymestamp,
                        expiry = entity.expiry
                    )
                )
            }
            println("$TAG Success! executeIDPurchasesSearch(): ${list.size}")
            // emit List<SnftrIDPDto> from cache
            emit(DataState.success(list))
        } catch (e: Exception) {
            emit(
                DataState.error<List<SnftrIDPDto>>(
                    e.message ?: "$TAG Unknown Error - message null"
                )
            )
        }
    }.snftrFlow()

    companion object {
        const val TAG = "FetchLatestIDP"
    }
}