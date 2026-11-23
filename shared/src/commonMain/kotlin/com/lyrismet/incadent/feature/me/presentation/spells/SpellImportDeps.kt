package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.core.analytics.AnalyticsTracker
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImporter
import dev.zacsweers.metro.Inject

/** what the import of a spell needs, one object so a presenter that embeds the block takes one parameter */
@Inject
class SpellImportDeps(
    val importer: SpellImporter,
    val analytics: AnalyticsTracker,
)
