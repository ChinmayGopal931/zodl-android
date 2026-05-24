package co.electriccoin.zcash.ui.screen.offramp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.Spacer
import co.electriccoin.zcash.ui.design.component.ZashiButton
import co.electriccoin.zcash.ui.design.component.ZashiButtonDefaults
import co.electriccoin.zcash.ui.design.component.ZashiScreenModalBottomSheet
import co.electriccoin.zcash.ui.design.component.ZashiTextField
import co.electriccoin.zcash.ui.design.component.rememberScreenModalBottomSheetState
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ProvideZappTheme
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfframpView(
    state: OfframpState?,
    sheetState: SheetState = rememberScreenModalBottomSheetState(),
) {
    ZashiScreenModalBottomSheet(
        state = state,
        sheetState = sheetState,
        content = { state, contentPadding ->
            Content(
                modifier = Modifier.weight(1f, false),
                state = state,
                contentPadding = contentPadding
            )
        },
    )
}

@Composable
private fun Content(
    state: OfframpState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = contentPadding.calculateBottomPadding()
                )
    ) {
        Text(
            text = stringResource(R.string.offramp_title),
            color = ZappTheme.colors.text,
            style = ZappTheme.typography.sectionTitle,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(16.dp)
        Text(
            text = stringResource(R.string.offramp_amount_label),
            color = ZappTheme.colors.text,
            style = ZappTheme.typography.caption,
            fontWeight = FontWeight.Medium
        )
        Spacer(4.dp)
        ZashiTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.amount,
            onValueChange = state.onAmountChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.offramp_amount_hint),
                    color = ZappTheme.colors.textSubtle,
                    style = ZappTheme.typography.body
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        state.fiatAmount?.let { fiat ->
            Spacer(4.dp)
            Text(
                text = fiat.getValue(),
                color = ZappTheme.colors.textSubtle,
                style = ZappTheme.typography.caption
            )
        }
        Spacer(16.dp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZappTheme.colors.surfaceAlt),
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.offramp_info_body),
                color = ZappTheme.colors.textSubtle,
                style = ZappTheme.typography.caption
            )
        }
        Spacer(24.dp)
        ZashiButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = state.onContinue,
            text = stringResource(R.string.offramp_continue_button),
            enabled = state.isContinueEnabled,
        )
        Spacer(8.dp)
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            text = state.attribution.getValue(),
            color = ZappTheme.colors.textSubtle,
            style = ZappTheme.typography.caption,
            textAlign = TextAlign.Center
        )
        Spacer(8.dp)
        ZashiButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = state.onBack,
            text = stringResource(R.string.offramp_cancel),
            colors = ZashiButtonDefaults.secondaryColors(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreens
@Composable
private fun Preview() =
    ZcashTheme {
        ProvideZappTheme {
            OfframpView(
                state =
                    OfframpState(
                        onBack = {},
                        amount = "0.08",
                        fiatAmount = null,
                        onAmountChange = {},
                        onContinue = {},
                        isContinueEnabled = true,
                        attribution =
                            co.electriccoin.zcash.ui.design.util
                                .stringRes("Powered by peer.xyz"),
                    )
            )
        }
    }
