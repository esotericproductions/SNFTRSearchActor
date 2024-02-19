package com.exoteric.snftrsearchinteractor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform