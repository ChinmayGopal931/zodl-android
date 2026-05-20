package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.component.zapp.ZappRowDivider
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.transactionhistory.ActivityState
import co.electriccoin.zcash.ui.screen.transactionhistory.widget.ActivityWidgetState

internal fun LazyListScope.activitySection(state: ActivityWidgetState) {
    when (state) {
        is ActivityWidgetState.Data -> {
            val lastKey = state.transactions.lastOrNull()?.key
            items(
                items = state.transactions,
                key = { it.key },
            ) { activity ->
                ActivityRow(activity)
                if (activity.key != lastKey) {
                    ZappRowDivider(inset = true)
                }
            }
        }

        is ActivityWidgetState.Empty ->
            item { ActivityEmpty() }

        ActivityWidgetState.Loading ->
            item { ActivityLoading() }
    }
}

@Composable
private fun ActivityRow(state: ActivityState) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(c.surfaceAlt, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(state.bigIcon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = state.title.getValue(),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state.subtitle?.let { subtitle ->
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = subtitle.getValue(),
                    style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        state.value?.let { styled ->
            val value: AnnotatedString = styled.getValue()
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = value,
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ActivityEmpty() {
    val c = ZappTheme.colors
    // Swiss-style: left-aligned, no centered illustration, sharp top rule that
    // matches the divider rhythm an actual transaction list would have.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.dp, c.border), RectangleShape)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(c.accent, RectangleShape),
        )
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "No transactions yet.",
            style = ZappTheme.typography.rowTitle.copy(
                color = c.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = "Your sends and receives will appear here.",
            style = ZappTheme.typography.rowSubtitle.copy(
                color = c.textMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        )
    }
}

@Composable
private fun ActivityLoading() {
    val c = ZappTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = c.accent)
    }
}
