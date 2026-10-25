package com.lyrismet.incadent.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val DatabaseDispatcher: CoroutineDispatcher = Dispatchers.IO
