package com.lyrismet.incadent.feature.codex.data.remote

import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportFailure
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val FIREBALL =
    """{"name":"Fireball","desc":["Takes 8d6 fire damage."],"range":"150 feet","components":["V","S"],
    "duration":"Instantaneous","casting_time":"1 action","level":3,"school":{"name":"Evocation"}}"""
private const val API_LINK = "https://www.dnd5eapi.co/api/2014/spells/fireball"

class KtorSpellImporterTest {
    private fun importer(
        requests: MutableList<HttpRequestData> = mutableListOf(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = KtorSpellImporter(
        HttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ) { configureAppClient() },
    )

    private fun failure(result: SpellImportResult) = assertIs<SpellImportResult.Failed>(result).failure

    @Test
    fun `an answer is read into a spell`() =
        runTest {
            val result = importer { respond(FIREBALL, HttpStatusCode.OK) }.import(API_LINK)

            val spell = assertIs<SpellImportResult.Imported>(result).spell
            assertEquals("Fireball", spell.name)
            assertEquals(listOf("8d6"), spell.dice)
        }

    @Test
    fun `the request goes over https with the app user agent`() =
        runTest {
            val requests = mutableListOf<HttpRequestData>()
            importer(requests) { respond(FIREBALL, HttpStatusCode.OK) }.import("dnd5eapi.co/api/spells/fireball")

            assertEquals("https://www.dnd5eapi.co/api/2014/spells/fireball", requests.single().url.toString())
            assertEquals("Incadent", requests.single().headers[HttpHeaders.UserAgent])
        }

    @Test
    fun `a missing page is not found`() =
        runTest {
            assertEquals(
                SpellImportFailure.NOT_FOUND,
                failure(importer { respond("", HttpStatusCode.NotFound) }.import(API_LINK)),
            )
        }

    @Test
    fun `a server error is unreadable`() =
        runTest {
            assertEquals(
                SpellImportFailure.UNREADABLE,
                failure(importer { respond("", HttpStatusCode.InternalServerError) }.import(API_LINK)),
            )
        }

    @Test
    fun `a failed connection is offline`() =
        runTest {
            assertEquals(
                SpellImportFailure.OFFLINE,
                failure(importer { error("no route to host") }.import(API_LINK)),
            )
        }

    @Test
    fun `another site is not asked at all`() =
        runTest {
            val requests = mutableListOf<HttpRequestData>()

            val result = importer(requests) { respond(FIREBALL, HttpStatusCode.OK) }.import("https://roll20.net/x")

            assertEquals(SpellImportFailure.UNSUPPORTED_SITE, failure(result))
            assertTrue(requests.isEmpty())
        }

    @Test
    fun `a redirect inside the known sites is followed`() =
        runTest {
            val requests = mutableListOf<HttpRequestData>()
            val importer =
                importer(requests) {
                    // the first answer moves the request, the second one is the spell
                    if (requests.size > 1) {
                        respond(FIREBALL, HttpStatusCode.OK)
                    } else {
                        respond("", HttpStatusCode.MovedPermanently, headersOf(HttpHeaders.Location, "/moved/fireball"))
                    }
                }

            assertIs<SpellImportResult.Imported>(importer.import("https://www.dnd5eapi.co/api/spells/fireball"))
            assertEquals(2, requests.size)
        }

    @Test
    fun `a redirect to another site is stopped`() =
        runTest {
            val requests = mutableListOf<HttpRequestData>()
            val importer =
                importer(requests) {
                    respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://evil.example/x"))
                }

            assertEquals(SpellImportFailure.UNREADABLE, failure(importer.import(API_LINK)))
            assertEquals(1, requests.size)
        }

    @Test
    fun `an answer over the size limit is unreadable`() =
        runTest {
            val huge = "x".repeat(1_000_001)

            assertEquals(
                SpellImportFailure.UNREADABLE,
                failure(importer { respond(huge, HttpStatusCode.OK) }.import(API_LINK)),
            )
        }

    @Test
    fun `an answer that is not a spell is unreadable`() =
        runTest {
            assertEquals(
                SpellImportFailure.UNREADABLE,
                failure(importer { respond("<html></html>", HttpStatusCode.OK) }.import(API_LINK)),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelling the import cancels the request`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val importer =
                importer {
                    started.complete(Unit)
                    CompletableDeferred<Unit>().await()
                    respond(FIREBALL, HttpStatusCode.OK)
                }
            val job = launch { importer.import(API_LINK) }
            runCurrent()
            started.await()

            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
        }
}
