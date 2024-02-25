package com.exoteric.sharedactor

import com.exoteric.snftrsearchlibr.providers
import com.exoteric.snftrsearchlibr.trenders
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class Providers {
    val allProviders
        get() = providers

    val allTrendsProviders
        get() = trenders

    val gifProviders
        get() = com.exoteric.snftrsearchlibr.gifProviders
}