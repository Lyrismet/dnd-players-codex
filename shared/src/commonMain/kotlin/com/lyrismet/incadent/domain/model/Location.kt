package com.lyrismet.incadent.domain.model

data class Location(
    val id: Long,
    val name: String,
    val description: String,
    // the place this one is inside, null for a top level place
    val parentId: Long? = null,
    // wide picture of the place shown at the top of its card, null until one is set
    val cover: Photo? = null,
)
