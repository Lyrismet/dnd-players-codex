package com.lyrismet.incadent.core.identity

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** a fresh stable id for a record, 32 lowercase hex like the ones the migration gave the old rows */
@OptIn(ExperimentalUuidApi::class)
fun newOriginId(): String = Uuid.random().toHexString()

/** keeps an id the record already has, a blank one means it is new */
fun String.orNewOriginId(): String = ifBlank(::newOriginId)
