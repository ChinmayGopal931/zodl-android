package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewConversationView(state: NewConversationState, modifier: Modifier = Modifier) {
    val c = ZappTheme.colors

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
    ) {
        ZappScreenHeader(title = state.title.getValue())

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            if (state.showEmptyState) {
                EmptyState(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp),
                )
            } else {
                ConversationBody(state = state)
            }
        }

        SearchField(
            value = state.searchInput,
            onChange = state.onSearchInputChange,
            onClear = state.onClearSearch,
        )

        BottomDock(state = state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConversationBody(state: NewConversationState) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
    ) {
        if (state.selectedParticipants.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.selectedParticipants.forEach { chip -> ParticipantChip(chip = chip) }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.isPublicKeyDetected) {
            Spacer(Modifier.height(12.dp))
            PublicKeyDetectedBanner(
                detectedKey = state.detectedPublicKey,
                onAdd = state.onAddDetectedKey,
            )
            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = state.contacts, key = { it.contact.publicKey }) { item ->
                ContactSelectRow(item = item)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp)
                            .height(1.dp)
                            .background(c.border, RectangleShape),
                )
            }
        }
    }
}
