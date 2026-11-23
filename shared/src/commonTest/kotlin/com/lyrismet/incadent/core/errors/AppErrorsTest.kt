package com.lyrismet.incadent.core.errors

import com.lyrismet.incadent.core.analytics.CrashReporter
import com.lyrismet.incadent.core.analytics.NoOpCrashReporter
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppErrorsTest {
    private class Recording : CrashReporter {
        val recorded = mutableListOf<Throwable>()

        override fun recordNonFatal(throwable: Throwable) {
            recorded += throwable
        }
    }

    @AfterTest
    fun restore() = AppErrors.install(NoOpCrashReporter)

    @Test
    fun `a reported failure reaches the crash backend with its component and cause`() {
        val reporter = Recording().also(AppErrors::install)
        val cause = IllegalStateException("boom")

        AppErrors.report("Undo", "undo failed", cause)

        val sent = reporter.recorded.single()
        assertEquals("[Undo] undo failed", sent.message)
        assertSame(cause, sent.cause)
    }

    @Test
    fun `a crash backend that fails does not fail the report`() {
        AppErrors.install(
            object : CrashReporter {
                override fun recordNonFatal(throwable: Throwable) = error("backend down")
            },
        )

        AppErrors.report("Undo", "undo failed", IllegalStateException("boom"))
    }
}
