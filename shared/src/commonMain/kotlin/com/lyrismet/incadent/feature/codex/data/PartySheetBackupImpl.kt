package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterFeatRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSpellRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartySheetBackup
import com.lyrismet.incadent.feature.codex.domain.repository.PartySheetSnapshot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PartySheetBackupImpl(
    private val sheets: CharacterSheetRepository,
    private val resources: CharacterResourceRepository,
    private val spells: CharacterSpellRepository,
    private val feats: CharacterFeatRepository,
) : PartySheetBackup {
    override suspend fun capture(memberId: Long) =
        PartySheetSnapshot(
            sheet = sheets.getSheet(memberId),
            vitals = sheets.observeVitals(memberId).first(),
            resources = resources.observe(memberId).first(),
            spells = spells.observe(memberId).first(),
            feats = feats.observe(memberId).first(),
        )

    override suspend fun restore(
        memberId: Long,
        snapshot: PartySheetSnapshot,
    ) {
        if (snapshot.sheet != CharacterSheet()) sheets.saveSheet(memberId, snapshot.sheet)
        snapshot.vitals?.let { sheets.saveVitals(memberId, it) }
        snapshot.resources.forEach { resources.upsert(memberId, it) }
        snapshot.spells.forEach { spells.upsert(memberId, it) }
        snapshot.feats.forEach { feats.upsert(memberId, it) }
    }
}
