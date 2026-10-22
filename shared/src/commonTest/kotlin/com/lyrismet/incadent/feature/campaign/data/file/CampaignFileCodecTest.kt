package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.MAX_FILE_BYTES
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CampaignFileCodecTest {
    private val header = FileHeader(FILE_FORMAT, FILE_VERSION, "World", records = 3)
    private val date = LocalDateTime.parse("2026-01-02T03:04")

    private fun fileOf(vararg records: FileRecord): MemoryFile =
        MemoryFile().also { file ->
            val writer = CampaignFileWriter(file)
            writer.writeHeader(header)
            records.forEach(writer::write)
        }

    private fun readAll(file: MemoryFile): List<FileRecord> {
        val reader = CampaignFileReader.open(file)
        return generateSequence { reader.nextRecord() }.toList()
    }

    private fun errorOf(block: () -> Unit): ImportError = assertFailsWith<ImportFailedException>(block = block).error

    @Test
    fun `records written come back the same`() {
        val records =
            listOf(
                FileRecord.Species(1, "Wolf", CreatureType.BEAST),
                FileRecord.Place(1, "Town"),
                FileRecord.Place(2, "Inn", parentId = 1),
                FileRecord.Npc(1, "Bob", NpcStatus.NEUTRAL, speciesId = 1, placeIds = listOf(2)),
                FileRecord.Quest(1, "Find", QuestStatus.ACTIVE, giverNpcId = 1, placeId = 2),
                FileRecord.Session(1, "Day one", date, tags = listOf("fight")),
                FileRecord.Entry(1, date, "met @[Bob|n:1]"),
                FileRecord.Photo(PhotoOwner.NPC, 1, "AAAA"),
            )
        assertEquals(records, readAll(fileOf(*records.toTypedArray())))
    }

    @Test
    fun `a species line from before hit points reads with none`() {
        val file =
            MemoryFile(
                listOf(
                    """{"format":"$FILE_FORMAT","version":$FILE_VERSION,"name":"World"}""",
                    """{"t":"species","id":1,"name":"Wolf","creatureType":"BEAST"}""",
                ),
            )

        assertEquals(listOf<FileRecord>(FileRecord.Species(1, "Wolf", CreatureType.BEAST)), readAll(file))
    }

    @Test
    fun `the header is read before any record`() {
        val reader = CampaignFileReader.open(fileOf())
        assertEquals("World", reader.header.name)
        assertNull(reader.nextRecord())
    }

    @Test
    fun `a file without the mark is not an incadent file`() {
        assertEquals(ImportError.NotIncadent, errorOf { CampaignFileReader.open(MemoryFile(listOf("""{"a":1}"""))) })
        assertEquals(ImportError.NotIncadent, errorOf { CampaignFileReader.open(MemoryFile(listOf("hello"))) })
        assertEquals(ImportError.NotIncadent, errorOf { CampaignFileReader.open(MemoryFile()) })
    }

    @Test
    fun `a file of a newer version is turned away`() {
        val file = MemoryFile(listOf("""{"format":"incadent","version":${FILE_VERSION + 1}}"""))
        assertEquals(ImportError.NewerVersion, errorOf { CampaignFileReader.open(file) })
    }

    @Test
    fun `a record of an unknown type is malformed`() {
        val file = MemoryFile(listOf("""{"format":"incadent","version":1}""", """{"t":"dragon"}"""))
        assertEquals(ImportError.Malformed, errorOf { CampaignFileReader.open(file).nextRecord() })
    }

    @Test
    fun `an unknown status value is malformed`() {
        val line = """{"t":"npc","id":1,"name":"Bob","status":"EVIL_GENIUS"}"""
        val file = MemoryFile(listOf("""{"format":"incadent","version":1}""", line))
        assertEquals(ImportError.Malformed, errorOf { CampaignFileReader.open(file).nextRecord() })
    }

    @Test
    fun `a line past the limit is too large`() {
        val long = "x".repeat(MAX_LINE_CHARS + 1)
        val file = MemoryFile(listOf("""{"format":"incadent","version":1}""", long))
        assertEquals(ImportError.TooLarge, errorOf { CampaignFileReader.open(file).nextRecord() })
    }

    @Test
    fun `a reported size past the limit is refused before reading`() {
        val file =
            object : com.lyrismet.incadent.core.file.FileSource {
                override val sizeOrNull: Long = MAX_FILE_BYTES + 1

                override fun readLine(maxChars: Int): String = error("must not be read")

                override fun close() = Unit
            }
        assertEquals(ImportError.TooLarge, errorOf { CampaignFileReader.open(file) })
    }

    @Test
    fun `reading stops when the characters read pass the limit`() {
        val photo = FileRecord.Photo(PhotoOwner.NPC, 1, "A".repeat(MAX_LINE_CHARS - 100))
        val lines =
            listOf("""{"format":"incadent","version":1}""", """{"t":"npc","id":1,"name":"B","status":"NEUTRAL"}""") +
                List(
                    (MAX_FILE_BYTES / MAX_LINE_CHARS).toInt() + 2,
                ) { FileJson.encodeToString(FileRecord.serializer(), photo) }
        assertEquals(ImportError.TooLarge, errorOf { readAll(MemoryFile(lines)) })
    }

    @Test
    fun `blank lines between records are skipped`() {
        val lines = listOf("""{"format":"incadent","version":1}""", "", """{"t":"tag","name":"a"}""", "")
        assertEquals(listOf<FileRecord>(FileRecord.Tag("a")), readAll(MemoryFile(lines)))
    }
}
