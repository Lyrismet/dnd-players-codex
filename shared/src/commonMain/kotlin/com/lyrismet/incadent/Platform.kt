package com.lyrismet.incadent

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
