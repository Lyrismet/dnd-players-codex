package com.lyrismet.incadent.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/** where blocking database and file work runs, so it never takes cores away from Dispatchers.Default */
expect val DatabaseDispatcher: CoroutineDispatcher
