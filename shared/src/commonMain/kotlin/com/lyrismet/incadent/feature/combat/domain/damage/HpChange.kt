package com.lyrismet.incadent.feature.combat.domain.damage

/** the hit points and the temporary ones after a change, [effective] is how much it really did */
data class HpChange(
    val hp: Int,
    val temp: Int,
    val effective: Int,
)

/** damage takes the temporary hit points first and the rest from the hit points, which stop at zero */
fun takeDamage(
    hp: Int,
    temp: Int,
    amount: Int,
): HpChange {
    val absorbed = minOf(temp, amount)
    val left = maxOf(0, hp - (amount - absorbed))
    return HpChange(hp = left, temp = temp - absorbed, effective = absorbed + (hp - left))
}

/** healing stops at the maximum */
fun takeHeal(
    hp: Int,
    max: Int,
    temp: Int,
    amount: Int,
): HpChange {
    val healed = minOf(max, hp + amount)
    return HpChange(hp = healed, temp = temp, effective = healed - hp)
}

/** temporary hit points never add up, the larger number stays */
fun takeTemp(
    hp: Int,
    temp: Int,
    amount: Int,
): HpChange {
    val kept = maxOf(temp, amount)
    return HpChange(hp = hp, temp = kept, effective = kept - temp)
}
