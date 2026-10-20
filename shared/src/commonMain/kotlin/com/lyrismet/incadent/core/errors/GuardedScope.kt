package com.lyrismet.incadent.core.errors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.lyrismet.incadent.core.logging.AppLog
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.error_action_failed
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.stringResource

private const val LOG_COMPONENT = "Action"

/** a presenter scope whose failed launches are logged and reported to [onFailure], not left to end the app */
@Composable
fun rememberGuardedScope(onFailure: (Throwable) -> Unit = {}): CoroutineScope {
    val latest = rememberUpdatedState(onFailure)
    val base =
        rememberCoroutineScope {
            CoroutineExceptionHandler { _, failure ->
                AppLog.error(LOG_COMPONENT, "an action failed", failure)
                latest.value(failure)
            }
        }
    // a supervisor so one failed launch does not cancel the later ones
    return remember(base) { CoroutineScope(base.coroutineContext + SupervisorJob(base.coroutineContext[Job])) }
}

/** shows the localized "could not save" text, or the one [known] gives for a failure it recognises, then clears it */
@Composable
fun ReportFailures(
    failure: MutableState<Throwable?>,
    known: (Throwable) -> String? = { null },
    show: (String) -> Unit,
) {
    val text = stringResource(Res.string.error_action_failed)
    LaunchedEffect(failure.value) {
        val current = failure.value
        if (current != null) {
            show(known(current) ?: text)
            failure.value = null
        }
    }
}
