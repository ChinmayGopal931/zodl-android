package co.electriccoin.zcash.ui.screen.onboarding.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

/** Six square dots that fill as the user enters each digit. */
@Composable
internal fun PinDotRow(filledCount: Int, hasError: Boolean) {
    val c = ZappTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) { i ->
            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .background(
                            when {
                                hasError -> c.danger
                                i < filledCount -> c.text
                                else -> c.border
                            },
                            RectangleShape,
                        ),
            )
        }
    }
}

/** Standard phone-layout numeric keypad (1-9, blank, 0, ⌫). */
@Composable
internal fun PinKeypad(modifier: Modifier = Modifier, onKey: (String) -> Unit) {
    val c = ZappTheme.colors
    val rows =
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(null, "0", "⌫"),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    if (key == null) {
                        Box(modifier = Modifier.weight(1f).height(60.dp))
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .border(1.dp, c.border, RectangleShape)
                                    .clickable(onClick = { onKey(key) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = key,
                                style =
                                    ZappTheme.typography.button.copy(
                                        color = c.text,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
