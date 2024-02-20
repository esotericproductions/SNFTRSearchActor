package com.exoteric.sharedactor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform