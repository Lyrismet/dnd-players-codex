package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.domain.model.Photo

// no full jpeg means "unchanged" from a list-level model, and a picture equal to the stored one needs no rewrite
internal fun Photo.needsWrite(stored: Photo?): Boolean = full != null && !full.contentEquals(stored?.full)
