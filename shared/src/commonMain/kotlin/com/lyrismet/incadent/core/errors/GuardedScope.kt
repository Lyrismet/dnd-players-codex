package com.lyrismet.incadent.core.errors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.lyrismet.incadent.core.logging.AppLog
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.error_action_failed
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource

private const val LOG_COMPONENT = "Action"

/** a presenter scope whose failed launches are logged and reported to [onFailure], not left to end the app */
@Composable
fun rememberGuardedScope(onFailure: (Throwable) -> Unit = {}): CoroutineScope {
    val latest = rememberUpdatedState(onFailure)
    return rememberCoroutineScope {
        CoroutineExceptionHandler { _, failure ->
            AppLog.error(LOG_COMPONENT, "an action failed", failure)
            latest.value(failure)
        }
    }
}

/**
 * hands the localized "could not save" text to [show] for every failure [failure] receives, then clears it. A host
 * that knows a failure by its own cause answers with a text for it from [known], null leaves it to the general one.
 */
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
