package com.lyrismet.incadent.core.hp

import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.calc_heal_to_max_toast_many
import incadent.shared.generated.resources.calc_heal_to_max_toast_one
import incadent.shared.generated.resources.calc_heal_to_max_toast_sub
import incadent.shared.generated.resources.calc_heal_to_max_toast_sub_temp
import org.jetbrains.compose.resources.getString

/** the hit points a participant had before a heal to the maximum, shown on the undo toast */
class HealedToMax(
    val name: String,
    val hp: Int,
    val max: Int,
    val temp: Int,
)

/** the title and the subtitle of the undo toast: one name with its numbers or the count and the names */
suspend fun healToMaxToast(healed: List<HealedToMax>): Pair<String, String> {
    val first = healed.first()
    if (healed.size > 1) {
        return getString(Res.string.calc_heal_to_max_toast_many, healed.size) to healed.joinToString(", ") { it.name }
    }
    val subtitle =
        if (first.temp > 0) {
            getString(Res.string.calc_heal_to_max_toast_sub_temp, first.hp, first.max, first.temp)
        } else {
            getString(Res.string.calc_heal_to_max_toast_sub, first.hp, first.max)
        }
    return getString(Res.string.calc_heal_to_max_toast_one, first.name) to subtitle
}
