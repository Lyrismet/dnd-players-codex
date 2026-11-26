package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.core.analytics.AnalyticsEvent
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** the import of a spell by its link: the sheet for the link, the load and the hint over the form after it */
internal class SpellImportFlow(
    private val ui: SpellsUiState,
    private val scope: CoroutineScope,
    private val deps: SpellImportDeps,
    private val texts: suspend () -> SpellImportTexts = ::loadSpellImportTexts,
) {
    private var job: Job? = null

    // closing only takes back what the player has not made a spell of yet, so it works on an archived one too
    fun handle(
        event: SpellsEvent.ImportEvent,
        readOnly: Boolean,
        position: Int,
    ) {
        when (event) {
            SpellsEvent.ImportClosed -> close()
            SpellsEvent.ImportHintDismissed -> endHint()
            is SpellsEvent.ImportLinkChanged -> if (!readOnly) changeLink(event.link)
            SpellsEvent.ImportOpened -> if (!readOnly) open()
            SpellsEvent.ImportSubmitted -> if (!readOnly) submit(position)
        }
    }

    /** the form is gone, so is the sheet, a load that is still running and the hint */
    fun reset() {
        close()
        endHint()
    }

    /** only the form of a new spell has the button, a block or an existing spell never gets here */
    fun open() {
        if (ui.draft?.id == 0L && ui.block == null) ui.importSheet = SpellImportSheetState()
    }

    fun changeLink(link: String) {
        val sheet = ui.importSheet ?: return
        if (sheet.phase !is ImportPhase.Loading) ui.importSheet = sheet.copy(link = link, phase = ImportPhase.Idle)
    }

    /** [position] is where the spell would stand in the list */
    fun submit(position: Int) {
        val sheet = ui.importSheet ?: return
        if (sheet.link.isBlank() || sheet.phase is ImportPhase.Loading) return
        ui.importSheet = sheet.copy(phase = ImportPhase.Loading)
        job =
            scope.launch {
                when (val result = deps.importer.import(sheet.link)) {
                    is SpellImportResult.Imported -> {
                        ui.draft = result.spell.toDraft(texts(), position)
                        ui.block = null
                        ui.importSheet = null
                        ui.importHint = ImportHint(result.spell.source.siteName, result.spell.dice)
                        deps.analytics.track(AnalyticsEvent.SpellImported(result.spell.source.analyticsKey))
                    }
                    is SpellImportResult.Failed -> {
                        ui.importSheet = ui.importSheet?.copy(phase = ImportPhase.Failed(result.failure))
                        deps.analytics.track(AnalyticsEvent.SpellImportFailed(result.failure.name.lowercase()))
                    }
                }
            }
    }

    /** the sheet is closed or swiped away, a load that is still running is cancelled */
    fun close() {
        job?.cancel()
        ui.importSheet = null
    }

    /** the hint says what to check on a form the player has not touched yet, any edit or a close ends it */
    fun endHint() {
        ui.importHint = null
    }
}
