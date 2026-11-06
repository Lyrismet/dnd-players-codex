package com.lyrismet.incadent

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionTextStringsTest {
    private fun intros(
        folder: String,
        array: String,
    ): List<String> {
        val text = File("src/commonMain/composeResources/$folder/strings.xml").readText()
        val body = Regex("""<string-array name="$array">(.*?)</string-array>""", RegexOption.DOT_MATCHES_ALL)
        return Regex(
            """<item>(.*?)</item>""",
        ).findAll(body.find(text)!!.groupValues[1]).map { it.groupValues[1] }.toList()
    }

    private val sets =
        listOf("values", "values-en").flatMap { folder ->
            listOf("session_text_intros_named", "session_text_intros_plain").map { array ->
                Triple(folder, array, intros(folder, array))
            }
        }

    @Test
    fun `the named and the plain jokes pair up in both languages`() {
        val sizes = sets.map { it.third.size }.toSet()

        assertEquals(1, sizes.size, "every set of jokes has the same size")
        assertTrue(sizes.single() >= 2)
    }

    @Test
    fun `a named joke has the name slot once and a plain one has none`() {
        sets.forEach { (folder, array, items) ->
            items.forEach {
                val slots = Regex("""\{name\}""").findAll(it).count()
                if (array.endsWith("named")) {
                    assertEquals(1, slots, "$folder $array: $it")
                } else {
                    assertEquals(0, slots, "$folder $array: $it")
                }
            }
        }
    }

    @Test
    fun `the jokes keep to the present tense`() {
        val russianPast =
            Regex("""[а-яё]{3,}(ал|ала|ало|али|ил|ила|ило|или|ел|ела|ело|ели|ыл|ыла|ыло|ыли|ул|ула|уло|ули)\b""")
        val englishPast = Regex("""\b(was|were|had|did|\w{3,}ed)\b""")
        sets.forEach { (folder, array, items) ->
            items.forEach {
                val text = it.replace("{name}", "")
                val regex = if (folder == "values") russianPast else englishPast
                assertTrue(regex.find(text.lowercase()) == null, "$folder $array: $it")
            }
        }
    }
}
