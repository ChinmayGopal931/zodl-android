package co.electriccoin.zcash.ui.screen.chat.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatModelsTest {
    @Test
    fun `capDisplayName truncates strings longer than the limit`() {
        val long = "a".repeat(MAX_DISPLAY_NAME_LENGTH + 50)

        val capped = long.capDisplayName()

        assertEquals(MAX_DISPLAY_NAME_LENGTH, capped.length)
    }

    @Test
    fun `capDisplayName preserves strings at exactly the limit`() {
        val boundary = "a".repeat(MAX_DISPLAY_NAME_LENGTH)

        val capped = boundary.capDisplayName()

        assertEquals(boundary, capped)
    }

    @Test
    fun `capDisplayName preserves short strings unchanged`() {
        val short = "alice"

        val capped = short.capDisplayName()

        assertEquals("alice", capped)
    }

    @Test
    fun `capDisplayName preserves empty string`() {
        val capped = "".capDisplayName()

        assertEquals("", capped)
    }

    @Test
    fun `capDisplayName truncates from the start preserving the prefix`() {
        val long = "alice_" + "x".repeat(MAX_DISPLAY_NAME_LENGTH)

        val capped = long.capDisplayName()

        assertEquals("alice_", capped.substring(0, "alice_".length))
        assertEquals(MAX_DISPLAY_NAME_LENGTH, capped.length)
    }
}
