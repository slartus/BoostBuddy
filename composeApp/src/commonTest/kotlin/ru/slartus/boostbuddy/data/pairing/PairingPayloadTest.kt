package ru.slartus.boostbuddy.data.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairingPayloadTest {

    @Test
    fun normalizeEpochSeconds_keepsSeconds() {
        assertEquals(1_800_000_000L, normalizeEpochSeconds(1_800_000_000L))
        assertEquals(100_000_000_000L, normalizeEpochSeconds(100_000_000_000L))
    }

    @Test
    fun normalizeEpochSeconds_convertsMilliseconds() {
        assertEquals(1_800_000_000L, normalizeEpochSeconds(1_800_000_000_123L))
        assertEquals(100_000_000L, normalizeEpochSeconds(100_000_000_001L))
    }

    @Test
    fun normalizeEpochSeconds_rejectsMissingOrNonPositive() {
        assertNull(normalizeEpochSeconds(null))
        assertNull(normalizeEpochSeconds(0L))
        assertNull(normalizeEpochSeconds(-1L))
    }

    @Test
    fun toString_doesNotLeakTokens() {
        val payload = PairingPayload("access-secret", "refresh-secret", null, "device", PairingPayload.SOURCE_APP, 1L)

        assertEquals(false, payload.toString().contains("secret"))
    }
}
