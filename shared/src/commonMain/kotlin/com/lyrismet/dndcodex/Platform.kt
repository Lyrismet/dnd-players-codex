package com.lyrismet.dndcodex

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
