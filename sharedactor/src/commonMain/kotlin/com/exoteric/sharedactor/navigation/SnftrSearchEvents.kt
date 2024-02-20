package com.exoteric.sharedactor.navigation

import kotlin.native.concurrent.ThreadLocal

sealed class SnftrEvent

sealed class SnftrSearchEvent: SnftrEvent() {
    @ThreadLocal
    data object NewSearchEvent : SnftrSearchEvent()
    @ThreadLocal
    data object RestoreStateEvent : SnftrSearchEvent()
    @ThreadLocal
    data object SearchPreviousPgEvent : SnftrSearchEvent()
    @ThreadLocal
    data object SearchNextPgEvent : SnftrSearchEvent()
}
