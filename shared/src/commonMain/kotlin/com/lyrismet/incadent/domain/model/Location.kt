package com.lyrismet.incadent.domain.model

data class Location(
    val id: Long,
    val name: String,
    val type: String,
    val description: String,
    val region: String,
)
