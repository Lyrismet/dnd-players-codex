package com.lyrismet.incadent.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Dispatchers.IO is not public on native and shares this pool anyway
actual val DatabaseDispatcher: CoroutineDispatcher = Dispatchers.Default
