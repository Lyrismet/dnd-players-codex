package com.lyrismet.incadent.feature.codex.data.remote

import com.lyrismet.incadent.core.errors.AppErrors
import com.lyrismet.incadent.feature.codex.data.remote.spellimport.AllowedHosts
import com.lyrismet.incadent.feature.codex.data.remote.spellimport.LinkParse
import com.lyrismet.incadent.feature.codex.data.remote.spellimport.SpellRequest
import com.lyrismet.incadent.feature.codex.data.remote.spellimport.parseSpellLink
import com.lyrismet.incadent.feature.codex.data.remote.spellimport.readSpell
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportFailure
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportResult
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImporter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray

private const val MAX_BYTES = 1_000_000L
private const val MAX_REDIRECTS = 3
private val REDIRECTS = HttpStatusCode.MultipleChoices.value until HttpStatusCode.BadRequest.value

private class UnreadablePageException(
    site: String,
) : Exception("the page of $site has no spell the readers know")

private val Origin = Regex("^(https://[^/?#]+)")

/** fetches the page or the api answer of a known site over https and reads the spell out of it */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class KtorSpellImporter(
    private val client: HttpClient,
) : SpellImporter {
    private val reportedSites = mutableSetOf<String>()

    override suspend fun import(link: String): SpellImportResult =
        when (val parsed = parseSpellLink(link)) {
            LinkParse.UnsupportedSite -> failed(SpellImportFailure.UNSUPPORTED_SITE)
            LinkParse.NotASpell -> failed(SpellImportFailure.UNREADABLE)
            is LinkParse.Request -> read(parsed.request)
        }

    private suspend fun read(request: SpellRequest): SpellImportResult =
        when (val fetched = fetchSafely(request.url)) {
            is Fetched.Body ->
                readSpell(
                    request.format,
                    fetched.text,
                )?.let { SpellImportResult.Imported(it.copy(source = request.source)) }
                    ?: unreadable(request)
            is Fetched.Failure -> failed(fetched.failure)
        }

    // a page the readers cannot make a spell of means a site changed its markup, the one failure worth reporting
    private fun unreadable(request: SpellRequest): SpellImportResult {
        val site = request.source.siteName
        // once per site and launch, so a challenge page answered to every try does not flood the reports
        if (reportedSites.add(
                site,
            )
        ) {
            AppErrors.report("SpellImport", "a page of $site could not be read", UnreadablePageException(site))
        }
        return failed(SpellImportFailure.UNREADABLE)
    }

    // no connection, a refused or a timed out request all read the same to the player
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun fetchSafely(url: String): Fetched =
        try {
            fetch(url)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (any: Exception) {
            Fetched.Failure(SpellImportFailure.OFFLINE)
        }

    private suspend fun fetch(start: String): Fetched {
        var url: String? = start
        repeat(MAX_REDIRECTS + 1) {
            val response = client.get(url ?: return@repeat)
            if (response.status.value !in REDIRECTS) return answer(response)
            url = nextUrl(url, response.headers[HttpHeaders.Location])
        }
        return Fetched.Failure(SpellImportFailure.UNREADABLE)
    }

    private suspend fun answer(response: HttpResponse): Fetched =
        when {
            response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone ->
                Fetched.Failure(SpellImportFailure.NOT_FOUND)
            response.status.isSuccess() -> readBody(response)
            else -> Fetched.Failure(SpellImportFailure.UNREADABLE)
        }

    // a redirect may only lead to a known site over https
    private fun nextUrl(
        current: String,
        location: String?,
    ): String? {
        val target =
            when {
                location?.startsWith("https://") == true -> location
                location?.startsWith("/") == true -> Origin.find(current)?.value.orEmpty() + location
                else -> null
            }
        val host = target?.let { Origin.find(it)?.value?.removePrefix("https://") }
        return target?.takeIf { host in AllowedHosts }
    }

    private suspend fun readBody(response: HttpResponse): Fetched {
        val bytes = response.bodyAsChannel().readRemaining(MAX_BYTES + 1).readByteArray()
        return if (bytes.size > MAX_BYTES) {
            Fetched.Failure(SpellImportFailure.UNREADABLE)
        } else {
            Fetched.Body(bytes.decodeToString())
        }
    }

    private fun failed(failure: SpellImportFailure) = SpellImportResult.Failed(failure)

    private sealed interface Fetched {
        data class Body(
            val text: String,
        ) : Fetched

        data class Failure(
            val failure: SpellImportFailure,
        ) : Fetched
    }
}
