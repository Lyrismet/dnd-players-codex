package com.lyrismet.incadent.feature.campaign.domain.exchange

import com.lyrismet.incadent.core.format.foldForSearch

/** how two records are told to be the same one: edges, case and ё/е ignored - the preview and the import must agree */
fun String.nameKey(): String = trim().foldForSearch()
