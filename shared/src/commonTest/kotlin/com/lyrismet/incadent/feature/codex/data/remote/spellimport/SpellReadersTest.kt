package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedDuration
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedRange
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSchool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpellReadersTest {
    private val dndSuPage =
        """
        <h2 class="card-title" itemprop="name"><span data-copy="Адское возмездие [Hellish rebuke]">Адское</span></h2>
        <ul class="params card__article-body"><li class="size-type-alignment">1 уровень, воплощение</li>
        <li><strong>Время накладывания:</strong> 1 реакция, совершаемая вами</li>
        <li><strong>Дистанция:</strong> 60 футов</li>
        <li><strong>Компоненты:</strong> В, С, М (щепотка &quot;пепла&quot;)</li>
        <li><strong>Длительность:</strong> Концентрация, вплоть до 1 минуты</li>
        <li><strong>Классы:</strong> колдун</li>
        <li class="subsection desc"><div itemprop="description"><p>Урон 2к10 огнём.</p>
        <p><strong>На больших уровнях.</strong> Урон растёт на 1к10.</p>
        </div></li></ul>
        """.trimIndent()

    @Test
    fun `dnd su page is read into the fields`() {
        val spell = assertNotNull(readDndSuSpell(dndSuPage))

        assertEquals("Адское возмездие", spell.name)
        assertEquals(1, spell.level)
        assertEquals(SpellSchool.EVOCATION, spell.school)
        assertEquals(CastKind.REACTION, spell.castKind)
        assertEquals(ImportedRange.Feet(60), spell.range)
        assertTrue(spell.verbal && spell.somatic)
        assertEquals("щепотка \"пепла\"", spell.material)
        assertEquals(ImportedDuration(DurationUnit.MINUTE, 1, ""), spell.duration)
        assertTrue(spell.concentration)
        assertFalse(spell.ritual)
        assertEquals(listOf("2к10", "1к10"), spell.dice)
        assertTrue(spell.text.startsWith("Урон 2к10 огнём."))
    }

    @Test
    fun `next dnd su page with linked labels is read into the fields`() {
        val html =
            """
            <h2 class="card-title" itemprop="name">
                <span data-copy="Бурлящий котёл Таши [Tasha’s Bubbling Cauldron]">Бурлящий</span>
            </h2>
            <ul class='params card__article-body params-spells'><li class='school_level'><a href='/a'>6 уровень</a>, <a href='/b'>Призыв</a></li>
            <li class='cast_time'><strong><a href='/c'>Время сотворения</a>:</strong> <a href='/g'>Действие</a></li>
            <li class='range'><strong><a href='/d'>Дистанция</a>:</strong> 5 футов</li>
            <li class='components'><strong><a href='/e'>Компоненты</a>:</strong> В, C, М (позолоченный черпак)</li>
            <li class='duration'><strong><a href='/f'>Длительность</a>:</strong> 10 минут</li>
            <li class='subsection desc'><div itemprop="description"><p>Вы сотворяете котёл.</p></div></li></ul>
            """.trimIndent()

        val spell = assertNotNull(readDndSuSpell(html))

        assertEquals("Бурлящий котёл Таши", spell.name)
        assertEquals(6, spell.level)
        assertEquals(SpellSchool.CONJURATION, spell.school)
        assertEquals(CastKind.ACTION, spell.castKind)
        assertTrue(spell.verbal)
        assertTrue(spell.somatic)
        assertEquals("позолоченный черпак", spell.material)
        assertTrue(spell.text.startsWith("Вы сотворяете котёл."))
    }

    @Test
    fun `dnd su cantrip and ritual are read from the first line`() {
        val html = dndSuPage.replace("1 уровень, воплощение", "Заговор, прорицание (ритуал)")

        val spell = assertNotNull(readDndSuSpell(html))

        assertEquals(0, spell.level)
        assertEquals(SpellSchool.DIVINATION, spell.school)
        assertTrue(spell.ritual)
    }

    @Test
    fun `dnd su page without a description is not read`() {
        assertNull(readDndSuSpell(dndSuPage.substringBefore("<li class=\"subsection desc\">")))
        assertNull(readDndSuSpell("<html>nothing</html>"))
    }

    @Test
    fun `dnd5eapi spell is read into the fields`() {
        val json =
            """{"index":"fireball","name":"Fireball","desc":["A bright streak.","Takes 8d6 fire damage."],
            "higher_level":["Grows by 1d6."],"range":"150 feet","components":["V","S","M"],
            "material":"A tiny ball","ritual":false,"duration":"Instantaneous","concentration":false,
            "casting_time":"1 action","level":3,"school":{"index":"evocation","name":"Evocation"}}"""

        val spell = assertNotNull(readDnd5eApiSpell(json))

        assertEquals("Fireball", spell.name)
        assertEquals(3, spell.level)
        assertEquals(SpellSchool.EVOCATION, spell.school)
        assertEquals(CastKind.ACTION, spell.castKind)
        assertEquals(ImportedRange.Feet(150), spell.range)
        assertEquals("A tiny ball", spell.material)
        assertEquals(DurationUnit.INSTANT, spell.duration.unit)
        assertEquals(listOf("8d6", "1d6"), spell.dice)
        assertEquals("A bright streak.\n\nTakes 8d6 fire damage.\n\nGrows by 1d6.", spell.text)
    }

    @Test
    fun `open5e v1 spell reads yes and no strings as flags`() {
        val json =
            """{"slug":"shield","name":"Shield","desc":"A barrier.","range":"Self","requires_verbal_components":true,
            "requires_somatic_components":true,"requires_material_components":false,"ritual":"no",
            "duration":"1 round","concentration":"no","casting_time":"1 reaction","level_int":1,"school":"Abjuration"}"""

        val spell = assertNotNull(readOpen5eV1Spell(json))

        assertEquals(ImportedRange.Self, spell.range)
        assertNull(spell.material)
        assertEquals(ImportedDuration(DurationUnit.ROUND, 1, ""), spell.duration)
        assertEquals(CastKind.REACTION, spell.castKind)
        assertFalse(spell.concentration)
    }

    @Test
    fun `open5e v2 spell spaces the keys of the casting time`() {
        val json =
            """{"key":"srd-2024_alarm","name":"Alarm","desc":"A ward.","level":1,"school":{"name":"Abjuration"},
            "range_text":"30 feet","casting_time":"1minute","verbal":true,"somatic":true,"material":true,
            "material_specified":"a bell","duration":"8 hours","concentration":false,"ritual":true}"""

        val spell = assertNotNull(readOpen5eV2Spell(json))

        assertEquals(CastKind.LONG, spell.castKind)
        assertEquals("1 minute", spell.castingTime)
        assertEquals(ImportedDuration(DurationUnit.HOUR, 8, ""), spell.duration)
        assertTrue(spell.ritual)
        assertEquals("a bell", spell.material)
    }

    @Test
    fun `an answer without a name or a level is not read`() {
        assertNull(readDnd5eApiSpell("""{"desc":["x"],"level":1}"""))
        assertNull(readDnd5eApiSpell("""{"name":"X","desc":["x"]}"""))
        assertNull(readDnd5eApiSpell("""{"name":"X","level":12,"desc":["x"]}"""))
        assertNull(readDnd5eApiSpell("not json"))
    }

    @Test
    fun `ranges read into feet touch self or the sites own words`() {
        assertEquals(ImportedRange.Feet(60), readRange("60 футов"))
        assertEquals(ImportedRange.Touch, readRange("Касание"))
        assertEquals(ImportedRange.Self, readRange("Self (30-foot radius)"))
        assertEquals(ImportedRange.Text("Sight"), readRange("Sight"))
    }

    @Test
    fun `durations read into a number or keep the words`() {
        assertEquals(ImportedDuration(DurationUnit.INSTANT, 0, ""), readDuration("Мгновенная"))
        assertEquals(ImportedDuration(DurationUnit.DAY, 7, ""), readDuration("7 days"))
        assertEquals(ImportedDuration(DurationUnit.NONE, 0, "Until dispelled"), readDuration("Until dispelled"))
    }

    @Test
    fun `dice are found in order without repeats`() {
        assertEquals(listOf("8d6", "2к10"), findDice("8d6 fire and 2к10 then 8d6 again"))
        assertTrue(findDice("no dice here, level 3").isEmpty())
    }
}
