package co.electriccoin.zcash.ui.screen.tor.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappBottomActionBar
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.screen.exchangerate.settings.Option

@Composable
internal fun TorSettingsView(state: TorSettingsState) {
    var isOptInSelected by remember(state.isOptedIn) { mutableStateOf(state.isOptedIn) }

    val isSaveEnabled by remember {
        derivedStateOf { isOptInSelected != state.isOptedIn }
    }

    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
    ) {
        ZappScreenHeader(title = stringResource(R.string.tor_settings_title))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_tor_settings),
                contentDescription = null,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            BasicText(
                text = stringResource(R.string.tor_settings_subtitle),
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )
            Spacer(Modifier.height(24.dp))
            Option(
                modifier = Modifier.fillMaxWidth(),
                image = R.drawable.ic_opt_in,
                isChecked = isOptInSelected,
                title = stringResource(R.string.exchange_rate_opt_in_option_title),
                subtitle = stringResource(R.string.tor_settings_item_subtitle_1),
                onClick = { isOptInSelected = true }
            )
            Spacer(Modifier.height(12.dp))
            Option(
                modifier = Modifier.fillMaxWidth(),
                image = R.drawable.ic_opt_out,
                isChecked = !isOptInSelected,
                title = stringResource(R.string.exchange_rate_opt_out_option_title),
                subtitle = stringResource(R.string.tor_settings_item_subtitle_2),
                onClick = { isOptInSelected = false }
            )
        }

        ZappBottomActionBar(
            onBack = state.onDismiss,
            primaryAction = {
                ZappButton(
                    text = stringResource(R.string.exchange_rate_opt_in_save),
                    onClick = { state.onSaveClick(isOptInSelected) },
                    enabled = isSaveEnabled,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                )
            },
        )
    }
}

@PreviewScreens
@Composable
private fun TorSettingsPreview() =
    ZcashTheme {
        TorSettingsView(
            state =
                TorSettingsState(
                    isOptedIn = true,
                    onSaveClick = {},
                    onDismiss = {}
                )
        )
    }
