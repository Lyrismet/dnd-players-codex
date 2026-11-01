package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.domain.parseMentions

// a note with mention tokens is split into chips, a plain one stays a single text
internal fun noteSegments(
    text: String,
    lookup: EntityLookup,
): List<MentionSegment> = if ('@' in text) parseMentions(text, lookup.mentionIndex) else emptyList()
