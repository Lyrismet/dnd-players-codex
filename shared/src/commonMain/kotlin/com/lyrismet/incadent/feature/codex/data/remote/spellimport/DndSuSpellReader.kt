package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource

private val Title = Regex("<h2 class=\"card-title\"[^>]*>\\s*<span data-copy=\"([^\"]*)\"")

// the class of any character is used so a line break matches, the dot flag is not in common Kotlin
private val ListItem = Regex("<li[^>]*>([\\s\\S]*?)</li>")
private val Labeled = Regex("^<strong>(?:<a[^>]*>)?([^<:]+)(?:</a>)?:</strong>([\\s\\S]*)$")
private val LevelLine = Regex("^(\\d+)\\s*уровень", RegexOption.IGNORE_CASE)
private val Tag = Regex("<[^>]*>")
private val BlockBreak = Regex("</p>|<br\\s*/?>")
private val BlankRuns = Regex("\n{3,}")
private val Params = Regex("<ul class=[\"']params[^>]*>")
private val DescriptionItem = Regex("<li class=[\"']subsection desc[\"']>")
private const val DESCRIPTION = "itemprop=\"description\">"
private const val DESCRIPTION_END = "</div></li>"

/** a spell from the page of dnd.su, null when the page is not one or has no name, level or text */
internal fun readDndSuSpell(html: String): ImportedSpell? {
    val name =
        Title
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.let(::decodeEntities)
            ?.substringBefore(" [")
    val paramsStart = Params.find(html)?.range?.last
    val params = paramsStart?.let { html.substring(it + 1) }.orEmpty()
    if (name == null || params.isEmpty()) return null
    val head = params.split(DescriptionItem, limit = 2)[0]
    val body = params.substringAfter(DESCRIPTION, "").substringBefore(DESCRIPTION_END)
    val items = ListItem.findAll(head).map { it.groupValues[1].trim() }.toList()
    val line = items.firstOrNull { !it.startsWith("<strong>") }?.let(::htmlToText).orEmpty()
    val fields =
        items
            .mapNotNull { Labeled.find(it) }
            .associate { it.groupValues[1].trim() to htmlToText(it.groupValues[2]) }
    val level =
        LevelLine
            .find(line)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: if (line.lowercase().startsWith("заговор")) 0 else -1
    val components = fields["Компоненты"].orEmpty()
    // the new site writes the component letters with latin lookalikes
    val letters =
        components.substringBefore('(').split(',').map {
            it
                .trim()
                .replace('C', 'С')
                .replace('M', 'М')
                .replace('B', 'В')
        }
    val duration = fields["Длительность"].orEmpty()
    val casting = (fields["Время накладывания"] ?: fields["Время сотворения"]).orEmpty()
    return SpellFields(
        source = SpellSource.DND_SU,
        name = name,
        level = level,
        schoolText = line.substringAfter(',', ""),
        castingText = casting,
        rangeText = fields["Дистанция"].orEmpty(),
        verbal = "В" in letters,
        somatic = "С" in letters,
        material = if ("М" in letters) components.substringAfter('(', "").substringBeforeLast(')', "") else null,
        durationText = duration,
        concentration = duration.contains("концентрац", ignoreCase = true),
        ritual = line.contains("ритуал", ignoreCase = true) || casting.contains("ритуал", ignoreCase = true),
        description = htmlToText(body),
    ).toImportedSpell()
}

private fun htmlToText(html: String): String =
    decodeEntities(
        html
            .replace(BlockBreak, "\n")
            .replace(Tag, ""),
    ).lines()
        .joinToString("\n") { it.trim() }
        .replace(BlankRuns, "\n\n")
        .trim()

private fun decodeEntities(text: String): String =
    text
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
