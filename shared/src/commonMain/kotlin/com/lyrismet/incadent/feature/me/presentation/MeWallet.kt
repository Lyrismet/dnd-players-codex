package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.NumberPadSheet
import com.lyrismet.incadent.core.designsystem.component.PillFill
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.sheet.SheetIcons
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.core.format.withThousands
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import com.lyrismet.incadent.feature.codex.presentation.item.coinAmountText
import com.lyrismet.incadent.feature.codex.presentation.item.coinLabel
import com.lyrismet.incadent.feature.me.domain.MORE_COINS
import com.lyrismet.incadent.feature.me.domain.QUICK_COINS
import com.lyrismet.incadent.feature.me.domain.WalletMode
import com.lyrismet.incadent.feature.me.domain.showsTotal
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_help_wallet_body
import incadent.shared.generated.resources.me_help_wallet_title
import incadent.shared.generated.resources.me_wallet_apply
import incadent.shared.generated.resources.me_wallet_change
import incadent.shared.generated.resources.me_wallet_coin_change_format
import incadent.shared.generated.resources.me_wallet_coin_separator
import incadent.shared.generated.resources.me_wallet_gain
import incadent.shared.generated.resources.me_wallet_gold_word
import incadent.shared.generated.resources.me_wallet_mode_minus
import incadent.shared.generated.resources.me_wallet_mode_plus
import incadent.shared.generated.resources.me_wallet_more
import incadent.shared.generated.resources.me_wallet_overline_format
import incadent.shared.generated.resources.me_wallet_rate_copper
import incadent.shared.generated.resources.me_wallet_rate_electrum
import incadent.shared.generated.resources.me_wallet_rate_gold
import incadent.shared.generated.resources.me_wallet_rate_platinum
import incadent.shared.generated.resources.me_wallet_rate_silver
import incadent.shared.generated.resources.me_wallet_spend
import incadent.shared.generated.resources.me_wallet_title
import incadent.shared.generated.resources.me_wallet_total_change_format
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val SMALL_COINS = listOf(Coin.PLATINUM, Coin.ELECTRUM, Coin.SILVER, Coin.COPPER)

/** the purse: gold big, every other coin that is not zero in words under it */
@Composable
internal fun MeWalletCard(
    purse: CoinPurse,
    readOnly: Boolean,
    onChange: () -> Unit,
) {
    val rest = SMALL_COINS.filter { purse[it] > 0 }.map { coinAmountText(purse[it], it) }
    Row(
        Modifier.fillMaxWidth().appCard().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(size = 40.dp, shape = AppShapes.Field, border = AppPalette.BorderPopover) {
            Icon(
                SheetIcons.Coin,
                contentDescription = null,
                tint = AppPalette.GoldBright,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    purse.gold.withThousands(),
                    style = AppText.numeral(28.sp),
                    color = AppPalette.TextHeading,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    pluralStringResource(Res.plurals.me_wallet_gold_word, purse.gold),
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.TextPrimary,
                    maxLines = 1,
                )
            }
            if (rest.isNotEmpty()) {
                Text(
                    rest.joinToString(stringResource(Res.string.me_wallet_coin_separator)),
                    style = AppText.footnote,
                    color = AppPalette.TextSecondary,
                )
            }
        }
        if (!readOnly) {
            AppButton(
                text = stringResource(Res.string.me_wallet_change),
                onClick = onChange,
                tone = ButtonTone.Neutral,
                variant = ButtonVariant.Solid,
                height = 40.dp,
            )
        }
    }
}

@Composable
internal fun MeWalletTitle() {
    MeSectionTitle(
        stringResource(Res.string.me_wallet_title),
        helpTitle = stringResource(Res.string.me_help_wallet_title),
        helpBody = stringResource(Res.string.me_help_wallet_body),
    )
}

/** the keypad of the wallet: spend or gain, the coin, and the purse before and after */
@Composable
internal fun MeWalletSheet(
    name: String,
    wallet: MeOverlay.Wallet,
    onEvent: (WalletEvent) -> Unit,
) {
    val pad = wallet.pad
    val change = wallet.change
    val before = change.before[pad.coin].withThousands()
    val after = change.after[pad.coin].withThousands()
    val total =
        if (change.showsTotal()) {
            stringResource(
                Res.string.me_wallet_total_change_format,
                change.totalBefore.toInt().withThousands(),
                change.totalAfter.toInt().withThousands(),
            )
        } else {
            ""
        }
    val titleRes =
        when (pad.mode) {
            WalletMode.SPEND -> Res.string.me_wallet_spend
            WalletMode.GAIN -> Res.string.me_wallet_gain
        }
    AppBottomSheet(onDismissRequest = { onEvent(WalletEvent.Closed) }) {
        NumberPadSheet(
            overline = stringResource(Res.string.me_wallet_overline_format, name).uppercase(),
            title = stringResource(titleRes),
            expr = "",
            result = pad.amount.withThousands(),
            rangeHint =
                stringResource(
                    Res.string.me_wallet_coin_change_format,
                    coinLabel(pad.coin).capitalizeFirst(),
                    before,
                    after,
                ),
            applyLabel = stringResource(Res.string.me_wallet_apply),
            applyEnabled = pad.canApply,
            onDigit = { onEvent(WalletEvent.Digit(it)) },
            onBackspace = { onEvent(WalletEvent.Backspace) },
            onClear = { onEvent(WalletEvent.Cleared) },
            onApply = { onEvent(WalletEvent.Applied) },
            onClose = { onEvent(WalletEvent.Closed) },
            modes = { WalletModes(wallet, total, onEvent) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletModes(
    wallet: MeOverlay.Wallet,
    total: String,
    onEvent: (WalletEvent) -> Unit,
) {
    val pad = wallet.pad
    val moreShown = pad.moreOpen || pad.coin in MORE_COINS
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppSegmentedTabs(
            tabs =
                listOf(
                    SegmentedTab(WalletMode.GAIN, stringResource(Res.string.me_wallet_mode_plus)),
                    SegmentedTab(WalletMode.SPEND, stringResource(Res.string.me_wallet_mode_minus)),
                ),
            selected = pad.mode,
            onSelected = { onEvent(WalletEvent.ModeChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (QUICK_COINS + if (moreShown) MORE_COINS else emptyList()).forEach { coin ->
                SelectablePill(
                    text = coinLabel(coin).capitalizeFirst(),
                    selectedFill = PillFill.GoldTint,
                    selected = pad.coin == coin,
                    onClick = { onEvent(WalletEvent.CoinChosen(coin)) },
                    size = PillSize.Regular,
                )
            }
            if (!moreShown) {
                SelectablePill(
                    text = stringResource(Res.string.me_wallet_more),
                    selected = false,
                    onClick = { onEvent(WalletEvent.MoreToggled) },
                    size = PillSize.Regular,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(rateText(pad.coin), Modifier.weight(1f), style = AppText.footnote, color = AppPalette.TextSecondary)
            Text(total, style = AppText.footnote, color = AppPalette.TextSecondary)
        }
    }
}

@Composable
private fun rateText(coin: Coin): String =
    stringResource(
        when (coin) {
            Coin.PLATINUM -> Res.string.me_wallet_rate_platinum
            Coin.GOLD -> Res.string.me_wallet_rate_gold
            Coin.ELECTRUM -> Res.string.me_wallet_rate_electrum
            Coin.SILVER -> Res.string.me_wallet_rate_silver
            Coin.COPPER -> Res.string.me_wallet_rate_copper
        },
    )
