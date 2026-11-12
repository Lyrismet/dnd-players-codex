package com.lyrismet.incadent.feature.codex.presentation.item

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.core.format.withThousands
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.ItemOwnerView
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.items_coin_copper
import incadent.shared.generated.resources.items_coin_copper_short
import incadent.shared.generated.resources.items_coin_electrum
import incadent.shared.generated.resources.items_coin_electrum_short
import incadent.shared.generated.resources.items_coin_gold
import incadent.shared.generated.resources.items_coin_gold_short
import incadent.shared.generated.resources.items_coin_platinum
import incadent.shared.generated.resources.items_coin_platinum_short
import incadent.shared.generated.resources.items_coin_silver
import incadent.shared.generated.resources.items_coin_silver_short
import incadent.shared.generated.resources.items_owner_me
import incadent.shared.generated.resources.items_owner_place_format
import incadent.shared.generated.resources.items_owner_shared
import incadent.shared.generated.resources.items_price_each_format
import incadent.shared.generated.resources.items_quantity_format
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** "50 золотых", the coin worded for the amount */
@Composable
fun coinAmountText(
    price: Int,
    coin: Coin,
): String = pluralStringResource(coin.plural(), price, price.withThousands())

/** "50 золотых за штуку" */
@Composable
fun itemPriceEachText(
    price: Int,
    coin: Coin,
): String = stringResource(Res.string.items_price_each_format, coinAmountText(price, coin))

/** "×3" */
@Composable
fun itemQuantityText(quantity: Int): String = stringResource(Res.string.items_quantity_format, quantity)

/** "золотые", the coin as a label of a choice */
@Composable
fun coinLabel(coin: Coin): String = stringResource(coin.short())

/** who carries the item in words - "я", a name, "место «Мирдейл»" or "Общее" */
@Composable
fun ItemOwnerView.text(): String =
    when {
        kind == null -> stringResource(Res.string.items_owner_shared)
        isMe -> stringResource(Res.string.items_owner_me)
        kind == EntityKind.LOCATION -> stringResource(Res.string.items_owner_place_format, name)
        else -> name
    }

/** the quantity and the owner under a name, "×3 · Лира" */
@Composable
fun itemMetaText(
    quantity: Int,
    owner: ItemOwnerView,
): String = joinWithDot(itemQuantityText(quantity), owner.text())

private fun Coin.plural(): PluralStringResource =
    when (this) {
        Coin.PLATINUM -> Res.plurals.items_coin_platinum
        Coin.GOLD -> Res.plurals.items_coin_gold
        Coin.ELECTRUM -> Res.plurals.items_coin_electrum
        Coin.SILVER -> Res.plurals.items_coin_silver
        Coin.COPPER -> Res.plurals.items_coin_copper
    }

private fun Coin.short(): StringResource =
    when (this) {
        Coin.PLATINUM -> Res.string.items_coin_platinum_short
        Coin.GOLD -> Res.string.items_coin_gold_short
        Coin.ELECTRUM -> Res.string.items_coin_electrum_short
        Coin.SILVER -> Res.string.items_coin_silver_short
        Coin.COPPER -> Res.string.items_coin_copper_short
    }
