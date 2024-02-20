package com.exoteric.sharedactor.domain.util

import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * SnftrFlow.watch() for collecting a flow on ios
 * Source:
 * https://stackoverflow.com/questions/64175099/listen-to-kotlin-coroutine-flow-from-ios
 */
fun <T> Flow<T>.snftrFlow(): SnftrFlow<T> = SnftrFlow(this)

class SnftrFlow<T>(private val origin: Flow<T>) : Flow<T> by origin {
    fun watch(block: (T) -> Unit): Closeable {
        val job = Job()

        onEach { block(it) }.launchIn(CoroutineScope(Dispatchers.Main + job))
        return object : Closeable {
            override fun close() {
                println("snftrFlow().close()")
                job.cancel()
            }
        }
    }
}







