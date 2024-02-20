package com.exoteric.sharedactor

class JVMPlatform: Platform {
    override val name: String = "JVMLib"
}

actual fun getPlatform(): Platform = JVMPlatform()