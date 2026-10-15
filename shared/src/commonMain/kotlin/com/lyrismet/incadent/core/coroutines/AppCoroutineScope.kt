package com.lyrismet.incadent.core.coroutines

import dev.zacsweers.metro.Qualifier

/** the process-wide scope for work that must outlive a screen, such as the undo timer */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AppCoroutineScope
