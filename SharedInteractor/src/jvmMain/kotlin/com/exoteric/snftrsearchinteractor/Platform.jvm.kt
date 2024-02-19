package com.exoteric.snftrsearchinteractor

import com.exoteric.snftrsearchinteractor.Platform

class JVMPlatform: Platform {
    override val name: String = "JVMLib"
}

actual fun getPlatform(): Platform = JVMPlatform()