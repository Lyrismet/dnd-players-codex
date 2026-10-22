package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.core.file.FileSource
import com.lyrismet.incadent.core.file.LineTooLongException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.MAX_FILE_BYTES
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// a main photo is at most about 90 KB, in base64 and with room to spare it still fits well under this
internal const val MAX_LINE_CHARS = 1_000_000
internal const val MAX_RECORDS = 200_000
private const val BOM = "\uFEFF"

internal val FileJson =
    Json {
        classDiscriminator = "t"
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

/** writes a file line by line, a photo is encoded and written alone so memory never holds two */
class CampaignFileWriter(
    private val sink: FileSink,
) {
    fun writeHeader(header: FileHeader) = sink.writeLine(FileJson.encodeToString(FileHeader.serializer(), header))

    fun write(record: FileRecord) = sink.writeLine(FileJson.encodeToString(FileRecord.serializer(), record))
}

/** reads a file line by line, every failure is an [ImportFailedException] and nothing past the header is trusted */
class CampaignFileReader private constructor(
    private val source: FileSource,
    val header: FileHeader,
    private var charsRead: Long,
) {
    private val validator = RecordValidator()
    private var count = 0

    /** the next record or null at the end of the file */
    fun nextRecord(): FileRecord? {
        while (true) {
            val line = readLimited() ?: return null
            if (line.isBlank()) continue
            if (++count > MAX_RECORDS) throw ImportFailedException(ImportError.TooLarge)
            return decode(line).also(validator::accept)
        }
    }

    // a serialization failure is an illegal argument too, so one catch covers a bad shape and a bad value
    private fun decode(line: String): FileRecord =
        try {
            FileJson.decodeFromString(FileRecord.serializer(), line)
        } catch (_: IllegalArgumentException) {
            throw ImportFailedException(ImportError.Malformed)
        }

    private fun readLimited(): String? {
        val line =
            try {
                source.readLine(MAX_LINE_CHARS)
            } catch (_: LineTooLongException) {
                throw ImportFailedException(ImportError.TooLarge)
            }
        charsRead += line?.length ?: 0
        if (charsRead > MAX_FILE_BYTES) throw ImportFailedException(ImportError.TooLarge)
        return line
    }

    companion object {
        /** checks the size and the header, so a foreign or too new file is turned away before its body is read */
        fun open(source: FileSource): CampaignFileReader {
            if ((source.sizeOrNull ?: 0L) > MAX_FILE_BYTES) throw ImportFailedException(ImportError.TooLarge)
            // a missing or endless first line reads as an empty one, which is no header
            val first =
                try {
                    source.readLine(MAX_LINE_CHARS).orEmpty()
                } catch (_: LineTooLongException) {
                    ""
                }
            val header = parseHeader(first.removePrefix(BOM))
            header.problem()?.let { throw ImportFailedException(it) }
            return CampaignFileReader(source, requireNotNull(header), first.length.toLong())
        }

        private fun parseHeader(line: String): FileHeader? =
            try {
                FileJson.decodeFromString(FileHeader.serializer(), line)
            } catch (_: SerializationException) {
                null
            }

        private fun FileHeader?.problem(): ImportError? =
            when {
                this == null || mark != FILE_FORMAT -> ImportError.NotIncadent
                version > FILE_VERSION -> ImportError.NewerVersion
                version < 1 -> ImportError.Malformed
                else -> null
            }
    }
}
