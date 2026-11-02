package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField

// a conflict stays mine and a fill is taken unless [chosen] says otherwise, [everything] takes all of the file
internal class FieldPick(
    private val chosen: Map<RecordField, FieldChoice>,
    private val everything: Boolean,
    private val label: String,
) {
    private fun choice(
        field: RecordField,
        fill: Boolean,
    ): FieldChoice = chosen[field] ?: if (everything || fill) FieldChoice.FILE else FieldChoice.MINE

    fun takes(change: FieldChange): Boolean = choice(change.field, change.isFill) != FieldChoice.MINE

    /** a file that has nothing in the field never clears it, a second text goes below the first with its mark */
    fun text(
        field: RecordField,
        mine: String,
        file: String,
    ): String =
        if (file.isBlank()) {
            mine
        } else {
            when (choice(field, mine.isBlank())) {
                FieldChoice.MINE -> mine
                FieldChoice.FILE -> file
                FieldChoice.BOTH -> if (mine.isBlank()) file else "$mine\n\n$label: $file"
            }
        }

    fun <T> value(
        field: RecordField,
        mine: T,
        file: T,
    ): T = if (choice(field, fill = false) == FieldChoice.FILE) file else mine

    fun number(
        field: RecordField,
        mine: Int?,
        file: Int?,
    ): Int? =
        if (file ==
            null
        ) {
            mine
        } else if (mine == null || choice(field, fill = false) == FieldChoice.FILE) {
            file
        } else {
            mine
        }
}
